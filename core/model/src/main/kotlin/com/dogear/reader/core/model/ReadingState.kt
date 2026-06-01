package com.dogear.reader.core.model

/** Reading lifecycle states surfaced on the shelf and used for filtering. */
enum class ReadingState(val displayName: String) {
    UNREAD("Unread"),
    READING("Reading"),
    FINISHED("Finished"),
    ABANDONED("Abandoned"),
}
