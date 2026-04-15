package vm.hive

import com.google.gson.Gson
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.streams.toList

object HiveSchemaLoader {
    private const val PackageConfigRelativePath = ".dart_tool/package_config.json"
    private const val GeneratedSchemaFileName = "hive_adapters.g.yaml"

    private val gson = Gson()

    fun load(projectRoot: String?): HiveSchemaRegistry {
        if (projectRoot.isNullOrBlank()) {
            return HiveSchemaRegistry(types = builtInSchema(), sourceFiles = emptyList())
        }

        val appRoot = Paths.get(projectRoot).normalize()
        val packageConfigPath = appRoot.resolve(PackageConfigRelativePath)

        val dependencySchemas = linkedMapOf<String, HiveSchemaType>()
        val appSchemas = linkedMapOf<String, HiveSchemaType>()
        val schemaFiles = linkedSetOf<Path>()

        val roots = linkedSetOf<Path>()
        roots.add(appRoot)
        roots.addAll(readPackageRoots(packageConfigPath))

        roots.forEach { root ->
            collectSchemaFiles(root).forEach { schemaPath ->
                val parsed = runCatching { parseSchema(schemaPath) }.getOrNull() ?: return@forEach
                schemaFiles.add(schemaPath)
                val target = if (schemaPath.normalize().startsWith(appRoot)) appSchemas else dependencySchemas
                target.putAll(parsed.types)
            }
        }

        val mergedTypes = linkedMapOf<String, HiveSchemaType>()
        mergedTypes.putAll(builtInSchema())
        mergedTypes.putAll(dependencySchemas)
        mergedTypes.putAll(appSchemas)

        return HiveSchemaRegistry(
            types = mergedTypes,
            sourceFiles = schemaFiles.toList(),
        )
    }

    private fun readPackageRoots(packageConfigPath: Path): List<Path> {
        if (!packageConfigPath.exists()) return emptyList()

        return runCatching {
            val raw = Files.readString(packageConfigPath)
            val config = gson.fromJson(raw, PackageConfig::class.java)
            val configDir = packageConfigPath.parent ?: return emptyList()
            config.packages.orEmpty().mapNotNull { pkg ->
                val rootUri = pkg.rootUri ?: return@mapNotNull null
                resolveRootUri(configDir, rootUri)
            }.distinct()
        }.getOrDefault(emptyList())
    }

    private fun resolveRootUri(configDir: Path, rootUri: String): Path? {
        return runCatching {
            val uri = URI(rootUri)
            when {
                uri.scheme == null -> configDir.resolve(rootUri).normalize()
                uri.scheme.equals("file", ignoreCase = true) -> Path.of(uri).normalize()
                else -> null
            }
        }.getOrNull()
    }

    private fun collectSchemaFiles(packageRoot: Path): List<Path> {
        val libDir = packageRoot.resolve("lib")
        if (!libDir.exists() || !libDir.isDirectory()) return emptyList()

        return runCatching {
            Files.walk(libDir).use { stream ->
                stream
                    .filter { Files.isRegularFile(it) && it.fileName.toString() == GeneratedSchemaFileName }
                    .toList()
            }
        }.getOrDefault(emptyList())
    }

    private fun parseSchema(path: Path): HiveSchema {
        val types = linkedMapOf<String, HiveSchemaType>()

        var currentTypeName: String? = null
        var currentTypeId = 0
        var currentNextIndex = 0
        var currentFields = linkedMapOf<String, HiveSchemaField>()
        var currentFieldName: String? = null
        var rootNextTypeId = 0

        fun flushCurrentType() {
            val name = currentTypeName ?: return
            types[name] = HiveSchemaType(
                typeId = currentTypeId,
                nextIndex = currentNextIndex,
                fields = LinkedHashMap(currentFields),
            )
        }

        Files.readAllLines(path).forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("#")) return@forEach

            val indent = line.indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: 0
            when {
                indent == 0 && trimmed.startsWith("nextTypeId:") -> {
                    rootNextTypeId = trimmed.substringAfter(':').trim().toIntOrNull() ?: 0
                }

                indent == 2 && trimmed.endsWith(":") -> {
                    flushCurrentType()
                    currentTypeName = trimmed.removeSuffix(":")
                    currentTypeId = 0
                    currentNextIndex = 0
                    currentFields = linkedMapOf()
                    currentFieldName = null
                }

                currentTypeName != null && indent == 4 && trimmed.startsWith("typeId:") -> {
                    currentTypeId = trimmed.substringAfter(':').trim().toIntOrNull() ?: 0
                }

                currentTypeName != null && indent == 4 && trimmed.startsWith("nextIndex:") -> {
                    currentNextIndex = trimmed.substringAfter(':').trim().toIntOrNull() ?: 0
                }

                currentTypeName != null && indent == 6 && trimmed.endsWith(":") -> {
                    currentFieldName = trimmed.removeSuffix(":")
                }

                currentTypeName != null && currentFieldName != null && indent >= 8 && trimmed.startsWith("index:") -> {
                    val index = trimmed.substringAfter(':').trim().toIntOrNull() ?: return@forEach
                    currentFields[currentFieldName!!] = HiveSchemaField(index = index)
                }
            }
        }

        flushCurrentType()
        return HiveSchema(rootNextTypeId, types)
    }

    private fun builtInSchema(): Map<String, HiveSchemaType> = mapOf(
        "Color" to HiveSchemaType(
            typeId = 200,
            nextIndex = 5,
            fields = mapOf(
                "a" to HiveSchemaField(index = 0),
                "r" to HiveSchemaField(index = 1),
                "g" to HiveSchemaField(index = 2),
                "b" to HiveSchemaField(index = 3),
                "colorSpace" to HiveSchemaField(index = 4),
            ),
        ),
    )
}

private data class PackageConfig(
    val packages: List<PackageConfigPackage>? = null,
)

private data class PackageConfigPackage(
    val name: String? = null,
    val rootUri: String? = null,
)
