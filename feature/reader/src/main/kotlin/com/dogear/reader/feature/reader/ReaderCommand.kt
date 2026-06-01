package com.dogear.reader.feature.reader

/** Navigation intents sent from the reader UI (tap zones / TOC) to the active render engine. */
sealed interface ReaderCommand {
    data object Next : ReaderCommand
    data object Previous : ReaderCommand
    data class GoTo(val spineIndex: Int, val fraction: Float) : ReaderCommand

    /** Jump to an overall position (0..1). Both engines honor it — used by bookmarks and the scrubber. */
    data class GoToProgression(val progression: Float) : ReaderCommand

    /** Reflow only: read the current text selection and report it via the highlight callback. */
    data object RequestHighlight : ReaderCommand
}
