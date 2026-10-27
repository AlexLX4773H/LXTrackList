package com.lx.tracklist.data.importer

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lx.tracklist.data.model.MediaEntry
import java.io.InputStream
import java.io.InputStreamReader

data class BackupWrapper(
    val app: String? = null,
    val version: Int? = null,
    val exportedAt: Long? = null,
    val totalCount: Int? = null,
    val entries: List<MediaEntry>? = null
)

object JsonImporter {

    private val gson = Gson()

    fun parse(inputStream: InputStream): List<MediaEntry> {
        val reader = InputStreamReader(inputStream, "UTF-8")
        val content = reader.readText().trim()

        return try {
            if (content.startsWith("{")) {
                val wrapper = gson.fromJson(content, BackupWrapper::class.java)
                wrapper.entries ?: emptyList()
            } else if (content.startsWith("[")) {
                val listType = object : TypeToken<List<MediaEntry>>() {}.type
                gson.fromJson(content, listType) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
