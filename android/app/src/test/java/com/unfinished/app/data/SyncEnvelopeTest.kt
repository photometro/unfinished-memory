package com.unfinished.app.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Base64

class SyncEnvelopeTest {
    @Test
    fun recoveryEnvelopeRoundTripsAcrossIndependentCalls() {
        val contentKey = ByteArray(32) { (it * 3).toByte() }
        val recoverySecret = ByteArray(32) { (255 - it).toByte() }
        val envelope = ContentKeyEnvelopeCrypto.create(contentKey, recoverySecret, "vault-a", "key-1")

        assertArrayEquals(contentKey, ContentKeyEnvelopeCrypto.open(envelope, recoverySecret, "vault-a"))
    }

    @Test
    fun wrongSecretVaultOrTamperedEnvelopeNeverReturnsAKey() {
        val contentKey = ByteArray(32) { it.toByte() }
        val secret = ByteArray(32) { (it + 17).toByte() }
        val wrongSecret = secret.clone().also { it[0] = (it[0].toInt() xor 0x5a).toByte() }
        val envelope = ContentKeyEnvelopeCrypto.create(contentKey, secret, "vault-a", "key-1")
        val packed = Base64.getUrlDecoder().decode(envelope)
        packed[8] = (packed[8].toInt() xor 0x01).toByte()
        val tampered = Base64.getUrlEncoder().withoutPadding().encodeToString(packed)

        assertThrows(Exception::class.java) { ContentKeyEnvelopeCrypto.open(envelope, wrongSecret, "vault-a") }
        assertThrows(IllegalArgumentException::class.java) { ContentKeyEnvelopeCrypto.open(envelope, secret, "vault-b") }
        assertThrows(Exception::class.java) { ContentKeyEnvelopeCrypto.open(tampered, secret, "vault-a") }
    }
}
