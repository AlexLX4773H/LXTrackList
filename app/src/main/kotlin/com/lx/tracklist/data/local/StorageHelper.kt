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
    private const val PREFS_NAME = "lx_storage_prefs"
    private const val KEY_CUSTOM_STORAGE_PATH = "custom_storage_path"

    @Volatile
    private var cachedCustomPath: String? = null

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        cachedCustomPath = prefs.getString(KEY_CUSTOM_STORAGE_PATH, null)?.trim()?.takeIf { it.isNotBlank() }
    }

    fun getCustomStoragePath(context: Context? = null): String? {
        if (cachedCustomPath != null) return cachedCustomPath
        if (context != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            cachedCustomPath = prefs.getString(KEY_CUSTOM_STORAGE_PATH, null)?.trim()?.takeIf { it.isNotBlank() }
        }
        return cachedCustomPath
    }

    fun setCustomStoragePath(context: Context, path: String?) {
        val cleanPath = path?.trim()?.takeIf { it.isNotBlank() }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (cleanPath != null) {
                putString(KEY_CUSTOM_STORAGE_PATH, cleanPath)
            } else {
                remove(KEY_CUSTOM_STORAGE_PATH)
            }
            apply()
        }
        cachedCustomPath = cleanPath
    }

    fun getDefaultBaseDir(): File {
        val root = Environment.getExternalStorageDirectory()
        val typoDir = File(root, FOLDER_NAME_TYPO)
        val primaryDir = File(root, FOLDER_NAME_PRIMARY)
        return if (typoDir.exists() && typoDir.isDirectory) typoDir else primaryDir
    }

    fun parseTreeUriToPath(uri: Uri): String? {
        val uriStr = uri.toString()
        if (uriStr.contains("primary%3A", ignoreCase = true)) {
            val rel = Uri.decode(uriStr.substringAfter("primary%3A"))
            return "${Environment.getExternalStorageDirectory().absolutePath}/$rel".trimEnd('/')
        }
        val path = uri.path ?: return null
        if (path.contains("primary:")) {
            val rel = path.substringAfter("primary:")
            return "${Environment.getExternalStorageDirectory().absolutePath}/$rel".trimEnd('/')
        }
        return null
    }

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
        val custom = cachedCustomPath
        if (!custom.isNullOrBlank()) {
            val dir = File(custom)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

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

    fun getDataFolder(): File {
        val folder = File(getBaseDir(), "data")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }

    fun listDataFiles(): List<StorageFileInfo> {
        val dataDir = getDataFolder()
        val files = dataDir.listFiles() ?: return emptyList()

        return files
            .filter { it.isFile }
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

    fun saveAvatarImage(context: Context, sourceUri: Uri): String? {
        return try {
            val avatarsDir = File(context.filesDir, "avatars")
            if (!avatarsDir.exists()) {
                avatarsDir.mkdirs()
            }
            val targetFile = File(avatarsDir, "avatar_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        } catch (_: Exception) {
            sourceUri.toString()
        }
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
            lower.endsWith(".txt") || lower.endsWith(".md") -> "Report / Text"
            else -> "Data"
        }
    }
}
