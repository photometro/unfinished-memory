package com.unfinished.app.data.media

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.security.KeyStore
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

/** All destructive fixtures have their own private directory AND Keystore alias. */
@RunWith(AndroidJUnit4::class)
class MediaRuntimeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun originalAndTransparentPreviewSurviveSourceDeletionAndRecreation() = runBlocking {
        fixture { fixture ->
            val source = fixture.source("source.png", png(128, 96, transparent = true))
            val sourceBytes = source.readBytes()
            val store = fixture.store()
            val media = store.importPhoto("memory-a", "asset-a", Uri.fromFile(source), 0)
            assertEquals("image/png", media.mimeType)
            assertEquals(128, media.width)
            assertEquals(96, media.height)
            assertEquals(sourceBytes.size.toLong(), media.originalBytes)
            assertEquals(64, media.cipherHash.length)
            assertEquals(64, media.previewCipherHash.length)
            assertFalse(media.originalPath == media.previewPath)
            assertTrue(source.delete()) // Only the synthetic source, never a user's gallery file.
            val reopened = fixture.store()
            assertArrayEquals(sourceBytes, reopened.originalBytes("memory-a", "asset-a", media.originalPath))
            reopened.verify("memory-a", "asset-a", media.originalPath, media.previewPath)
            reopened.preview("memory-a", "asset-a", media.previewPath).also { preview ->
                assertEquals(128, preview.width)
                assertEquals(96, preview.height)
                assertEquals(0, Color.alpha(preview.getPixel(0, 0)))
                assertTrue(preview.hasAlpha())
                preview.recycle()
            }
            assertTrue(fixture.media.listFiles().orEmpty().all { it.name.endsWith(".ufmi") })
            assertFalse(File(fixture.media, media.originalPath).readBytes().contentEquals(sourceBytes))
        }
    }

    @Test fun exifOrientationIsAppliedOnlyToIndependentPreview() = runBlocking {
        fixture { fixture ->
            val sourceBytes = orientedJpeg()
            val source = fixture.source("turned.jpg", sourceBytes)
            val store = fixture.store()
            val media = store.importPhoto("memory-a", "asset-a", Uri.fromFile(source), 0)
            assertArrayEquals(sourceBytes, store.originalBytes("memory-a", "asset-a", media.originalPath))
            store.preview("memory-a", "asset-a", media.previewPath).also { preview ->
                assertEquals(80, preview.width)
                assertEquals(120, preview.height)
                preview.recycle()
            }
            assertTrue(source.exists())
            store.discard(listOf(media.originalPath, media.previewPath))
            assertTrue("discard may not remove the gallery/source fixture", source.exists())
        }
    }

    @Test fun everyFileBoundaryFailureCleansOnlyThisImportAndCanRetry() = runBlocking {
        fixture { fixture ->
            val source = fixture.source("source.png", png(64, 64))
            val good = fixture.store().importPhoto("memory-a", "keep", Uri.fromFile(source), 0)
            val keep = setOf(good.originalPath, good.previewPath)
            listOf("before-copy", "original-chunk", "original-written", "original-committed",
                "preview-chunk", "preview-written", "preview-committed", "verified").forEach { stage ->
                val failed = runCatching {
                    fixture.store(fault = { if (it == stage) throw IOException("synthetic fault") })
                        .importPhoto("memory-a", "retry", Uri.fromFile(source), good.originalBytes)
                }
                assertTrue("fault must be visible at $stage", failed.exceptionOrNull() is MediaStoreException)
                assertEquals("failure at $stage must preserve unrelated valid files", keep,
                    fixture.media.listFiles().orEmpty().map { it.name }.toSet())
            }
            val retry = fixture.store().importPhoto("memory-a", "retry", Uri.fromFile(source), good.originalBytes)
            fixture.store().verify("memory-a", "retry", retry.originalPath, retry.previewPath)
            fixture.store().verify("memory-a", "keep", good.originalPath, good.previewPath)
        }
    }

    @Test fun recoveryPreservesCurrentReferencesAndRemovesOnlyRecognizedOrphans() = runBlocking {
        fixture { fixture ->
            val source = fixture.source("source.png", png(48, 32))
            val store = fixture.store()
            val keep = store.importPhoto("memory-a", "keep", Uri.fromFile(source), 0)
            val orphan = store.importPhoto("memory-a", "unregistered", Uri.fromFile(source), keep.originalBytes)
            val interrupted = File(fixture.media, "${UUID.randomUUID()}-original.ufmi.part")
            interrupted.writeBytes(byteArrayOf(85, 70, 77, 73, 1, 1, 12))
            val unknown = File(fixture.media, "not-an-owned-file.bin")
            unknown.writeBytes(byteArrayOf(1, 2, 3))
            val references = setOf(keep.originalPath, keep.previewPath)
            store.recover(references)
            references.forEach { assertTrue(File(fixture.media, it).exists()) }
            assertFalse(File(fixture.media, orphan.originalPath).exists())
            assertFalse(File(fixture.media, orphan.previewPath).exists())
            assertFalse(interrupted.exists())
            assertTrue(unknown.exists())
            store.verify("memory-a", "keep", keep.originalPath, keep.previewPath)
            val snapshot = fixture.media.listFiles().orEmpty().map { it.name }.toSet()
            assertTrue(runCatching { store.recover(setOf("../escaped.ufmi")) }.isFailure)
            assertTrue(runCatching { store.discard(listOf("../escaped.ufmi")) }.isFailure)
            assertEquals(snapshot, fixture.media.listFiles().orEmpty().map { it.name }.toSet())
        }
    }

    @Test fun missingKeyAndAuthenticationFailurePreserveFilesAndNeverRegenerateKey() = runBlocking {
        fixture { fixture ->
            val source = fixture.source("source.png", png(64, 48))
            val store = fixture.store()
            val media = store.importPhoto("memory-a", "asset-a", Uri.fromFile(source), 0)
            val originalFile = File(fixture.media, media.originalPath)
            val packed = originalFile.readBytes()
            originalFile.writeBytes(packed.clone().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() })
            assertTrue(runCatching { store.verify("memory-a", "asset-a", media.originalPath, media.previewPath) }.isFailure)
            assertTrue(runCatching { store.originalBytes("memory-a", "asset-a", media.originalPath) }.isFailure)
            assertTrue(originalFile.exists())
            originalFile.writeBytes(packed)
            assertTrue(runCatching { store.preview("wrong-memory", "asset-a", media.previewPath) }.isFailure)
            fixture.deleteOwnKey()
            assertTrue(runCatching { store.preview("memory-a", "asset-a", media.previewPath) }.isFailure)
            assertTrue(runCatching { store.importPhoto("memory-b", "other", Uri.fromFile(source), 0) }.isFailure)
            assertFalse(fixture.keyStore().containsAlias(fixture.alias))
            assertTrue(originalFile.exists())
            assertTrue(File(fixture.media, media.previewPath).exists())
        }
    }

    @Test fun corruptUnsupportedOversizePixelAndSpaceInputsAreVisibleFailures() = runBlocking {
        fixture { fixture ->
            val source = fixture.source("source.png", png(64, 48))
            suspend fun refused(file: File, limits: MediaLimits = MediaLimits(), permanent: Boolean = true) {
                val error = runCatching { fixture.store(limits).importPhoto("memory-a", UUID.randomUUID().toString(), Uri.fromFile(file), 0) }.exceptionOrNull()
                assertTrue(error is MediaStoreException)
                assertEquals(permanent, (error as MediaStoreException).invalidInput)
                assertEquals(0, fixture.media.listFiles().orEmpty().size)
                assertTrue(file.exists())
            }
            refused(fixture.source("broken.jpg", byteArrayOf(0, 1, 2, 3)))
            // A single-frame GIF is also outside the confirmed static format scope.
            refused(fixture.source("static.gif", android.util.Base64.decode(
                "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7", android.util.Base64.DEFAULT)))
            refused(source, MediaLimits(maxOriginalBytes = 16))
            refused(source, MediaLimits(minFreeReserveBytes = Long.MAX_VALUE / 2), permanent = false)
            refused(fixture.source("huge.png", oversizedPngHeader()))
        }
    }

    @Test fun maximumPixelGuardrailUsesBoundedOutputWithoutFullSizeBitmap(): Unit = runBlocking {
        fixture { fixture ->
            // Streams 10,000 scanlines, never allocates an 800 MB source Bitmap or raw image.
            val source = fixture.source("200mp.png", streamedPng(20_000, 10_000))
            val store = fixture.store()
            val imported = store.importPhoto("memory-a", "200mp", Uri.fromFile(source), 0)
            assertEquals(20_000, imported.width)
            assertEquals(10_000, imported.height)
            assertTrue(imported.originalBytes < MediaLimits().maxOriginalBytes)
            store.preview("memory-a", "200mp", imported.previewPath).also { preview ->
                assertEquals(1024, preview.width)
                assertEquals(512, preview.height)
                assertTrue(preview.allocationByteCount <= 1024 * 1024 * 4)
                preview.recycle()
            }
        }
    }

    @Test fun actualConfiguredOriginalAndAggregateBoundaryStayBounded(): Unit = runBlocking {
        fixture { fixture ->
            val limits = MediaLimits()
            val source = File(fixture.directory, "byte-boundary.png")
            val small = png(8, 8)
            // A legal, unknown ancillary chunk makes a real large original,
            // without a giant bitmap or a source-sized heap buffer.
            val payloadSize = (limits.maxOriginalBytes - small.size - 12).toInt()
            val type = "npAD".toByteArray(Charsets.US_ASCII)
            val crc = CRC32().apply { update(type) }
            DataOutputStream(source.outputStream().buffered()).use { output ->
                output.write(small, 0, small.size - 12)
                output.writeInt(payloadSize); output.write(type)
                val chunk = ByteArray(16 * 1024)
                var remaining = payloadSize
                while (remaining > 0) {
                    val count = minOf(remaining, chunk.size)
                    output.write(chunk, 0, count); crc.update(chunk, 0, count)
                    remaining -= count
                }
                output.writeInt(crc.value.toInt())
                output.write(small, small.size - 12, 12)
            }
            assertEquals(limits.maxOriginalBytes, source.length())
            val store = fixture.store()
            val existing = limits.maxMemoryOriginalBytes - source.length()
            val imported = store.importPhoto("memory-a", "boundary", Uri.fromFile(source), existing)
            assertEquals(limits.maxOriginalBytes, imported.originalBytes)
            store.verify("memory-a", "boundary", imported.originalPath, imported.previewPath)
            store.preview("memory-a", "boundary", imported.previewPath).also {
                assertEquals(8, it.width); assertEquals(8, it.height); it.recycle()
            }
            val paths = fixture.media.listFiles()!!.map { it.name }.toSet()
            val aggregate = runCatching {
                store.importPhoto("memory-a", "aggregate-over", Uri.fromFile(source), existing + 1)
            }.exceptionOrNull()
            assertTrue(aggregate is MediaStoreException && !aggregate.invalidInput)
            source.appendBytes(byteArrayOf(0))
            val oversized = runCatching {
                store.importPhoto("memory-a", "original-over", Uri.fromFile(source), 0)
            }.exceptionOrNull()
            assertTrue(oversized is MediaStoreException && oversized.invalidInput)
            // Lock down the original OOM trigger as an explicit early rejection.
            java.io.RandomAccessFile(source, "rw").use { it.setLength(64L * MediaLimits.MIB) }
            val oldCandidate = runCatching {
                store.importPhoto("memory-a", "old-64mib-candidate", Uri.fromFile(source), 0)
            }.exceptionOrNull()
            assertTrue(oldCandidate is MediaStoreException && oldCandidate.invalidInput)
            assertEquals(paths, fixture.media.listFiles()!!.map { it.name }.toSet())
            store.verify("memory-a", "boundary", imported.originalPath, imported.previewPath)
        }
    }

    @Test fun oneLargeCompressedInputIsSubsampledAndTwentyImportsDoNotRetainBitmaps() = runBlocking {
        fixture { fixture ->
            val largeSource = fixture.source("large.png", png(4000, 3000))
            val store = fixture.store()
            val large = store.importPhoto("memory-a", "large", Uri.fromFile(largeSource), 0)
            assertEquals(4000, large.width)
            assertEquals(3000, large.height)
            store.preview("memory-a", "large", large.previewPath).also { preview ->
                assertEquals(1024, preview.width)
                assertEquals(768, preview.height)
                assertTrue(preview.allocationByteCount <= 1024 * 1024 * 4)
                preview.recycle()
            }
            var total = large.originalBytes
            val smallSource = fixture.source("small.png", png(128, 96))
            repeat(19) { index ->
                val imported = store.importPhoto("memory-a", "asset-$index", Uri.fromFile(smallSource), total)
                total += imported.originalBytes
                store.preview("memory-a", "asset-$index", imported.previewPath).recycle()
            }
            assertEquals(40, fixture.media.listFiles().orEmpty().size)
        }
    }

    @Test fun cancellationDoesNotBecomeFalseSuccessOrRemovePreviousAttachment() = runBlocking {
        fixture { fixture ->
            val source = fixture.source("source.png", png(64, 48))
            val saved = fixture.store().importPhoto("memory-a", "saved", Uri.fromFile(source), 0)
            val result = runCatching {
                fixture.store(fault = { if (it == "original-committed") throw CancellationException("synthetic cancellation") })
                    .importPhoto("memory-a", "cancelled", Uri.fromFile(source), saved.originalBytes)
            }
            assertTrue(result.exceptionOrNull() is CancellationException)
            assertEquals(setOf(saved.originalPath, saved.previewPath), fixture.media.listFiles().orEmpty().map { it.name }.toSet())
            fixture.store().verify("memory-a", "saved", saved.originalPath, saved.previewPath)
        }
    }

    @Test fun stricterNewImportLimitDoesNotInvalidateExistingV1OrItsPreview(): Unit = runBlocking {
        fixture { fixture ->
            val bytes = png(64, 48)
            val source = fixture.source("existing.png", bytes)
            val imported = fixture.store().importPhoto("memory-a", "existing", Uri.fromFile(source), 0)
            val reader = fixture.store(MediaLimits(maxOriginalBytes = 16))
            assertArrayEquals(bytes, reader.originalBytes("memory-a", "existing", imported.originalPath))
            reader.verify("memory-a", "existing", imported.originalPath, imported.previewPath)
            val limitedHeap = LocalMediaStore(context, "synthetic-vault", fixture.media,
                MediaLimits(maxOriginalBytes = 16), keyAlias = fixture.alias, availableHeapBytes = { 0 })
            val rejected = runCatching {
                limitedHeap.verify("memory-a", "existing", imported.originalPath, imported.previewPath)
            }.exceptionOrNull()
            assertTrue(rejected is MediaStoreException && !rejected.invalidInput)
            assertTrue(rejected!!.message!!.contains("内存"))
            assertTrue(File(fixture.media, imported.originalPath).isFile)
            limitedHeap.preview("memory-a", "existing", imported.previewPath).also {
                assertEquals(64, it.width); assertEquals(48, it.height); it.recycle()
            }
            reader.verify("memory-a", "existing", imported.originalPath, imported.previewPath)
        }
    }

    private inner class Fixture(val directory: File, val alias: String) {
        val media = File(directory, "media")
        fun store(limits: MediaLimits = MediaLimits(), fault: (String) -> Unit = {}) =
            LocalMediaStore(context, "synthetic-vault", media, limits, fault, alias)
        fun source(name: String, bytes: ByteArray) = File(directory, name).apply { writeBytes(bytes) }
        fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        fun deleteOwnKey() {
            require(alias.startsWith("unfinished.test.media."))
            keyStore().deleteEntry(alias)
        }
    }

    private suspend fun <T> fixture(block: suspend (Fixture) -> T): T {
        val id = UUID.randomUUID().toString()
        val parent = context.noBackupFilesDir.canonicalFile
        val directory = File(parent, "item12-media-test-$id").canonicalFile
        require(directory.parentFile == parent && directory.name.startsWith("item12-media-test-"))
        assertTrue(directory.mkdirs())
        val fixture = Fixture(directory, "unfinished.test.media.$id")
        try { return block(fixture) } finally {
            fixture.deleteOwnKey()
            require(directory.parentFile == parent && directory.name == "item12-media-test-$id")
            directory.deleteRecursively()
        }
    }

    private fun png(width: Int, height: Int, transparent: Boolean = false): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(if (transparent) Color.TRANSPARENT else Color.rgb(24, 123, 180))
            if (transparent) bitmap.setPixel(width / 2, height / 2, Color.BLUE)
            return ByteArrayOutputStream().use { output ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray()
            }
        } finally { bitmap.recycle() }
    }

    private fun oversizedPngHeader(): ByteArray = png(2, 2).also { bytes ->
        ByteBuffer.wrap(bytes, 16, 8).putInt(20_000).putInt(20_000)
        val crc = CRC32().apply { update(bytes, 12, 17) }.value.toInt()
        ByteBuffer.wrap(bytes, 29, 4).putInt(crc)
    }

    private fun streamedPng(width: Int, height: Int): ByteArray {
        val compressed = ByteArrayOutputStream()
        val deflater = Deflater(1)
        try {
            DeflaterOutputStream(compressed, deflater).use { output ->
                val row = ByteArray(width * 4 + 1)
                // PNG filter byte 0, RGBA=(0,0,0,255); retained within one scanline buffer.
                for (x in 0 until width) row[1 + x * 4 + 3] = 0xff.toByte()
                repeat(height) { output.write(row) }
            }
        } finally { deflater.end() }
        return ByteArrayOutputStream().use { png ->
            val output = DataOutputStream(png)
            output.write(byteArrayOf(0x89.toByte(), 80, 78, 71, 13, 10, 26, 10))
            fun chunk(name: String, payload: ByteArray) {
                val type = name.toByteArray(Charsets.US_ASCII)
                output.writeInt(payload.size)
                output.write(type)
                output.write(payload)
                val crc = CRC32().apply { update(type); update(payload) }
                output.writeInt(crc.value.toInt())
            }
            val header = ByteBuffer.allocate(13).putInt(width).putInt(height)
                .put(8.toByte()).put(6.toByte()).put(0.toByte()).put(0.toByte()).put(0.toByte()).array()
            chunk("IHDR", header)
            chunk("IDAT", compressed.toByteArray())
            chunk("IEND", byteArrayOf())
            png.toByteArray()
        }
    }

    private fun orientedJpeg(): ByteArray {
        val bitmap = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(25, 110, 160))
        val jpeg = try {
            ByteArrayOutputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
                output.toByteArray()
            }
        } finally { bitmap.recycle() }
        // EXIF little-endian TIFF, IFD0 orientation SHORT=6 (90 degrees clockwise).
        val exif = byteArrayOf(69, 120, 105, 102, 0, 0, 73, 73, 42, 0, 8, 0, 0, 0,
            1, 0, 18, 1, 3, 0, 1, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0)
        val length = exif.size + 2
        return jpeg.copyOfRange(0, 2) + byteArrayOf(0xff.toByte(), 0xe1.toByte(),
            (length shr 8).toByte(), length.toByte()) + exif + jpeg.copyOfRange(2, jpeg.size)
    }
}
