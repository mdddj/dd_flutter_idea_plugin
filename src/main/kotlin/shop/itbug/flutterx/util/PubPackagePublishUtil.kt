package shop.itbug.flutterx.util

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.yaml.psi.YAMLFile
import shop.itbug.flutterx.common.yaml.PubspecYamlFileTools
import shop.itbug.flutterx.i18n.PluginBundle
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.ArrayDeque

data class ChildPubPackage(
    val name: String,
    val version: String,
    val directory: VirtualFile,
    val workDirectory: File,
    val publishTo: String?,
    val localDependencies: Set<String>
) {
    val publishable: Boolean
        get() = !publishTo.equals("none", ignoreCase = true)
}

object PubPackagePublishUtil {

    fun hasMultipleChildPackages(rootDirectory: VirtualFile): Boolean {
        return rootDirectory.isDirectory && rootDirectory.children.count {
            it.isDirectory && it.findChild("pubspec.yaml") != null
        } > 1
    }

    fun collectChildPackages(project: Project, rootDirectory: VirtualFile): List<ChildPubPackage> {
        return runBlocking(Dispatchers.IO) {
            buildList {
                rootDirectory.children
                    .filter { it.isDirectory }
                    .forEach { childDir ->
                        val pubspec = childDir.findChild("pubspec.yaml") ?: return@forEach
                        val yamlFile = readAction {
                            PsiManager.getInstance(project).findFile(pubspec) as? YAMLFile
                        } ?: return@forEach
                        val tools = PubspecYamlFileTools.create(yamlFile)
                        val rootValues = tools.getRootKeyValueList().orEmpty()
                        val dependencies = (
                            tools.getDependencies() +
                                tools.getDevDependencies() +
                                tools.getDependencyOverrides()
                            )
                            .map { it.keyText.trim() }
                            .filter { it.isNotBlank() }
                            .toSet()

                        add(
                            ChildPubPackage(
                                name = rootValues.findValue("name") ?: childDir.name,
                                version = rootValues.findValue("version") ?: "",
                                directory = childDir,
                                workDirectory = childDir.toNioPath().toFile(),
                                publishTo = rootValues.findValue("publish_to"),
                                localDependencies = dependencies
                            )
                        )
                    }
                }
        }
    }

    fun sortForPublish(packages: List<ChildPubPackage>): List<ChildPubPackage> {
        if (packages.size < 2) return packages

        val indexMap = packages.mapIndexed { index, item -> item.name to index }.toMap()
        val packagesByName = packages.associateBy { it.name }
        val inDegree = packages.associate { it.name to 0 }.toMutableMap()
        val adjacency = mutableMapOf<String, MutableSet<String>>()

        packages.forEach { item ->
            item.localDependencies
                .filter { packagesByName.containsKey(it) }
                .forEach { dependencyName ->
                    adjacency.getOrPut(dependencyName) { linkedSetOf() }.add(item.name)
                    inDegree[item.name] = inDegree.getValue(item.name) + 1
                }
        }

        val queue = ArrayDeque(
            packages
                .filter { inDegree[it.name] == 0 }
                .sortedBy { indexMap[it.name] ?: Int.MAX_VALUE }
        )
        val result = mutableListOf<ChildPubPackage>()

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            result.add(current)
            adjacency[current.name]
                .orEmpty()
                .sortedBy { indexMap[it] ?: Int.MAX_VALUE }
                .forEach { dependentName ->
                    val nextDegree = inDegree.getValue(dependentName) - 1
                    inDegree[dependentName] = nextDegree
                    if (nextDegree == 0) {
                        packagesByName[dependentName]?.let(queue::addLast)
                    }
                }
        }

        return if (result.size == packages.size) result else packages
    }

    fun updatePubspecVersion(workDirectory: File, packageName: String, version: String) {
        val pubspecFile = File(workDirectory, "pubspec.yaml")
        if (!pubspecFile.exists()) {
            throw IllegalStateException(PluginBundle.get("batch_publish_child_packages_pubspec_not_found", packageName))
        }

        val originalContent = pubspecFile.readText(Charsets.UTF_8)
        val lineSeparator = detectLineSeparator(originalContent)
        val versionRegex = Regex("(?m)^version\\s*:\\s*.*$")
        if (!versionRegex.containsMatchIn(originalContent)) {
            throw IllegalStateException(
                PluginBundle.get("batch_publish_child_packages_pubspec_version_missing", packageName)
            )
        }

        val updatedContent = versionRegex.replaceFirst(originalContent, "version: $version")
        pubspecFile.writeText(ensureTrailingLineBreak(updatedContent, lineSeparator), Charsets.UTF_8)
    }

    fun updateChangelogForPublish(
        workDirectory: File,
        version: String,
        releaseNotes: String,
        includePublishDate: Boolean
    ) {
        val changelogFile = File(workDirectory, "CHANGELOG.md")
        if (!changelogFile.exists()) {
            throw IllegalStateException(PluginBundle.get("pubspec_notification_publish_changelog_not_found"))
        }

        val originalContent = changelogFile.readText(Charsets.UTF_8)
        val lineSeparator = detectLineSeparator(originalContent)
        val normalizedNotes = normalizeReleaseNotes(releaseNotes, lineSeparator)
        val updatedContent = insertOrUpdateChangelogSection(
            originalContent,
            version,
            normalizedNotes,
            lineSeparator,
            includePublishDate
        )

        changelogFile.writeText(updatedContent, Charsets.UTF_8)
    }

    private fun insertOrUpdateChangelogSection(
        content: String,
        version: String,
        releaseNotes: String,
        lineSeparator: String,
        includePublishDate: Boolean
    ): String {
        val heading = buildChangelogHeading(content, version, includePublishDate)
        val versionHeaderRegex = Regex(
            "(?m)^## (\\[${Regex.escape(version)}\\]|${Regex.escape(version)})(?:\\s+-.*)?$"
        )
        val existingVersionMatch = versionHeaderRegex.find(content)
        if (existingVersionMatch != null) {
            val nextHeaderRegex = Regex("(?m)^## ")
            val nextHeaderMatch = nextHeaderRegex.find(content, existingVersionMatch.range.last + 1)
            val sectionEnd = nextHeaderMatch?.range?.first ?: content.length
            val existingSection = content.substring(existingVersionMatch.range.first, sectionEnd)
            val existingBody = existingSection.substringAfter(existingVersionMatch.value).trim()
            val mergedSection = buildString {
                append(heading)
                append(lineSeparator)
                append(lineSeparator)
                append(releaseNotes)
                if (existingBody.isNotBlank()) {
                    append(lineSeparator)
                    append(lineSeparator)
                    append(existingBody)
                }
                if (nextHeaderMatch != null) {
                    append(lineSeparator)
                    append(lineSeparator)
                }
            }
            return ensureTrailingLineBreak(
                content.replaceRange(existingVersionMatch.range.first, sectionEnd, mergedSection),
                lineSeparator
            )
        }

        val unreleasedRegex = Regex("(?m)^## Unreleased(?:\\r?\\n)*")
        val unreleasedMatch = unreleasedRegex.find(content)
        if (unreleasedMatch == null) {
            val prependedContent = buildString {
                append(heading)
                append(lineSeparator)
                append(lineSeparator)
                append(releaseNotes)
                val remainingContent = content.trimStart('\r', '\n')
                if (remainingContent.isNotBlank()) {
                    append(lineSeparator)
                    append(lineSeparator)
                    append(remainingContent)
                }
            }
            return ensureTrailingLineBreak(prependedContent, lineSeparator)
        }

        val insertedContent = buildString {
            append("## Unreleased")
            append(lineSeparator)
            append(lineSeparator)
            append(heading)
            append(lineSeparator)
            append(lineSeparator)
            append(releaseNotes)
            append(lineSeparator)
            append(lineSeparator)
        }
        return ensureTrailingLineBreak(
            content.replaceRange(unreleasedMatch.range.first, unreleasedMatch.range.last + 1, insertedContent),
            lineSeparator
        )
    }

    private fun buildChangelogHeading(content: String, version: String, includePublishDate: Boolean): String {
        val usesBracketVersion = Regex("(?m)^## \\[").containsMatchIn(content)
        val publishDateSuffix = if (includePublishDate) {
            " - ${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}"
        } else {
            ""
        }
        return if (usesBracketVersion) {
            "## [$version]$publishDateSuffix"
        } else {
            "## $version$publishDateSuffix"
        }
    }

    private fun normalizeReleaseNotes(releaseNotes: String, lineSeparator: String): String {
        return releaseNotes
            .trim()
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')
            .joinToString(lineSeparator)
    }

    private fun detectLineSeparator(text: String): String = if (text.contains("\r\n")) "\r\n" else "\n"

    private fun ensureTrailingLineBreak(text: String, lineSeparator: String): String {
        return if (text.endsWith(lineSeparator)) text else text + lineSeparator
    }

    private fun List<org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl>.findValue(key: String): String? {
        return firstOrNull { it.keyText.trim() == key }
            ?.valueText
            ?.trim()
            ?.removeSurrounding("\"")
            ?.removeSurrounding("'")
            ?.takeIf { it.isNotBlank() }
    }
}
