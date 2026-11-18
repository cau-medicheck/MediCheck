package com.cau.medicheck.domain

/**
 * MoSCoW 우선순위: 기능을 "꼭(Must) / 되도록(Should) / 여유 되면(Could) / 이번에는 안 함(Won’t)"으로 나눈 것.
 * @param csvText 자산 파일의 'MoSCoW' 칸에 적힌 글자 그대로 (Won’t의 ’는 둥근 따옴표 U+2019)
 */
enum class Moscow(val csvText: String) {
    MUST("Must"), SHOULD("Should"), COULD("Could"), WONT("Won’t");

    companion object {
        /** 자산 글자 → 우선순위. 곧은 따옴표로 적힌 "Won't"도 받아 준다. 모르는 글자면 null */
        fun fromCsv(text: String): Moscow? {
            val trimmed = text.trim()
            return entries.firstOrNull { it.csvText == trimmed } ?: if (trimmed == "Won't") WONT else null
        }
    }
}

/**
 * 107개 기능(요구사항) 중 하나. S45 요구사항 맵과 S46 안전·개발 범위가 보여 준다.
 *
 * @param id 기능 번호. 예 "F001"
 * @param scope 분류. 예 "계정"
 * @param title 기능 이름. 예 "회원가입"
 * @param screens 이 기능을 볼 수 있는 화면 ID들. 예 ["S07", "S10", "S12", "S32"]
 */
data class Requirement(
    val id: String,
    val scope: String,
    val title: String,
    val moscow: Moscow,
    val screens: List<String>
)

/** 기능 목록을 거르고 세는 계산. 순수 함수라 폰 없이 단위 테스트한다. */
object Requirements {

    /**
     * S45의 검색과 우선순위 고르기. (프로토타입 reqMap과 같은 규칙)
     * "번호 이름 분류"를 공백으로 이어 소문자로 바꾼 글자에, 소문자로 바꾼 검색어가 들어 있으면 남긴다.
     * 검색어의 앞뒤 공백은 지우지 않는다. 화면 ID로는 찾지 않는다.
     *
     * @param tier 고른 우선순위. null이면 "전체"
     */
    fun filter(all: List<Requirement>, query: String, tier: Moscow?): List<Requirement> {
        val needle = query.lowercase()
        return all.filter { requirement ->
            (tier == null || requirement.moscow == tier) &&
                "${requirement.id} ${requirement.title} ${requirement.scope}".lowercase().contains(needle)
        }
    }

    /** 우선순위별 개수. 검색과 상관없이 전체 목록 기준이다. (S45 위쪽의 개수 상자) */
    fun countByMoscow(all: List<Requirement>): Map<Moscow, Int> =
        Moscow.entries.associateWith { moscow -> all.count { it.moscow == moscow } }
}
