# Deliverable 6 — Database Schema

Room (SQLite) schema. Designed for thousands of books, indexed for fast sort/filter/search,
with FTS for library + in-book search, and a migration strategy from day one.

---

## 1. Entity Overview

```
books ──< book_tags >── tags
books ──< book_collections >── collections
books ──1:1── reading_progress
books ──1:1── reading_stats
books ──< bookmarks
books ──< highlights ──1:0..1── notes
books ──< notes (standalone allowed)
books ──1:1── book_files (path/uri + content hash)
books_fts (FTS4/5 mirror of searchable fields)
book_text_index (per-book extracted text for in-book search, optional/lazy)
```

---

## 2. Tables

### `books`
| Column | Type | Notes |
|--------|------|------|
| `id` | INTEGER PK | autogenerate |
| `content_hash` | TEXT | unique; for duplicate detection & cache keys |
| `title` | TEXT | indexed |
| `subtitle` | TEXT | |
| `author` | TEXT | indexed (primary/sort author) |
| `authors_json` | TEXT | full author list |
| `publisher` | TEXT | |
| `published_date` | TEXT | ISO where known |
| `description` | TEXT | |
| `isbn` | TEXT | |
| `language` | TEXT | BCP-47 |
| `series` | TEXT | for future grouping |
| `series_index` | REAL | |
| `categories_json` | TEXT | subjects/genres |
| `format` | TEXT | EPUB/EPUB3/PDF/... (enum) |
| `file_size` | INTEGER | bytes; sort key |
| `cover_path` | TEXT | disk cache path (extracted or generated) |
| `cover_is_generated` | INTEGER | 0/1 |
| `reading_state` | TEXT | UNREAD/READING/FINISHED/ABANDONED (enum), indexed |
| `added_at` | INTEGER | epoch ms; sort key |
| `last_opened_at` | INTEGER | epoch ms; "recently read" sort, indexed |
| `metadata_locked` | INTEGER | 0/1 — user-edited metadata not overwritten by refresh |

**Indices:** `content_hash` (unique), `title`, `author`, `reading_state`, `last_opened_at`,
`added_at`, `format`.

### `book_files`
| `id` PK · `book_id` FK · `uri` TEXT (SAF/content URI) · `display_name` · `mime` ·
`content_hash` · `is_available` INTEGER (revalidated on access). | Keeps file location
separate from metadata so storage moves don't churn `books`. |

### `reading_progress` (1:1 with book)
| `book_id` PK/FK · `spine_index` · `selector` · `char_offset` · `text_snippet` ·
`page_index` · `progression` REAL (0..1) · `chapter_title` · `updated_at`. |
Atomic upsert on each save (survives crash/force-kill). |

### `reading_stats` (1:1)
| `book_id` PK/FK · `total_reading_ms` · `session_count` · `pages_turned` ·
`started_at` · `finished_at`. |

### `bookmarks`
| `id` PK · `book_id` FK · `name` TEXT (nullable) · locator fields (spine_index, selector,
char_offset, text_snippet, page_index, progression) · `excerpt` TEXT · `created_at`. |
Searchable by name/excerpt (joined into FTS). |

### `highlights`
| `id` PK · `book_id` FK · `color` INTEGER · `selected_text` TEXT · locator-start +
locator-end fields · `text_snippet` (anchor for re-import) · `created_at`. |

### `notes`
| `id` PK · `book_id` FK · `highlight_id` FK (nullable → standalone) · `body` TEXT ·
locator fields (for standalone) · `created_at` · `updated_at`. |

### `collections`
| `id` PK · `name` · `sort_order` · `created_at`. |
### `book_collections`
| `book_id` FK · `collection_id` FK · PK(book_id, collection_id). |

### `tags`
| `id` PK · `name` UNIQUE. |
### `book_tags`
| `book_id` FK · `tag_id` FK · PK(book_id, tag_id). |

### `books_fts` (FTS4/5, external-content over `books`)
Indexes `title`, `author`, `description`, `series`, `categories_json` plus tag names →
instant library search. Kept in sync via triggers or DAO writes.

### `book_text_index` (optional, lazy)
| `book_id` FK · `spine_index` · `plain_text` TEXT (FTS) | Built on demand for in-book
search; can be cleared by cache management and rebuilt.

---

## 3. Type Converters & Enums
- Enums (`BookFormat`, `ReadingState`) stored as TEXT via converters.
- Lists stored as JSON TEXT (`authors_json`, `categories_json`) — small, read-mostly.
- All timestamps epoch-ms `INTEGER`.

---

## 4. Query Patterns (designed for indices)
- **Shelf list:** `SELECT ... FROM books ORDER BY <sortKey>` paged via Paging 3, filtered by
  joins to `book_tags`/`book_collections` and `WHERE reading_state/format/author = ?`.
- **Search:** `books_fts MATCH ?` joined back to `books`.
- **Recently read:** `ORDER BY last_opened_at DESC`.
- **Progress save:** single-row upsert into `reading_progress` (cheap, frequent).
- **In-book search:** `book_text_index MATCH ?` for the active book.

All list queries return `Flow`/`PagingSource` so the UI updates reactively and never blocks.

---

## 5. Migrations
- Versioned Room schema with `exportSchema = true` (schemas committed under
  `:core:database/schemas/` for diffable history).
- Explicit `Migration` objects for every version bump; **no destructive fallback** in
  release builds (data loss is unacceptable for a personal library).
- Migration tests via `MigrationTestHelper` (see [09-testing-strategy.md](09-testing-strategy.md)).

---

## 6. Integrity & Durability
- Foreign keys ON with cascade delete (deleting a book removes its progress/annotations).
- Progress/annotation writes use transactions; WAL mode for concurrent read during indexing.
- Backup/restore (Deliverable, [10-implementation-plan.md](10-implementation-plan.md))
  serializes these tables + DataStore prefs to a portable archive and restores atomically.
