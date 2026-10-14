package com.squishey.stoolsentry.domain

enum class BristolType(
    val number: Int,
    val label: String,
    val description: String,
) {
    TYPE_1(1, "Type 1", "Separate hard lumps, like nuts"),
    TYPE_2(2, "Type 2", "Sausage-shaped but lumpy"),
    TYPE_3(3, "Type 3", "Like a sausage but with cracks on the surface"),
    TYPE_4(4, "Type 4", "Like a sausage or snake, smooth and soft"),
    TYPE_5(5, "Type 5", "Soft blobs with clear-cut edges"),
    TYPE_6(6, "Type 6", "Fluffy pieces with ragged edges, a mushy stool"),
    TYPE_7(7, "Type 7", "Watery, no solid pieces, entirely liquid"),
    ;

    companion object {
        fun fromNumber(number: Int): BristolType? = entries.firstOrNull { it.number == number }
    }
}
