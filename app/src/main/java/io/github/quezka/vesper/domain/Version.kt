package io.github.quezka.vesper.domain

/** A dotted version such as 0.2.1; tags like "v0.2.1" are accepted. */
data class Version(val parts: List<Int>) : Comparable<Version> {
    override fun compareTo(other: Version): Int {
        for (i in 0 until maxOf(parts.size, other.parts.size)) {
            val c = (parts.getOrElse(i) { 0 }).compareTo(other.parts.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }

    override fun toString() = parts.joinToString(".")

    companion object {
        fun parse(text: String): Version? {
            val core = text.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore('+')
            val nums = core.split('.').map { it.toIntOrNull() ?: return null }
            return if (nums.isEmpty()) null else Version(nums)
        }
    }
}
