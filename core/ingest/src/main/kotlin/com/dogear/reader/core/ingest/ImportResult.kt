package com.dogear.reader.core.ingest

/** Outcome of importing a single source. */
sealed interface ImportResult {
    data class Imported(val bookId: Long) : ImportResult
    data object Duplicate : ImportResult
    data object Unsupported : ImportResult
    data class Failed(val reason: String) : ImportResult

    /** A ZIP archive of books: how many were imported vs. skipped (duplicate/unsupported). */
    data class Archive(val imported: Int, val skipped: Int) : ImportResult
}

/** Aggregate result of importing several sources (multi-select / folder). */
data class BatchImportResult(
    val imported: Int = 0,
    val duplicates: Int = 0,
    val unsupported: Int = 0,
    val failed: Int = 0,
) {
    operator fun plus(result: ImportResult): BatchImportResult = when (result) {
        is ImportResult.Imported -> copy(imported = imported + 1)
        ImportResult.Duplicate -> copy(duplicates = duplicates + 1)
        ImportResult.Unsupported -> copy(unsupported = unsupported + 1)
        is ImportResult.Failed -> copy(failed = failed + 1)
        is ImportResult.Archive -> copy(
            imported = imported + result.imported,
            unsupported = unsupported + result.skipped,
        )
    }
}
