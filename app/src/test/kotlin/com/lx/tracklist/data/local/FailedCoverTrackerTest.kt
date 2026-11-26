package com.lx.tracklist.data.local

import com.lx.tracklist.data.model.MediaEntry
import com.lx.tracklist.data.model.MediaStatus
import com.lx.tracklist.data.model.MediaType
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

class FailedCoverTrackerTest {

    private lateinit var originalClient: OkHttpClient
    private lateinit var originalContentChecker: (android.content.Context?, String) -> Boolean

    @Before
    fun setUp() {
        originalClient = FailedCoverTracker.probeHttpClient
        originalContentChecker = FailedCoverTracker.contentUriChecker
        FailedCoverTracker.resetForTest()
    }

    @After
    fun tearDown() {
        FailedCoverTracker.probeHttpClient = originalClient
        FailedCoverTracker.contentUriChecker = originalContentChecker
        FailedCoverTracker.resetForTest()
    }

    private fun entryWithCover(id: Long, url: String): MediaEntry = MediaEntry(
        id = id,
        title = "Entry $id",
        mediaType = MediaType.ANIME,
        status = MediaStatus.WATCHING,
        coverImageUri = url
    )

    @Test
    fun `data image and readable content uris are not flagged broken and bypass http client`() = runBlocking {
        val httpRequests = CopyOnWriteArrayList<String>()
        FailedCoverTracker.probeHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                httpRequests.add("${chain.request().method} ${chain.request().url}")
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(500)
                    .message("Should not probe non-HTTP schemes")
                    .body("".toResponseBody("text/plain".toMediaType()))
                    .build()
            }
            .build()

        FailedCoverTracker.contentUriChecker = { _, uri -> uri.endsWith("/valid") }

        val dataUri = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUg=="
        val validContentUri = "content://media/external/images/valid"
        val brokenContentUri = "content://media/external/images/revoked"

        val entries = listOf(
            entryWithCover(1L, dataUri),
            entryWithCover(2L, validContentUri),
            entryWithCover(3L, brokenContentUri)
        )

        val brokenIds = FailedCoverTracker.probeBrokenCovers(entries, context = null)

        assertEquals(setOf(3L), brokenIds)
        assertTrue(httpRequests.isEmpty())
        assertFalse(FailedCoverTracker.checkUrlBroken(dataUri))
        assertFalse(FailedCoverTracker.checkUrlBroken(validContentUri))
        assertTrue(FailedCoverTracker.checkUrlBroken(brokenContentUri))
    }

    @Test
    fun `head 403 and 405 retry with ranged get and succeed on 200 or 206`() = runBlocking {
        val calls = CopyOnWriteArrayList<Triple<String, String, String?>>()
        FailedCoverTracker.probeHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                val path = req.url.encodedPath
                calls.add(Triple(req.method, path, req.header("Range")))

                val code = when {
                    path == "/cdn-405" && req.method == "HEAD" -> 405
                    path == "/cdn-405" && req.method == "GET" && req.header("Range") == "bytes=0-1024" -> 206
                    path == "/cdn-403" && req.method == "HEAD" -> 403
                    path == "/cdn-403" && req.method == "GET" && req.header("Range") == "bytes=0-1024" -> 200
                    else -> 404
                }
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("Mock $code")
                    .body(ByteArray(4).toResponseBody("image/jpeg".toMediaType()))
                    .build()
            }
            .build()

        val e1 = entryWithCover(10L, "https://cdn.example/cdn-405")
        val e2 = entryWithCover(11L, "https://cdn.example/cdn-403")
        val e3 = entryWithCover(12L, "https://cdn.example/missing-404")

        val brokenIds = FailedCoverTracker.probeBrokenCovers(listOf(e1, e2, e3), context = null)

        assertEquals(setOf(12L), brokenIds)
        assertFalse(FailedCoverTracker.isEntryCoverBroken(e1))
        assertFalse(FailedCoverTracker.isEntryCoverBroken(e2))
        assertTrue(FailedCoverTracker.isEntryCoverBroken(e3))

        // Verify HEAD was followed by GET with Range: bytes=0-1024 for each non-2xx HEAD
        assertTrue(calls.contains(Triple("HEAD", "/cdn-405", null)))
        assertTrue(calls.contains(Triple("GET", "/cdn-405", "bytes=0-1024")))
        assertTrue(calls.contains(Triple("HEAD", "/cdn-403", null)))
        assertTrue(calls.contains(Triple("GET", "/cdn-403", "bytes=0-1024")))
    }

    @Test
    fun `file uris are checked on filesystem without http calls`() = runBlocking {
        val tempFile = File.createTempFile("cover_test_", ".jpg")
        try {
            val missingPath = tempFile.absolutePath + ".missing"
            val entries = listOf(
                entryWithCover(20L, tempFile.toURI().toString()),
                entryWithCover(21L, File(missingPath).toURI().toString())
            )
            val brokenIds = FailedCoverTracker.probeBrokenCovers(entries, context = null)
            assertEquals(setOf(21L), brokenIds)
        } finally {
            tempFile.delete()
        }
    }
}
