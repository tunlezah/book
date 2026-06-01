package com.dogear.reader.feature.reader

/** Navigation intents sent from the reader UI (tap zones / TOC) to the active render engine. */
sealed interface ReaderCommand {
    data object Next : ReaderCommand
    data object Previous : ReaderCommand
    data class GoTo(val spineIndex: Int, val fraction: Float) : ReaderCommand
}
