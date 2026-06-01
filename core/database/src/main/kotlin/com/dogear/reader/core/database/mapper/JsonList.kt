package com.dogear.reader.core.database.mapper

import org.json.JSONArray

/**
 * Tiny JSON string-list codec for columns like `authors_json` / `categories`. Uses the
 * platform `org.json` (no extra dependency). Values can contain commas safely, unlike a
 * delimiter-joined string.
 */
internal object JsonList {

    fun encode(values: List<String>): String? {
        if (values.isEmpty()) return null
        val array = JSONArray()
        values.forEach { array.put(it) }
        return array.toString()
    }

    fun decode(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList(array.length()) {
                for (i in 0 until array.length()) add(array.getString(i))
            }
        }.getOrDefault(emptyList())
    }
}
