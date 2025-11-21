package shop.itbug.fluttercheckversionx.window.migrate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.JBColor
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.*
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import shop.itbug.fluttercheckversionx.i18n.PluginBundle
import shop.itbug.fluttercheckversionx.services.DotMigrateService
import shop.itbug.fluttercheckversionx.services.DotRemoveElement

@Composable
fun EnumMigrateWindow(project: Project) {
    val service = project.getService(DotMigrateService::class.java)
    val elements = service.cachedElements.collectAsState().value
    val isLoading = service.isLoading.collectAsState().value
    val errorMessage = service.errorMessage.collectAsState().value
    val scanProgress = service.scanProgress.collectAsState().value
    val currentAnalysisFile = service.currentAnalysisFile.collectAsState().value

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 工具栏 - 包含标题、统计信息、进度和按钮
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 标题
            Text(PluginBundle.get("enum.migrate.tool.title"))

            // 错误信息或统计信息
            errorMessage?.let { error ->
                Text(error, color = Color.Red)
                IconButton(onClick = { service.clearError() }) {
                    Icon(key = AllIconsKeys.General.Close, null)
                }
            } ?: run {
                // 统计信息和进度
                scanProgress?.let { progress ->
                    Text(
                        "${PluginBundle.get("enum.migrate.total.arguments")}: ${progress.totalArguments}",
                        modifier = Modifier.animateContentSize()
                    )
                    Text(
                        "${PluginBundle.get("enum.migrate.analyzed")}: ${progress.processedCount}",
                        modifier = Modifier.animateContentSize()
                    )
                    Text(
                        "${PluginBundle.get("enum.migrate.found.enums")}: ${progress.foundEnums} ${PluginBundle.get("enum.migrate.found.enums.suffix")}",
                        modifier = Modifier.animateContentSize()
                    )

                    // 进度百分比
                    val percentage = if (progress.totalArguments > 0) {
                        (progress.processedCount * 100.0 / progress.totalArguments).toInt()
                    } else 0
                    Text(
                        "${PluginBundle.get("enum.migrate.progress")}: $percentage%",
                        modifier = Modifier.animateContentSize()
                    )

                    // 进度条
                    AnimatedVisibility(isLoading) {
                        val progressValue = if (progress.totalArguments > 0) {
                            progress.processedCount.toFloat() / progress.totalArguments
                        } else 0f

                        val animatedProgress by animateFloatAsState(progressValue)

                        HorizontalProgressBar(
                            progress = animatedProgress,
                            modifier = Modifier.width(150.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                } ?: run {
                    Text(
                        "${PluginBundle.get("enum.migrate.found.enums")}: ${elements.size} ${PluginBundle.get("enum.migrate.found.enums.suffix")}",
                        modifier = Modifier.animateContentSize()
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // 按钮
            AnimatedVisibility(isLoading) {
                IconActionButton(key= AllIconsKeys.Run.Stop,onClick = {
                    service.stop()
                }, contentDescription = "Stop")
            }
            DefaultButton(
                onClick = {
                    service.launch {
                        service.refreshScan()
                    }
                },
                enabled = !isLoading
            ) {
                Text(if (isLoading) PluginBundle.get("enum.migrate.scanning") else PluginBundle.get("enum.migrate.refresh"), modifier = Modifier.animateContentSize())
            }

            DefaultButton(
                onClick = {
                    service.launch {
                        elements.forEach {
                            performMigration(project,it)
                        }
                        service.cleanElements()
                    }
                },
                enabled = elements.isNotEmpty() && !isLoading
            ) {
                Text(PluginBundle.get("enum.migrate.migrate.all"))
            }
        }

        Divider(orientation = Orientation.Horizontal)

        // 表格内容
        if (elements.isEmpty() && !isLoading) {
            // 只有在不加载且没有数据时才显示空状态
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(PluginBundle.get("enum.migrate.no.enums.found"))
            }
        } else if (elements.isEmpty()) {
            // 加载中且没有数据时显示加载状态
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
               Column(verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                   Text(PluginBundle.get("enum.migrate.scanning.status"))
                   AnimatedVisibility(!currentAnalysisFile.isNullOrBlank()){
                       Text("$currentAnalysisFile",modifier = Modifier.animateContentSize(), color = JewelTheme.globalColors.text.info)
                   }
               }
            }
        } else {
            // 有数据时显示表格（无论是否在加载中）
            EnumMigrateTable(project, elements) { element ->
                service.launch {
                    performMigration(project, element)
                    service.removeElement(element)
                }
            }
        }
    }
}

@Composable
private fun EnumMigrateTable(
    project: Project,
    elements: List<DotRemoveElement>,
    onMigrate: (DotRemoveElement) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 固定表头
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                PluginBundle.get("enum.migrate.table.enum.type"),
                modifier = Modifier.weight(1.5f),
                color = JewelTheme.globalColors.text.info
            )
            Text(
                PluginBundle.get("enum.migrate.table.full.text"),
                modifier = Modifier.weight(3f),
                color = JewelTheme.globalColors.text.info
            )
            Text(
                PluginBundle.get("enum.migrate.table.after.migration"),
                modifier = Modifier.weight(2f),
                color = JewelTheme.globalColors.text.info
            )
            Text(
                PluginBundle.get("enum.migrate.table.actions"),
                modifier = Modifier.width(200.dp),
                color = JewelTheme.globalColors.text.info
            )
        }
        Divider(orientation = Orientation.Horizontal)

        // 可滚动的数据行
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(elements) { element ->
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()

                // 动画效果
                val backgroundColor = if (isHovered) {
                    if (JewelTheme.isDark) Color.Black else Color.White
                } else {
                    Color.Transparent
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(backgroundColor, RoundedCornerShape(4.dp))
                            .hoverable(interactionSource)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable {
                                navigateToElement(project, element)
                            }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 枚举类型
                        Text(element.enumType, modifier = Modifier.weight(1.5f))

                        // 完整文本 - 高亮要删除的部分
                        Box(modifier = Modifier.weight(3f)) {
                            HighlightedText(element)
                        }

                        // 修改后的预览
                        Text(
                            text = getPreviewText(element),
                            modifier = Modifier.weight(2f),
                            color = Color(0xFF4CAF50) // 绿色表示修改后的文本
                        )

                        // 操作按钮
                        Row(
                            modifier = Modifier.width(320.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Link(PluginBundle.get("enum.migrate.view.location"), onClick = {
                                navigateToElement(project, element)
                            })
                            Link(PluginBundle.get("enum.migrate.execute"), onClick = {
                                onMigrate(element)
                            })

                        }
                    }
                    Divider(orientation = Orientation.Horizontal)
                }
            }
        }
    }
}

@Composable
private fun HighlightedText(element: DotRemoveElement) {
    val fullText = element.fullText
    val enumType = element.enumType

    // 查找枚举类型在完整文本中的位置
    val startIndex = fullText.indexOf(enumType)

    if (startIndex >= 0) {
        val endIndex = startIndex + enumType.length

        val annotatedString = buildAnnotatedString {
            // 前面的部分
            if (startIndex > 0) {
                append(fullText.substring(0, startIndex))
            }

            // 要删除的部分 - 红色背景
            withStyle(
                style = SpanStyle(
                    background = Color(0xFFFF5252), // 红色背景
                    color = Color.White // 白色文字
                )
            ) {
                append(enumType)
            }

            // 后面的部分
            if (endIndex < fullText.length) {
                append(fullText.substring(endIndex))
            }
        }

        Text(annotatedString)
    } else {
        Text(fullText)
    }
}

private fun getPreviewText(element: DotRemoveElement): String {
    val fullText = element.fullText
    val enumType = element.enumType
    val startIndex = fullText.indexOf(enumType)

    return if (startIndex >= 0) {
        val endIndex = startIndex + enumType.length
        fullText.substring(0, startIndex) + fullText.substring(endIndex)
    } else {
        fullText
    }
}

private fun navigateToElement(project: Project, element: DotRemoveElement) {
    ApplicationManager.getApplication().invokeLater {
        val psiElement = element.element.element ?: return@invokeLater
        val containingFile = psiElement.containingFile ?: return@invokeLater
        val virtualFile = containingFile.virtualFile ?: return@invokeLater

        // 打开文件并获取编辑器
        val editor = FileEditorManager.getInstance(project).openTextEditor(
            OpenFileDescriptor(project, virtualFile, element.removeOffsetEnd),
            true
        ) ?: return@invokeLater

        // 选中要删除的文本
        val selectionModel = editor.selectionModel
        selectionModel.setSelection(element.removeOffsetStart, element.removeOffsetEnd)

        // 添加红色背景高亮
        val markupModel = editor.markupModel
        val textAttributes = com.intellij.openapi.editor.markup.TextAttributes().apply {
            backgroundColor = JBColor.red
        }

        // 清除之前的高亮
        markupModel.removeAllHighlighters()

        // 添加新的高亮
        markupModel.addRangeHighlighter(
            element.removeOffsetStart,
            element.removeOffsetEnd,
            com.intellij.openapi.editor.markup.HighlighterLayer.SELECTION,
            textAttributes,
            com.intellij.openapi.editor.markup.HighlighterTargetArea.EXACT_RANGE
        )

        // 滚动到选中的位置
        editor.scrollingModel.scrollToCaret(com.intellij.openapi.editor.ScrollType.CENTER)
    }
}

private fun performMigration(project: Project, element: DotRemoveElement) {
    ApplicationManager.getApplication().invokeLater {
        val psiElement = element.element.element ?: return@invokeLater
        val removedElement = element.removedElement.element ?: return@invokeLater
        val containingFile = psiElement.containingFile ?: return@invokeLater
        val document = PsiDocumentManager.getInstance(project).getDocument(containingFile) ?: return@invokeLater

        WriteCommandAction.runWriteCommandAction(project, "Enum Migration", null, {
            removedElement.delete()
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }, containingFile)
    }
}
