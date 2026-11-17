package com.cau.medicheck.data.export

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 사용자가 파일 저장 화면에서 고른 파일에 글자를 쓴다. (S43 CSV 내보내기)
 *
 * 저장 권한은 필요 없다. 안드로이드의 파일 저장 화면(SAF)이 "사용자가 고른 그 파일 하나"에만 쓸 수 있는
 * 주소(Uri)를 준다.
 */
class CsvFileWriter(private val context: Context) {

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
