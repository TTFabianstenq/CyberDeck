package com.cyberdeck.android.data

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest

object CryptoTools {
    fun hashText(algo: String, text: String): String {
        val md = MessageDigest.getInstance(algo)
        return md.digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    fun hashStream(algo: String, input: InputStream): String {
        val md = MessageDigest.getInstance(algo)
        val buf = ByteArray(16 * 1024)
        while (true) {
            val n = input.read(buf)
            if (n <= 0) break
            md.update(buf, 0, n)
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    fun hashUri(context: Context, algo: String, uri: Uri): String {
        context.contentResolver.openInputStream(uri)?.use { return hashStream(algo, it) }
            ?: return "unable to open file"
    }

    fun encode(mode: String, text: String): String {
        val bytes = text.toByteArray(Charsets.UTF_8)
        return when (mode) {
            "base64" -> android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            "url" -> URLEncoder.encode(text, "UTF-8")
            "hex" -> bytes.joinToString("") { "%02x".format(it) }
            "binary" -> bytes.joinToString(" ") { it.toUByte().toString(2).padStart(8, '0') }
            "utf8" -> bytes.joinToString(" ") { it.toInt().and(0xFF).toString() }
            else -> "unknown mode"
        }
    }

    fun decode(mode: String, text: String): String {
        return try {
            when (mode) {
                "base64" -> String(android.util.Base64.decode(text.trim(), android.util.Base64.DEFAULT), Charsets.UTF_8)
                "url" -> URLDecoder.decode(text, "UTF-8")
                "hex" -> {
                    val clean = text.replace(Regex("\\s+"), "")
                    require(clean.length % 2 == 0) { "hex length must be even" }
                    val out = ByteArray(clean.length / 2)
                    for (i in out.indices) out[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
                    String(out, Charsets.UTF_8)
                }
                "binary" -> String(text.trim().split(Regex("\\s+")).map { it.toInt(2).toByte() }.toByteArray(), Charsets.UTF_8)
                "utf8" -> String(text.trim().split(Regex("\\s+")).map { it.toInt().toByte() }.toByteArray(), Charsets.UTF_8)
                else -> "unknown mode"
            }
        } catch (e: Exception) {
            "decode error: ${e.message}"
        }
    }
}
