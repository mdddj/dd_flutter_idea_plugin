package shop.itbug.fluttercheckversionx.util

data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {
    override fun compareTo(other: Version): Int {
        return compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
    }

    companion object {
        fun parse(versionStr: String): Version {
            val parts = versionStr.split('.').map { it.toIntOrNull() ?: 0 }
            return Version(parts.getOrElse(0) { 0 }, parts.getOrElse(1) { 0 }, parts.getOrElse(2) { 0 })
        }
    }
}

data class VersionInterval(
    val start: Version?, val startInclusive: Boolean, val end: Version?, val endInclusive: Boolean
) {
    companion object {
        fun all() = VersionInterval(null, true, null, true)
        fun exact(version: Version) = VersionInterval(version, true, version, true)
        fun greaterThan(version: Version) = VersionInterval(version, false, null, true)
        fun greaterThanOrEqual(version: Version) = VersionInterval(version, true, null, true)
        fun lessThan(version: Version) = VersionInterval(null, true, version, false)
        fun lessThanOrEqual(version: Version) = VersionInterval(null, true, version, true)
        fun caret(version: Version) = VersionInterval(
            version, true, Version(version.major + 1, 0, 0), false
        )
    }
}

fun parseConstraint(constraint: String): VersionInterval = when {
    constraint.startsWith("^") -> VersionInterval.caret(Version.parse(constraint.drop(1)))
    constraint.startsWith(">=") -> VersionInterval.greaterThanOrEqual(Version.parse(constraint.drop(2)))
    constraint.startsWith(">") -> VersionInterval.greaterThan(Version.parse(constraint.drop(1)))
    constraint.startsWith("<=") -> VersionInterval.lessThanOrEqual(Version.parse(constraint.drop(2)))
    constraint.startsWith("<") -> VersionInterval.lessThan(Version.parse(constraint.drop(1)))
    else -> VersionInterval.exact(Version.parse(constraint))
}

fun intersect(a: VersionInterval, b: VersionInterval): VersionInterval? {
    val start = listOf(a.start to a.startInclusive, b.start to b.startInclusive).filter { it.first != null }
        .maxWithOrNull(compareBy { it.first!! }) ?: (null to true)

    val end = listOf(a.end to a.endInclusive, b.end to b.endInclusive).filter { it.first != null }
        .minWithOrNull(compareBy { it.first!! }) ?: (null to true)

    if (start.first != null && end.first != null && start.first!! > end.first!!) return null
    if (start.first == end.first && !(start.second && end.second)) return null

    return VersionInterval(
        start.first, start.second, end.first, end.second
    )
}

/**
 * 判断[constraint]版本号是否大于[version]
 */
fun isVersionGreaterThanThree(constraint: String, version: Version): Boolean {
    val intervals: List<VersionInterval?> = constraint.split(' ').map(::parseConstraint)
    if (intervals.isEmpty()) return false

    val merged = intervals.reduceOrNull { acc, interval ->
        interval?.let { intersect(acc ?: return@reduceOrNull null, it) } ?: return false
    } ?: return false

    val gtThree = VersionInterval.greaterThan(version)
    return intersect(merged, gtThree) != null
}