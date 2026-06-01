package com.dogear.reader.feature.settings

import android.content.Context
import android.net.Uri
import com.dogear.reader.core.common.dispatchers.IoDispatcher
import com.dogear.reader.core.database.dao.BackupDao
import com.dogear.reader.core.database.entity.BookCollectionCrossRef
import com.dogear.reader.core.database.entity.BookEntity
import com.dogear.reader.core.database.entity.BookFileEntity
import com.dogear.reader.core.database.entity.BookTagCrossRef
import com.dogear.reader.core.database.entity.BookmarkEntity
import com.dogear.reader.core.database.entity.CollectionEntity
import com.dogear.reader.core.database.entity.HighlightEntity
import com.dogear.reader.core.database.entity.NoteEntity
import com.dogear.reader.core.database.entity.ReadingProgressEntity
import com.dogear.reader.core.database.entity.TagEntity
import com.dogear.reader.core.datastore.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * Exports/restores the full library state (metadata, reading progress, annotations, collections,
 * tags, and settings) as a single JSON document to a user-picked file. Restore is atomic-ish:
 * tables are cleared then repopulated in one transaction-friendly sequence. Book *files* are not
 * embedded — on the same device the stored paths still resolve; cross-device restore brings back
 * everything except the book files themselves.
 */
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupDao: BackupDao,
    private val settingsRepository: SettingsRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun export(uri: Uri): Boolean = withContext(io) {
        runCatching {
            val root = JSONObject()
            root.put("version", BACKUP_VERSION)
            root.put("books", BackupCodec.encodeBooks(backupDao.books()))
            root.put("files", BackupCodec.encodeFiles(backupDao.files()))
            root.put("progress", BackupCodec.encodeProgress(backupDao.progress()))
            root.put("bookmarks", BackupCodec.encodeBookmarks(backupDao.bookmarks()))
            root.put("highlights", BackupCodec.encodeHighlights(backupDao.highlights()))
            root.put("notes", BackupCodec.encodeNotes(backupDao.notes()))
            root.put("collections", BackupCodec.encodeCollections(backupDao.collections()))
            root.put("tags", BackupCodec.encodeTags(backupDao.tags()))
            root.put("bookCollections", BackupCodec.encodeBookCollections(backupDao.bookCollections()))
            root.put("bookTags", BackupCodec.encodeBookTags(backupDao.bookTags()))
            root.put("settings", BackupCodec.encodeSettings(settingsRepository))
            context.contentResolver.openOutputStream(uri)?.use { it.write(root.toString().toByteArray()) }
                ?: error("Cannot open output")
            true
        }.getOrDefault(false)
    }

    suspend fun import(uri: Uri): Boolean = withContext(io) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                ?: error("Cannot open input")
            val root = JSONObject(text)

            backupDao.clearTags()
            backupDao.clearCollections()
            backupDao.clearBooks() // cascades children

            backupDao.restoreBooks(BackupCodec.decodeBooks(root.optJSONArray("books")))
            backupDao.restoreFiles(BackupCodec.decodeFiles(root.optJSONArray("files")))
            backupDao.restoreProgress(BackupCodec.decodeProgress(root.optJSONArray("progress")))
            backupDao.restoreCollections(BackupCodec.decodeCollections(root.optJSONArray("collections")))
            backupDao.restoreTags(BackupCodec.decodeTags(root.optJSONArray("tags")))
            backupDao.restoreBookmarks(BackupCodec.decodeBookmarks(root.optJSONArray("bookmarks")))
            backupDao.restoreHighlights(BackupCodec.decodeHighlights(root.optJSONArray("highlights")))
            backupDao.restoreNotes(BackupCodec.decodeNotes(root.optJSONArray("notes")))
            backupDao.restoreBookCollections(BackupCodec.decodeBookCollections(root.optJSONArray("bookCollections")))
            backupDao.restoreBookTags(BackupCodec.decodeBookTags(root.optJSONArray("bookTags")))
            root.optJSONObject("settings")?.let { BackupCodec.decodeSettings(it, settingsRepository) }
            true
        }.getOrDefault(false)
    }

    private companion object {
        const val BACKUP_VERSION = 1
    }
}

/** JSON (de)serialization for backup entities. Property names are the stable keys. */
private object BackupCodec {

    fun encodeBooks(list: List<BookEntity>) = array(list) { b ->
        JSONObject().apply {
            put("id", b.id); put("contentHash", b.contentHash); put("title", b.title)
            putN("subtitle", b.subtitle); put("author", b.author); putN("authorsJson", b.authorsJson)
            putN("publisher", b.publisher); putN("publishedDate", b.publishedDate)
            putN("description", b.description); putN("isbn", b.isbn); putN("language", b.language)
            putN("series", b.series); putN("seriesIndex", b.seriesIndex); putN("categories", b.categories)
            put("format", b.format); put("fileSize", b.fileSize); putN("coverPath", b.coverPath)
            put("coverIsGenerated", b.coverIsGenerated); put("readingState", b.readingState)
            put("addedAt", b.addedAt); putN("lastOpenedAt", b.lastOpenedAt)
            put("metadataLocked", b.metadataLocked)
        }
    }

    fun decodeBooks(arr: JSONArray?) = list(arr) { o ->
        BookEntity(
            id = o.getLong("id"), contentHash = o.getString("contentHash"), title = o.getString("title"),
            subtitle = o.nString("subtitle"), author = o.getString("author"),
            authorsJson = o.nString("authorsJson"), publisher = o.nString("publisher"),
            publishedDate = o.nString("publishedDate"), description = o.nString("description"),
            isbn = o.nString("isbn"), language = o.nString("language"), series = o.nString("series"),
            seriesIndex = o.nDouble("seriesIndex"), categories = o.nString("categories"),
            format = o.getString("format"), fileSize = o.optLong("fileSize"),
            coverPath = o.nString("coverPath"), coverIsGenerated = o.optBoolean("coverIsGenerated"),
            readingState = o.getString("readingState"), addedAt = o.optLong("addedAt"),
            lastOpenedAt = o.nLong("lastOpenedAt"), metadataLocked = o.optBoolean("metadataLocked"),
        )
    }

    fun encodeFiles(list: List<BookFileEntity>) = array(list) { f ->
        JSONObject().apply {
            put("id", f.id); put("bookId", f.bookId); put("uri", f.uri)
            putN("displayName", f.displayName); putN("mime", f.mime)
            put("contentHash", f.contentHash); put("isAvailable", f.isAvailable)
        }
    }

    fun decodeFiles(arr: JSONArray?) = list(arr) { o ->
        BookFileEntity(
            id = o.getLong("id"), bookId = o.getLong("bookId"), uri = o.getString("uri"),
            displayName = o.nString("displayName"), mime = o.nString("mime"),
            contentHash = o.getString("contentHash"), isAvailable = o.optBoolean("isAvailable", true),
        )
    }

    fun encodeProgress(list: List<ReadingProgressEntity>) = array(list) { p ->
        JSONObject().apply {
            put("bookId", p.bookId); putN("spineIndex", p.spineIndex); putN("selector", p.selector)
            putN("charOffset", p.charOffset); putN("textSnippet", p.textSnippet)
            putN("pageIndex", p.pageIndex); put("progression", p.progression.toDouble())
            putN("chapterTitle", p.chapterTitle); put("updatedAt", p.updatedAt)
        }
    }

    fun decodeProgress(arr: JSONArray?) = list(arr) { o ->
        ReadingProgressEntity(
            bookId = o.getLong("bookId"), spineIndex = o.nInt("spineIndex"), selector = o.nString("selector"),
            charOffset = o.nInt("charOffset"), textSnippet = o.nString("textSnippet"),
            pageIndex = o.nInt("pageIndex"), progression = o.optDouble("progression").toFloat(),
            chapterTitle = o.nString("chapterTitle"), updatedAt = o.optLong("updatedAt"),
        )
    }

    fun encodeBookmarks(list: List<BookmarkEntity>) = array(list) { b ->
        JSONObject().apply {
            put("id", b.id); put("bookId", b.bookId); putN("name", b.name)
            putN("spineIndex", b.spineIndex); putN("selector", b.selector); putN("charOffset", b.charOffset)
            putN("textSnippet", b.textSnippet); putN("pageIndex", b.pageIndex)
            put("progression", b.progression.toDouble()); putN("excerpt", b.excerpt); put("createdAt", b.createdAt)
        }
    }

    fun decodeBookmarks(arr: JSONArray?) = list(arr) { o ->
        BookmarkEntity(
            id = o.getLong("id"), bookId = o.getLong("bookId"), name = o.nString("name"),
            spineIndex = o.nInt("spineIndex"), selector = o.nString("selector"), charOffset = o.nInt("charOffset"),
            textSnippet = o.nString("textSnippet"), pageIndex = o.nInt("pageIndex"),
            progression = o.optDouble("progression").toFloat(), excerpt = o.nString("excerpt"),
            createdAt = o.optLong("createdAt"),
        )
    }

    fun encodeHighlights(list: List<HighlightEntity>) = array(list) { h ->
        JSONObject().apply {
            put("id", h.id); put("bookId", h.bookId); put("color", h.color); put("selectedText", h.selectedText)
            putN("startSpineIndex", h.startSpineIndex); putN("startSelector", h.startSelector)
            putN("startCharOffset", h.startCharOffset); putN("endSpineIndex", h.endSpineIndex)
            putN("endSelector", h.endSelector); putN("endCharOffset", h.endCharOffset)
            putN("textSnippet", h.textSnippet); put("progression", h.progression.toDouble()); put("createdAt", h.createdAt)
        }
    }

    fun decodeHighlights(arr: JSONArray?) = list(arr) { o ->
        HighlightEntity(
            id = o.getLong("id"), bookId = o.getLong("bookId"), color = o.optInt("color"),
            selectedText = o.getString("selectedText"), startSpineIndex = o.nInt("startSpineIndex"),
            startSelector = o.nString("startSelector"), startCharOffset = o.nInt("startCharOffset"),
            endSpineIndex = o.nInt("endSpineIndex"), endSelector = o.nString("endSelector"),
            endCharOffset = o.nInt("endCharOffset"), textSnippet = o.nString("textSnippet"),
            progression = o.optDouble("progression").toFloat(), createdAt = o.optLong("createdAt"),
        )
    }

    fun encodeNotes(list: List<NoteEntity>) = array(list) { n ->
        JSONObject().apply {
            put("id", n.id); put("bookId", n.bookId); putN("highlightId", n.highlightId); put("body", n.body)
            putN("spineIndex", n.spineIndex); putN("selector", n.selector); putN("charOffset", n.charOffset)
            putN("progression", n.progression?.toDouble()); put("createdAt", n.createdAt); put("updatedAt", n.updatedAt)
        }
    }

    fun decodeNotes(arr: JSONArray?) = list(arr) { o ->
        NoteEntity(
            id = o.getLong("id"), bookId = o.getLong("bookId"), highlightId = o.nLong("highlightId"),
            body = o.getString("body"), spineIndex = o.nInt("spineIndex"), selector = o.nString("selector"),
            charOffset = o.nInt("charOffset"), progression = o.nDouble("progression")?.toFloat(),
            createdAt = o.optLong("createdAt"), updatedAt = o.optLong("updatedAt"),
        )
    }

    fun encodeCollections(list: List<CollectionEntity>) = array(list) { c ->
        JSONObject().apply { put("id", c.id); put("name", c.name); put("sortOrder", c.sortOrder); put("createdAt", c.createdAt) }
    }

    fun decodeCollections(arr: JSONArray?) = list(arr) { o ->
        CollectionEntity(id = o.getLong("id"), name = o.getString("name"), sortOrder = o.optInt("sortOrder"), createdAt = o.optLong("createdAt"))
    }

    fun encodeTags(list: List<TagEntity>) = array(list) { t ->
        JSONObject().apply { put("id", t.id); put("name", t.name) }
    }

    fun decodeTags(arr: JSONArray?) = list(arr) { o -> TagEntity(id = o.getLong("id"), name = o.getString("name")) }

    fun encodeBookCollections(list: List<BookCollectionCrossRef>) = array(list) { r ->
        JSONObject().apply { put("bookId", r.bookId); put("collectionId", r.collectionId) }
    }

    fun decodeBookCollections(arr: JSONArray?) = list(arr) { o ->
        BookCollectionCrossRef(bookId = o.getLong("bookId"), collectionId = o.getLong("collectionId"))
    }

    fun encodeBookTags(list: List<BookTagCrossRef>) = array(list) { r ->
        JSONObject().apply { put("bookId", r.bookId); put("tagId", r.tagId) }
    }

    fun decodeBookTags(arr: JSONArray?) = list(arr) { o ->
        BookTagCrossRef(bookId = o.getLong("bookId"), tagId = o.getLong("tagId"))
    }

    suspend fun encodeSettings(repo: SettingsRepository): JSONObject {
        val app = repo.settings.first()
        val reader = repo.readerSettings.first()
        return JSONObject().apply {
            put("themeMode", app.themeMode.name); put("palette", app.palette.name)
            put("uiFont", app.uiFont.name); put("readerFont", app.readerFont.name)
            put("viewMode", app.viewMode.name); put("sort", app.sort.name)
            put("sortAscending", app.sortAscending); put("showTutorialOverlay", app.showTutorialOverlay)
            put("readingTheme", reader.themeId.name)
            put("customBg", reader.custom.background); put("customText", reader.custom.text)
            put("customLink", reader.custom.link); put("customHighlight", reader.custom.highlight)
            put("fontSizeSp", reader.fontSizeSp); put("fontWeight", reader.fontWeight)
            put("lineHeight", reader.lineHeight.toDouble()); put("paragraphSpacingEm", reader.paragraphSpacingEm.toDouble())
            put("marginHorizontalDp", reader.marginHorizontalDp); put("marginVerticalDp", reader.marginVerticalDp)
            put("textAlign", reader.textAlign.name); put("hyphenation", reader.hyphenation)
            put("pageAnimation", reader.pageAnimation.name); put("brightnessMode", reader.brightnessMode.name)
            put("brightnessLevel", reader.brightnessLevel.toDouble()); put("keepAwake", reader.keepAwake.name)
            put("orientation", reader.orientation.name)
        }
    }

    suspend fun decodeSettings(o: JSONObject, repo: SettingsRepository) {
        val app = com.dogear.reader.core.model.AppSettings(
            themeMode = e(o.optString("themeMode"), com.dogear.reader.core.model.ThemeMode.SYSTEM),
            palette = e(o.optString("palette"), com.dogear.reader.core.model.AppPalette.INK),
            uiFont = e(o.optString("uiFont"), com.dogear.reader.core.model.FontChoice.INTER),
            readerFont = e(o.optString("readerFont"), com.dogear.reader.core.model.FontChoice.LITERATA),
            viewMode = e(o.optString("viewMode"), com.dogear.reader.core.model.ViewMode.GRID),
            sort = e(o.optString("sort"), com.dogear.reader.core.model.SortOption.RECENTLY_READ),
            sortAscending = o.optBoolean("sortAscending"),
            showTutorialOverlay = o.optBoolean("showTutorialOverlay", true),
        )
        val reader = com.dogear.reader.core.model.ReaderSettings(
            themeId = e(o.optString("readingTheme"), com.dogear.reader.core.model.ReadingThemeId.CREAM),
            custom = com.dogear.reader.core.model.CustomReadingTheme(
                background = o.optLong("customBg", 0xFFFBF0D9),
                text = o.optLong("customText", 0xFF5F4B32),
                link = o.optLong("customLink", 0xFF8A5A2B),
                highlight = o.optLong("customHighlight", 0xFFFFE08A),
            ),
            fontSizeSp = o.optInt("fontSizeSp", 18), fontWeight = o.optInt("fontWeight", 400),
            lineHeight = o.optDouble("lineHeight", 1.5).toFloat(),
            paragraphSpacingEm = o.optDouble("paragraphSpacingEm", 0.8).toFloat(),
            marginHorizontalDp = o.optInt("marginHorizontalDp", 24), marginVerticalDp = o.optInt("marginVerticalDp", 24),
            textAlign = e(o.optString("textAlign"), com.dogear.reader.core.model.ReaderTextAlign.LEFT),
            hyphenation = o.optBoolean("hyphenation", true),
            pageAnimation = e(o.optString("pageAnimation"), com.dogear.reader.core.model.PageAnimation.NONE),
            brightnessMode = e(o.optString("brightnessMode"), com.dogear.reader.core.model.BrightnessMode.SYSTEM),
            brightnessLevel = o.optDouble("brightnessLevel", 0.5).toFloat(),
            keepAwake = e(o.optString("keepAwake"), com.dogear.reader.core.model.KeepAwakeMode.WHILE_READING),
            orientation = e(o.optString("orientation"), com.dogear.reader.core.model.OrientationLock.AUTO),
        )
        repo.restore(app, reader)
    }

    private inline fun <reified T : Enum<T>> e(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private fun <T> array(list: List<T>, encode: (T) -> JSONObject): JSONArray =
        JSONArray().apply { list.forEach { put(encode(it)) } }

    private fun <T> list(arr: JSONArray?, decode: (JSONObject) -> T): List<T> {
        if (arr == null) return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) add(decode(arr.getJSONObject(i)))
        }
    }

    private fun JSONObject.putN(key: String, value: Any?) {
        if (value != null) put(key, value)
    }

    private fun JSONObject.nString(key: String): String? = if (isNull(key)) null else optString(key)
    private fun JSONObject.nInt(key: String): Int? = if (has(key) && !isNull(key)) getInt(key) else null
    private fun JSONObject.nLong(key: String): Long? = if (has(key) && !isNull(key)) getLong(key) else null
    private fun JSONObject.nDouble(key: String): Double? = if (has(key) && !isNull(key)) getDouble(key) else null
}
