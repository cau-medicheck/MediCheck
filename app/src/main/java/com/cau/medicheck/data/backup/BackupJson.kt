package com.cau.medicheck.data.backup

import com.cau.medicheck.domain.DoseCondition
import com.cau.medicheck.domain.backup.BackupData
import com.cau.medicheck.domain.backup.BackupDoseRecord
import com.cau.medicheck.domain.backup.BackupMedication
import com.cau.medicheck.domain.backup.BackupSchedule
import com.cau.medicheck.domain.backup.BackupStatusEvent
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * 백업 내용(BackupData) ↔ JSON 글자. (DB_DESIGN 부록 B)
 *
 * JSON: { "이름": 값 } 꼴의 글자 데이터 형식. 안드로이드에 들어 있는 org.json으로 읽고 쓴다.
 * 여기서는 "글자가 맞는 모양인가"만 본다(필요한 칸이 있는지, 숫자 자리에 숫자가 있는지).
 * 내용이 말이 되는지(없는 약을 가리키는 규칙 등)는 BackupValidator가 본다.
 */
object BackupJson {

    /**
     * 백업 내용 → JSON 글자.
     * @param pretty true면 사람이 읽기 쉽게 2칸 들여쓰기, false면 한 줄로 붙여 쓴다(파일이 작아진다)
     */
    fun encode(data: BackupData, pretty: Boolean = true): String = JSONObject().apply {
        put("format", BackupData.FORMAT)
        put("version", data.version)
        putOrNull("exportedAt", data.exportedAt)
        putOrNull("accountEmail", data.accountEmail)
        put("medications", JSONArray(data.medications.map(::medicationToJson)))
        put("schedules", JSONArray(data.schedules.map(::scheduleToJson)))
        put("doseRecords", JSONArray(data.doseRecords.map(::recordToJson)))
        put("statusEvents", JSONArray(data.statusEvents.map(::eventToJson)))
    }.let { if (pretty) it.toString(2) else it.toString() }

    /**
     * JSON 글자 → 백업 내용. 읽을 수 없으면 null.
     * null이 되는 경우: JSON이 아님, 맨 바깥이 { }가 아님, format이 다름, version이 정수가 아님,
     * 약·규칙·기록 목록이 없음, 꼭 있어야 하는 칸이 없거나 종류가 다름.
     * 모르는 칸은 무시한다. statusEvents는 칸이 없을 때만 빈 목록으로 읽는다(있는데 목록이 아니면 null).
     */
    fun decode(text: String): BackupData? = try {
        // 파일 맨 앞에 BOM(글자 인코딩 표시, 눈에 안 보이는 글자)이 있으면 떼고 읽는다.
        val root = JSONObject(text.removePrefix("﻿"))
        if (root.optString("format") != BackupData.FORMAT) {
            null
        } else {
            BackupData(
                version = wholeNumber(root.get("version")),
                exportedAt = root.stringOrNull("exportedAt"),
                accountEmail = root.stringOrNull("accountEmail"),
                medications = root.getJSONArray("medications").objects().map(::medicationFromJson),
                schedules = root.getJSONArray("schedules").objects().map(::scheduleFromJson),
                doseRecords = root.getJSONArray("doseRecords").objects().map(::recordFromJson),
                // 칸이 "없을 때"만 빈 목록이다. 칸은 있는데 목록이 아니면(깨진 파일) 오류로 본다 —
                // 빈 목록으로 읽어 버리면 복원이 지금 이력을 지우고 아무것도 넣지 않은 채 성공해 버린다.
                statusEvents = if (!root.has("statusEvents")) {
                    emptyList()
                } else {
                    root.getJSONArray("statusEvents").objects().map(::eventFromJson)
                }
            )
        }
    } catch (e: JSONException) {
        null
    } catch (e: IllegalArgumentException) {
        null   // 숫자 자리에 소수가 온 경우 등 (wholeNumber·requireString이 던진다)
    }

    // ----- 쓰기 -----

    private fun medicationToJson(m: BackupMedication) = JSONObject().apply {
        put("id", m.id); put("name", m.name); put("dosage", m.dosage)
        put("startDate", m.startDate); putOrNull("endDate", m.endDate); putOrNull("memo", m.memo)
        put("createdAt", m.createdAt); putOrNull("deletedAt", m.deletedAt)
        putOrNull("pausedFrom", m.pausedFrom); putOrNull("hospital", m.hospital)
        putOrNull("department", m.department); putOrNull("sourceMemo", m.sourceMemo)
    }

    private fun scheduleToJson(s: BackupSchedule) = JSONObject().apply {
        put("id", s.id); put("medicationId", s.medicationId); put("time", s.time)
        put("repeatDays", s.repeatDays); putOrNull("effectiveFrom", s.effectiveFrom)
        put("createdAt", s.createdAt); putOrNull("deletedAt", s.deletedAt)
        put("doseCondition", s.doseCondition)
    }

    private fun recordToJson(r: BackupDoseRecord) = JSONObject().apply {
        put("id", r.id); put("scheduleId", r.scheduleId); put("date", r.date)
        put("scheduledTime", r.scheduledTime); put("medicationName", r.medicationName)
        put("dosage", r.dosage); putOrNull("memo", r.memo); put("status", r.status)
        putOrNull("actedAt", r.actedAt); put("createdAt", r.createdAt)
        put("doseCondition", r.doseCondition)
    }

    private fun eventToJson(e: BackupStatusEvent) = JSONObject().apply {
        putOrNull("doseRecordId", e.doseRecordId); putOrNull("scheduleId", e.scheduleId)
        putOrNull("doseDate", e.doseDate); putOrNull("scheduledTime", e.scheduledTime)
        putOrNull("medicationName", e.medicationName)
        put("fromStatus", e.fromStatus); put("toStatus", e.toStatus)
        putOrNull("fromActedAt", e.fromActedAt); putOrNull("toActedAt", e.toActedAt)
        put("changedAt", e.changedAt); put("isUndo", e.isUndo)
    }

    // ----- 읽기 -----

    private fun medicationFromJson(o: JSONObject) = BackupMedication(
        id = o.getLong("id"),
        name = o.requireString("name"),
        dosage = o.requireString("dosage"),
        startDate = o.requireString("startDate"),
        endDate = o.stringOrNull("endDate"),
        memo = o.stringOrNull("memo"),
        createdAt = o.optLong("createdAt", 0L),
        deletedAt = o.longOrNull("deletedAt"),
        pausedFrom = o.stringOrNull("pausedFrom"),
        hospital = o.stringOrNull("hospital"),
        department = o.stringOrNull("department"),
        sourceMemo = o.stringOrNull("sourceMemo")
    )

    private fun scheduleFromJson(o: JSONObject) = BackupSchedule(
        id = o.getLong("id"),
        medicationId = o.getLong("medicationId"),
        time = o.requireString("time"),
        repeatDays = o.requireString("repeatDays"),
        effectiveFrom = o.stringOrNull("effectiveFrom"),
        createdAt = o.optLong("createdAt", 0L),
        deletedAt = o.longOrNull("deletedAt"),
        doseCondition = DoseCondition.parse(o.stringOrNull("doseCondition")).name
    )

    private fun recordFromJson(o: JSONObject) = BackupDoseRecord(
        id = o.getLong("id"),
        scheduleId = o.getLong("scheduleId"),
        date = o.requireString("date"),
        scheduledTime = o.requireString("scheduledTime"),
        medicationName = o.requireString("medicationName"),
        dosage = o.requireString("dosage"),
        memo = o.stringOrNull("memo"),
        status = o.requireString("status"),
        actedAt = o.longOrNull("actedAt"),
        createdAt = o.optLong("createdAt", 0L),
        doseCondition = DoseCondition.parse(o.stringOrNull("doseCondition")).name
    )

    private fun eventFromJson(o: JSONObject) = BackupStatusEvent(
        doseRecordId = o.longOrNull("doseRecordId"),
        scheduleId = o.longOrNull("scheduleId"),
        doseDate = o.stringOrNull("doseDate"),
        scheduledTime = o.stringOrNull("scheduledTime"),
        medicationName = o.stringOrNull("medicationName"),
        fromStatus = o.requireString("fromStatus"),
        toStatus = o.requireString("toStatus"),
        fromActedAt = o.longOrNull("fromActedAt"),
        toActedAt = o.longOrNull("toActedAt"),
        changedAt = o.getLong("changedAt"),
        isUndo = o.optBoolean("isUndo", false)
    )

    // ----- 작은 도우미 -----
    // org.json은 "값이 없음(null)"을 그냥 null이 아니라 JSONObject.NULL이라는 특별한 값으로 다룬다.
    // 그래서 넣을 때·읽을 때 따로 처리해 주는 도우미를 둔다.

    private fun JSONObject.putOrNull(name: String, value: Any?) {
        put(name, value ?: JSONObject.NULL)
    }

    /** 칸이 없거나 null이면 null, 글자면 그 글자. 글자가 아닌 값(숫자 등)이면 오류 */
    private fun JSONObject.stringOrNull(name: String): String? =
        if (isNull(name)) null else requireString(name)

    private fun JSONObject.longOrNull(name: String): Long? = if (isNull(name)) null else getLong(name)

    /** 꼭 있어야 하는 글자 칸. 없거나 글자가 아니면 오류 (숫자를 슬쩍 글자로 바꿔 읽지 않는다) */
    private fun JSONObject.requireString(name: String): String =
        get(name) as? String ?: throw IllegalArgumentException("$name 칸이 글자가 아닙니다")

    /** 목록의 원소를 { } 하나씩으로 꺼낸다. 원소가 { }가 아니면 오류 */
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    /** 정수면 그 값, 소수(1.5)나 숫자가 아닌 값이면 오류 */
    private fun wholeNumber(value: Any): Int {
        val number = value as? Number ?: throw IllegalArgumentException("숫자가 아닙니다")
        require(number.toDouble() == number.toInt().toDouble()) { "정수가 아닙니다" }
        return number.toInt()
    }
}
