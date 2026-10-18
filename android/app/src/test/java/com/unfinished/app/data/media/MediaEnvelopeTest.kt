package com.unfinished.app.data.media

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.crypto.spec.SecretKeySpec

class MediaEnvelopeTest {
    private val key = SecretKeySpec(ByteArray(32) { (it * 7).toByte() }, "AES")
    private val binding = MediaEnvelope.Binding("vault-a", "memory-a", "asset-a", MediaEnvelope.Role.ORIGINAL)
    private val original = ByteArray(150_003) { ((it % 251) + 1).toByte() }

    @Test fun streamingRoundTripPreservesBytesAndIndependentNonce() {
        val packed = MediaEnvelope.encrypt(original, key, binding)
        val next = MediaEnvelope.encrypt(original, key, binding)
        assertEquals(original.size + MediaEnvelope.OVERHEAD_BYTES, packed.size)
        assertArrayEquals(original, MediaEnvelope.decrypt(packed, key, binding, original.size.toLong()))
        assertFalse(packed.copyOfRange(7, 19).contentEquals(next.copyOfRange(7, 19)))
        assertEquals("UFMI", packed.copyOfRange(0, 4).toString(Charsets.UTF_8))
        assertEquals(1, packed[4].toInt())
        assertEquals(1, packed[5].toInt())
        assertEquals(12, packed[6].toInt())
    }

    @Test fun everyAssociationAndRoleIsAuthenticated() {
        val packed = MediaEnvelope.encrypt(original, key, binding)
        listOf(binding.copy(vaultId = "vault-b"), binding.copy(memoryId = "memory-b"),
            binding.copy(assetId = "asset-b"), binding.copy(role = MediaEnvelope.Role.PREVIEW)).forEach { wrong ->
            assertThrows(Exception::class.java) { MediaEnvelope.decrypt(packed, key, wrong, original.size.toLong()) }
        }
        val wrongKey = SecretKeySpec(ByteArray(32) { (it + 20).toByte() }, "AES")
        assertThrows(Exception::class.java) { MediaEnvelope.decrypt(packed, wrongKey, binding, original.size.toLong()) }
    }

    @Test fun changedHeaderNoncePayloadTagAndTruncationNeverReturnPlaintext() {
        val packed = MediaEnvelope.encrypt(original, key, binding)
        listOf(0, 4, 5, 6, 7, 20, packed.lastIndex).forEach { position ->
            val changed = packed.clone().also { it[position] = (it[position].toInt() xor 1).toByte() }
            assertThrows(Exception::class.java) { MediaEnvelope.decrypt(changed, key, binding, original.size.toLong()) }
        }
        listOf(0, 8, 19, packed.size - 1).forEach { length ->
            assertThrows(Exception::class.java) { MediaEnvelope.decrypt(packed.copyOf(length), key, binding, original.size.toLong()) }
        }
    }

    @Test fun sizeGuardChecksStreamsNotOnlyMetadata() {
        assertThrows(IllegalArgumentException::class.java) {
            MediaEnvelope.encrypt(ByteArrayInputStream(original), ByteArrayOutputStream(), key, binding, 100)
        }
        val packed = MediaEnvelope.encrypt(original, key, binding)
        assertThrows(IllegalArgumentException::class.java) {
            MediaEnvelope.decrypt(ByteArrayInputStream(packed), key, binding, 100)
        }
    }

    @Test fun lengthPrefixesDisambiguateTupleBoundariesAndHashIsCipherOnly() {
        val left = MediaEnvelope.aad(binding.copy(vaultId = "a", memoryId = "bc"))
        val right = MediaEnvelope.aad(binding.copy(vaultId = "ab", memoryId = "c"))
        assertFalse(left.contentEquals(right))
        val first = MediaEnvelope.encrypt(original, key, binding)
        val second = MediaEnvelope.encrypt(original, key, binding)
        val hash = MediaEnvelope.cipherHash(ByteArrayInputStream(first))
        assertEquals(64, hash.length)
        assertEquals(hash, MediaEnvelope.cipherHash(ByteArrayInputStream(first)))
        assertFalse(hash == MediaEnvelope.cipherHash(ByteArrayInputStream(second)))
    }
}
