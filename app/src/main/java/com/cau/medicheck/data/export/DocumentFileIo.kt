package com.cau.medicheck.data.export

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 사용자가 파일 저장·선택 화면에서 고른 파일에 글자를 쓰고 읽는다. (S43 CSV 내보내기, S28 백업·복원)
 *
 * 저장 권한은 필요 없다. 안드로이드의 파일 저장·선택 화면(SAF)이 "사용자가 고른 그 파일 하나"에만
 * 쓰거나 읽을 수 있는 주소(Uri)를 준다.
 */
class DocumentFileIo(private val context: Context) {

    /**
     * @param uri 사용자가 고른 파일의 주소
     * @return 다 썼으면 true, 파일을 열지 못했거나 쓰다가 오류가 나면 false
     */
    suspend fun write(uri: Uri, text: String): Boolean =
        // 파일 입출력은 오래 걸릴 수 있어 화면을 그리는 흐름이 아닌 입출력용 작업 공간(Dispatchers.IO)에서 한다.
        withContext(Dispatchers.IO) {
            val ok = runCatching {
                // "wt" = 쓰기 + 원래 있던 내용 지우기
                val stream = context.contentResolver.openOutputStream(uri, "wt") ?: return@runCatching false
                stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                true
            }.getOrDefault(false)
            if (!ok) discard(uri)
            ok
        }

    /**
     * 사용자가 고른 파일을 UTF-8 글자로 읽는다. (S28 백업 불러오기)
     *
     * @param maxBytes 이보다 큰 파일은 읽지 않는다 (엉뚱한 큰 파일을 통째로 메모리에 올리지 않게)
     * @return 읽은 글자. 파일을 열 수 없거나 너무 크면 null
     */
    suspend fun readText(uri: Uri, maxBytes: Int): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val stream = context.contentResolver.openInputStream(uri) ?: return@runCatching null
                stream.use { input ->
                    // 상한보다 1바이트 더 읽어 본다. 그만큼 읽히면 상한을 넘는 파일이다.
                    val bytes = input.readNBytesCompat(maxBytes + 1)
                    if (bytes.size > maxBytes) null else String(bytes, Charsets.UTF_8)
                }
            }.getOrNull()
        }

    /** 최대 limit바이트까지 읽는다. (InputStream.readNBytes는 Android 13부터라 직접 구현했다) */
    private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (out.size() < limit) {
            val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
            if (read < 0) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    /**
     * 저장 화면이 미리 만들어 둔 빈(또는 쓰다 만) 파일을 지운다.
     * 쓰기에 실패했거나 내보내기를 준비하다 오류가 났을 때 부른다 — 반쪽짜리 파일이 제대로 된 내보내기처럼 남지 않게.
     * 지우지 못해도 그냥 넘어간다.
     */
    suspend fun discard(uri: Uri) {
        withContext(Dispatchers.IO) {
            runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
        }
    }
}
