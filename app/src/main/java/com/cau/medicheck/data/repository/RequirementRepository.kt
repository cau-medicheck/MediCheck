package com.cau.medicheck.data.repository

import android.content.Context
import com.cau.medicheck.domain.Requirement
import com.cau.medicheck.domain.RequirementCsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 앱에 함께 들어 있는 107개 기능 목록(assets/requirements_107.csv)을 읽어 준다. (S45·S46)
 *
 * 자산(assets): 앱 설치 파일에 그대로 넣어 두는 읽기 전용 파일. DB와 달리 앱이 바꾸지 못한다.
 * 한 번 읽으면 앱이 켜져 있는 동안 메모리에 들고 있어 다시 읽지 않는다.
 */
class RequirementRepository(private val context: Context) {

    // @Volatile: 여러 작업 흐름(스레드)이 함께 봐도 가장 최근에 넣은 값이 보이게 하는 표시
    @Volatile
    private var cache: List<Requirement>? = null

    /**
     * 기능 목록을 돌려준다.
     * 읽지 못하면(파일 없음, 칸 이름 없음 등) 오류를 그대로 던진다 → ViewModel이 "불러오지 못했습니다"로 바꾼다.
     */
    suspend fun load(): List<Requirement> {
        cache?.let { return it }
        // 파일 읽기는 화면을 그리는 흐름이 아닌 입출력용 작업 공간(Dispatchers.IO)에서 한다.
        return withContext(Dispatchers.IO) {
            val text = context.assets.open(FILE_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
            RequirementCsv.parse(text)
        }.also { cache = it }
    }

    companion object {
        const val FILE_NAME = "requirements_107.csv"
    }
}
