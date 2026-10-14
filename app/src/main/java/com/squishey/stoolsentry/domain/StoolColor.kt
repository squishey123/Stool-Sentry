package com.squishey.stoolsentry.domain

enum class StoolColor(
    val label: String,
    val swatchArgb: Long,
    val clinicianFlag: Boolean = false,
) {
    VERY_LIGHT_BROWN("Very light brown", 0xFFE8C4A2),
    LIGHT_BROWN("Light brown", 0xFFD2996B),
    BROWN("Brown", 0xFFC47A3A),
    MEDIUM_BROWN("Medium brown", 0xFFA65B2E),
    DARK_BROWN("Dark brown", 0xFF6B3E26),
    GREEN("Green", 0xFF8A9A5B),
    YELLOW("Yellow", 0xFFF2D34A),
    ORANGE("Orange", 0xFFF08A24),
    RED("Red", 0xFFE23B3B, clinicianFlag = true),
    MAROON("Maroon", 0xFF7A1F2B, clinicianFlag = true),
    GRAY("Gray", 0xFFC8C8C8),
    BLACK("Black", 0xFF1A1A1A, clinicianFlag = true),
}
