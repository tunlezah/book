package com.dogear.reader.core.database

import androidx.paging.PagingSource
import androidx.sqlite.db.SimpleSQLiteQuery
import com.dogear.reader.core.database.dao.BookDao
import com.dogear.reader.core.database.model.ShelfRow
import com.dogear.reader.core.database.query.LibraryQueryBuilder
import com.dogear.reader.core.model.LibraryQuery
import javax.inject.Inject

/**
 * Bridges a [LibraryQuery] to a Room [PagingSource], keeping the SQLite query plumbing inside
 * the database module. The repository just asks for a source for the current query.
 */
class ShelfPagingSourceFactory @Inject constructor(
    private val bookDao: BookDao,
) {
    fun create(query: LibraryQuery): PagingSource<Int, ShelfRow> {
        val raw = LibraryQueryBuilder.build(query)
        return bookDao.shelf(SimpleSQLiteQuery(raw.sql, raw.args.toTypedArray()))
    }
}
