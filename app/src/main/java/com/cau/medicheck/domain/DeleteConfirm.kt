package com.cau.medicheck.domain

/**
 * 계정·데이터 삭제(S44)의 확인 글자 검사.
 * 실수로 지우지 않도록, 확인 칸에 정해진 낱말을 정확히 입력해야만 삭제한다.
 */
object DeleteConfirm {
    /** 입력해야 하는 낱말 (화면의 자리표시 글자와 같다) */
    const val WORD = "삭제"

    /** 앞뒤 공백만 봐주고, 그 밖에는 한 글자도 다르면 안 된다. ("삭 제", "삭제!", "delete"는 틀림) */
    fun matches(input: String): Boolean = input.trim() == WORD
}
