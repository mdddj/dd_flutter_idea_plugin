package shop.itbug.fluttercheckversionx.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.theme.simpleListItemStyle


@Composable
fun SearchResultCard(
    text: String,
    searchQuery: String,
    enableFuzzyMatch: Boolean,
    enableAnimation: Boolean,
    modifier: Modifier? = null,
    maxLines: Int = Int.MAX_VALUE
) {
    val animatedElevation by animateDpAsState(
        targetValue = if (searchQuery.isNotEmpty()) 6.dp else 2.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "elevation"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (searchQuery.isEmpty()) 0.8f else 1f,
        animationSpec = tween(300),
        label = "alpha"
    )

    Text(
        text = if (enableFuzzyMatch) {
            animatedFuzzyHighlight(text, searchQuery, enableAnimation)
        } else {
            animatedHighlightSearchText(text, searchQuery, enableAnimation)
        },
        modifier = modifier ?: Modifier.padding(16.dp),
        maxLines = maxLines
    )
}

/**
 * 带动画效果的高亮函数
 */
@Composable
private fun animatedHighlightSearchText(
    text: String,
    searchQuery: String,
    enableAnimation: Boolean
): AnnotatedString {
    val animatedColors by animateColorAsState(
        targetValue = if (enableAnimation) {
            JewelTheme.simpleListItemStyle.colors.backgroundSelectedActive.copy(alpha = 0.7f)
        } else {
            JewelTheme.simpleListItemStyle.colors.backgroundSelectedActive
        },
        animationSpec = tween(500),
        label = "highlight_color"
    )

    if (searchQuery.isEmpty()) {
        return AnnotatedString(text)
    }

    val annotatedStringBuilder = AnnotatedString.Builder()
    var currentIndex = 0

    val lowerText = text.lowercase()
    val lowerQuery = searchQuery.lowercase()

    while (currentIndex < text.length) {
        val foundIndex = lowerText.indexOf(lowerQuery, currentIndex)

        if (foundIndex == -1) {
            annotatedStringBuilder.append(text.substring(currentIndex))
            break
        }

        if (foundIndex > currentIndex) {
            annotatedStringBuilder.append(text.substring(currentIndex, foundIndex))
        }

        annotatedStringBuilder.withStyle(
            style = SpanStyle(
                background = animatedColors,
                color = JewelTheme.globalColors.text.selected,
                fontWeight = FontWeight.Bold,
                shadow = if (enableAnimation) {
                    Shadow(
                        color = Color.Gray.copy(alpha = 0.3f),
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                } else null
            )
        ) {
            append(text.substring(foundIndex, foundIndex + searchQuery.length))
        }

        currentIndex = foundIndex + searchQuery.length
    }

    return annotatedStringBuilder.toAnnotatedString()
}

/**
 * 模糊匹配函数 - 计算字符串相似度
 */
fun fuzzyMatch(text: String, query: String, threshold: Double = 0.6): Boolean {
    if (query.isEmpty()) return true
    if (text.isEmpty()) return false

    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()

    // 如果包含完整匹配，直接返回 true
    if (lowerText.contains(lowerQuery)) return true

    // 计算编辑距离相似度
    val similarity = calculateSimilarity(lowerText, lowerQuery)
    return similarity >= threshold
}

/**
 * 计算两个字符串的相似度（基于编辑距离）
 */
fun calculateSimilarity(s1: String, s2: String): Double {
    val maxLen = maxOf(s1.length, s2.length)
    if (maxLen == 0) return 1.0

    return (maxLen - levenshteinDistance(s1, s2)) / maxLen.toDouble()
}

/**
 * 计算编辑距离
 */
fun levenshteinDistance(s1: String, s2: String): Int {
    val len1 = s1.length
    val len2 = s2.length

    val matrix = Array(len1 + 1) { IntArray(len2 + 1) }

    for (i in 0..len1) matrix[i][0] = i
    for (j in 0..len2) matrix[0][j] = j

    for (i in 1..len1) {
        for (j in 1..len2) {
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
            matrix[i][j] = minOf(
                matrix[i - 1][j] + 1,      // 删除
                matrix[i][j - 1] + 1,      // 插入
                matrix[i - 1][j - 1] + cost // 替换
            )
        }
    }

    return matrix[len1][len2]
}

/**
 * 模糊匹配的高亮函数
 */
@Composable
private fun animatedFuzzyHighlight(
    text: String,
    searchQuery: String,
    enableAnimation: Boolean
): AnnotatedString {
    if (searchQuery.isEmpty()) {
        return AnnotatedString(text)
    }

    val animatedColors by animateColorAsState(
        targetValue = if (enableAnimation) {
            Color.Cyan.copy(alpha = 0.6f)
        } else {
            Color.Cyan
        },
        animationSpec = tween(500),
        label = "fuzzy_highlight_color"
    )

    val lowerText = text.lowercase()
    val lowerQuery = searchQuery.lowercase()

    // 首先尝试精确匹配
    if (lowerText.contains(lowerQuery)) {
        return animatedHighlightSearchText(text, searchQuery, enableAnimation)
    }

    // 模糊匹配：找到最佳匹配子串
    val bestMatch = findBestFuzzyMatch(text, searchQuery)

    return if (bestMatch != null) {
        val annotatedStringBuilder = AnnotatedString.Builder()
        val (start, end) = bestMatch

        // 添加匹配前的文本
        if (start > 0) {
            annotatedStringBuilder.append(text.take(start))
        }

        // 添加模糊匹配的高亮文本
        annotatedStringBuilder.withStyle(
            style = SpanStyle(
                background = animatedColors,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append(text.substring(start, end))
        }

        // 添加匹配后的文本
        if (end < text.length) {
            annotatedStringBuilder.append(text.substring(end))
        }

        annotatedStringBuilder.toAnnotatedString()
    } else {
        AnnotatedString(text)
    }
}

/**
 * 找到最佳模糊匹配位置
 */
private fun findBestFuzzyMatch(text: String, query: String): Pair<Int, Int>? {
    if (query.length > text.length) return null

    var bestMatch: Pair<Int, Int>? = null
    var bestSimilarity = 0.0
    val minLength = query.length
    val maxLength = minOf(text.length, query.length * 2)

    // 尝试不同长度的子串
    for (length in minLength..maxLength) {
        for (start in 0..text.length - length) {
            val substring = text.substring(start, start + length)
            val similarity = calculateSimilarity(substring.lowercase(), query.lowercase())

            if (similarity > bestSimilarity && similarity >= 0.6) {
                bestSimilarity = similarity
                bestMatch = Pair(start, start + length)
            }
        }
    }

    return bestMatch
}

enum class AnimationStyle(val displayName: String) {
    PULSE("脉冲"),
    GLOW("发光"),
    BOUNCE("弹跳"),
    WAVE("波浪")
}

@Composable
private fun AnimatedSearchResultCard(
    text: String,
    searchQuery: String,
    enableFuzzyMatch: Boolean,
    animationStyle: AnimationStyle,
    index: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "highlight_animation")

    // 不同的动画效果
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_intensity"
    )

    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce_offset"
    )

    Text(
        text = createAnimatedHighlight(
            text = text,
            searchQuery = searchQuery,
            enableFuzzyMatch = enableFuzzyMatch,
            animationStyle = animationStyle,
            pulseAlpha = pulseAlpha,
            glowIntensity = glowIntensity
        ),
        modifier = Modifier.padding(16.dp),
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
}

@Composable
private fun createAnimatedHighlight(
    text: String,
    searchQuery: String,
    enableFuzzyMatch: Boolean,
    animationStyle: AnimationStyle,
    pulseAlpha: Float,
    glowIntensity: Float
): AnnotatedString {
    if (searchQuery.isEmpty()) {
        return AnnotatedString(text)
    }

    val baseColor = when (animationStyle) {
        AnimationStyle.PULSE -> Color.Yellow.copy(alpha = pulseAlpha)
        AnimationStyle.GLOW -> Color.Cyan.copy(alpha = 0.6f + glowIntensity * 0.4f)
        AnimationStyle.BOUNCE -> Color.Green.copy(alpha = 0.7f)
        AnimationStyle.WAVE -> Color.Magenta.copy(alpha = 0.6f)
    }

    return if (enableFuzzyMatch) {
        createFuzzyHighlightAnnotatedString(text, searchQuery, baseColor)
    } else {
        createExactHighlightAnnotatedString(text, searchQuery, baseColor)
    }
}

private fun createExactHighlightAnnotatedString(
    text: String,
    searchQuery: String,
    highlightColor: Color
): AnnotatedString {
    val annotatedStringBuilder = AnnotatedString.Builder()
    var currentIndex = 0

    val lowerText = text.lowercase()
    val lowerQuery = searchQuery.lowercase()

    while (currentIndex < text.length) {
        val foundIndex = lowerText.indexOf(lowerQuery, currentIndex)

        if (foundIndex == -1) {
            annotatedStringBuilder.append(text.substring(currentIndex))
            break
        }

        if (foundIndex > currentIndex) {
            annotatedStringBuilder.append(text.substring(currentIndex, foundIndex))
        }

        annotatedStringBuilder.withStyle(
            style = SpanStyle(
                background = highlightColor,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        ) {
            append(text.substring(foundIndex, foundIndex + searchQuery.length))
        }

        currentIndex = foundIndex + searchQuery.length
    }

    return annotatedStringBuilder.toAnnotatedString()
}

private fun createFuzzyHighlightAnnotatedString(
    text: String,
    searchQuery: String,
    highlightColor: Color
): AnnotatedString {
    val bestMatch = findBestFuzzyMatch(text, searchQuery)

    return if (bestMatch != null) {
        val annotatedStringBuilder = AnnotatedString.Builder()
        val (start, end) = bestMatch

        if (start > 0) {
            annotatedStringBuilder.append(text.substring(0, start))
        }

        annotatedStringBuilder.withStyle(
            style = SpanStyle(
                background = highlightColor,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append(text.substring(start, end))
        }

        if (end < text.length) {
            annotatedStringBuilder.append(text.substring(end))
        }

        annotatedStringBuilder.toAnnotatedString()
    } else {
        // 如果没有找到好的模糊匹配，尝试高亮单个字符
        highlightIndividualCharacters(text, searchQuery, highlightColor)
    }
}

/**
 * 高亮单个匹配字符（用于极端模糊匹配）
 */
private fun highlightIndividualCharacters(
    text: String,
    searchQuery: String,
    highlightColor: Color
): AnnotatedString {
    val annotatedStringBuilder = AnnotatedString.Builder()
    val lowerText = text.lowercase()
    val lowerQuery = searchQuery.lowercase()

    text.forEachIndexed { index, char ->
        if (lowerQuery.contains(char.lowercase())) {
            annotatedStringBuilder.withStyle(
                style = SpanStyle(
                    background = highlightColor.copy(alpha = 0.3f),
                    fontWeight = FontWeight.Medium
                )
            ) {
                append(char)
            }
        } else {
            annotatedStringBuilder.append(char)
        }
    }

    return annotatedStringBuilder.toAnnotatedString()
}
