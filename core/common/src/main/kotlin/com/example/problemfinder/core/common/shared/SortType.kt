package com.example.problemfinder.core.common.shared

/** Browse ordering sent to the backend; the client preserves the order of the returned page. */
enum class SortType {
    NEWEST,
    OLDEST,
    LARGEST,
    SMALLEST
}
