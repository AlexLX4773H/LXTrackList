package com.lx.tracklist.data.exporter

import com.google.gson.GsonBuilder
import com.lx.tracklist.data.importer.BackupWrapper
import com.lx.tracklist.data.model.MediaEntry
import java.io.OutputStream
import java.io.OutputStreamWriter

object JsonExporter {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun exportToJson(entries: List<MediaEntry>): String {
        val backup = BackupWrapper(
            app = "LX Track List",
            version = 1,
            exportedAt = System.currentTimeMillis(),
            totalCount = entries.size,
            entries = entries
        )
        return gson.toJson(backup)
    }

    fun writeToStream(entries: List<MediaEntry>, outputStream: OutputStream) {
        val json = exportToJson(entries)
        OutputStreamWriter(outputStream, "UTF-8").use {
            it.write(json)
            it.flush()
        }
    }
}
