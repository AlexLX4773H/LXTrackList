package com.lx.tracklist.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StorageFileInfo(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val fileType: String
) {
    val sizeFormatted: String
        get() = when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", sizeBytes / 1024.0)
            else -> String.format(Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024.0))
        }

    val lastModifiedFormatted: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(lastModified))
}

object StorageHelper {

    private const val FOLDER_NAME_PRIMARY = "LXTrackList"
    private const val FOLDER_NAME_TYPO = "LXTractList"

    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val write = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val read = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            write && read
        }
    }

    fun requestStoragePermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }
        }
    }

    fun getBaseDir(): File {
        val root = Environment.getExternalStorageDirectory()
        val typoDir = File(root, FOLDER_NAME_TYPO)
        val primaryDir = File(root, FOLDER_NAME_PRIMARY)

        // If user already created LXTractList folder, support it
        val targetDir = if (typoDir.exists() && typoDir.isDirectory) typoDir else primaryDir

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        return targetDir
    }

    fun getInputFolder(): File {
        val folder = File(getBaseDir(), "input")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }

    fun getOutputFolder(): File {
        val folder = File(getBaseDir(), "output")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }

    fun listInputFiles(): List<StorageFileInfo> {
        val inputDir = getInputFolder()
        val files = inputDir.listFiles() ?: return emptyList()

        return files
            .filter { it.isFile && isSupportedFormat(it.name) }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                val type = detectType(file.name)
                StorageFileInfo(
                    file = file,
                    name = file.name,
                    sizeBytes = file.length(),
                    lastModified = file.lastModified(),
                    fileType = type
                )
            }
    }

    fun exportToJsonFile(jsonContent: String): File {
        val outputDir = getOutputFolder()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val targetFile = File(outputDir, "LX_Track_List_Backup_$timestamp.json")

        FileOutputStream(targetFile).use { fos ->
            fos.write(jsonContent.toByteArray(Charsets.UTF_8))
            fos.flush()
        }
        return targetFile
    }

    fun openFileInputStream(file: File): FileInputStream {
        return FileInputStream(file)
    }

    private fun isSupportedFormat(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".xml") ||
                lower.endsWith(".xml.gz") ||
                lower.endsWith(".gz") ||
                lower.endsWith(".csv") ||
                lower.endsWith(".json") ||
                lower.endsWith(".tachibk")
    }

    private fun detectType(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".tachibk") || lower.endsWith(".proto.gz") -> "Tachiyomi (.tachibk)"
            lower.endsWith(".xml.gz") || (lower.endsWith(".gz") && !lower.endsWith(".proto.gz")) -> "MAL XML (GZ)"
            lower.endsWith(".xml") -> "MAL XML"
            lower.endsWith(".csv") -> "CSV"
            lower.endsWith(".json") -> "JSON"
            else -> "Data"
        }
    }
}
