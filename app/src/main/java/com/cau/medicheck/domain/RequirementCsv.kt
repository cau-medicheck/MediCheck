package com.cau.medicheck.domain

/**
 * 107개 기능 자산(CSV 글자)을 Requirement 목록으로 바꾼다.
 * CSV: 칸을 쉼표로 나눈 표 모양의 글자 파일. 안드로이드와 상관없는 순수 코드라 JUnit으로 시험한다.
 *
 * 자산 파일은 팀의 기능 목록 CSV 원본에, 프로토타입이 쓰던 화면 목록을 'v3화면ID' 칸으로 하나 더 붙인 것이다.
 * 앱은 열세 칸 중 다섯 칸(ID·분류·기능·MoSCoW·v3화면ID)만 읽는다.
 */
object RequirementCsv {

    /**
     * @throws IllegalArgumentException 빈 파일이거나, 필요한 칸이 없거나, 모르는 우선순위 글자가 있을 때
     */
    fun parse(text: String): List<Requirement> {
        // 맨 앞의 BOM(인코딩 표시, 눈에 안 보이는 글자)을 떼고, 줄 끝이 CRLF든 LF든 줄로 나눈다.
        val lines = text.removePrefix("﻿").lines().filter { it.isNotBlank() }
        require(lines.isNotEmpty()) { "빈 파일" }

        // 칸 위치는 머리줄의 이름으로 찾는다 (칸 순서가 바뀌어도 읽히게).
        val header = splitLine(lines.first())
        fun column(name: String): Int = header.indexOf(name).also { require(it >= 0) { "칸 없음: $name" } }
        val idColumn = column("ID")
        val scopeColumn = column("분류")
        val titleColumn = column("기능")
        val moscowColumn = column("MoSCoW")
        val screensColumn = column("v3화면ID")

        return lines.drop(1).map { line ->
            val cells = splitLine(line)
            require(cells.size == header.size) { "칸 수가 맞지 않는 줄: ${cells.firstOrNull()}" }
            Requirement(
                id = cells[idColumn].trim(),
                scope = cells[scopeColumn].trim(),
                title = cells[titleColumn].trim(),
                moscow = requireNotNull(Moscow.fromCsv(cells[moscowColumn])) { "모르는 우선순위: ${cells[moscowColumn]}" },
                screens = cells[screensColumn].split(",").map { it.trim() }.filter { it.isNotEmpty() }
            )
        }
    }

    /**
     * 한 줄을 칸으로 나눈다. 큰따옴표로 감싼 칸 안의 쉼표는 나누지 않고, "" 는 " 하나로 읽는다.
     * 칸 안의 줄바꿈은 다루지 않는다 (지금 자산에는 없다).
     * @throws IllegalArgumentException 따옴표가 닫히지 않았을 때
     */
    fun splitLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                // 따옴표 안에서 "" 를 만나면 따옴표 글자 하나로 읽고 한 칸 더 건너뛴다.
                inQuotes && ch == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    cell.append('"')
                    i++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    cells.add(cell.toString())
                    cell.clear()
                }
                else -> cell.append(ch)
            }
            i++
        }
        // 따옴표를 열고 닫지 않은 줄은 깨진 줄이다. 그대로 받으면 엉뚱한 칸이 화면에 나오므로 여기서 멈춘다.
        require(!inQuotes) { "따옴표가 닫히지 않은 줄" }
        cells.add(cell.toString())
        return cells
    }
}
