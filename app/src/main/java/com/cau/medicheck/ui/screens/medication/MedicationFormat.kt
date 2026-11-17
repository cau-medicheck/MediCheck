package com.cau.medicheck.ui.screens.medication

import androidx.annotation.StringRes
import com.cau.medicheck.R
import com.cau.medicheck.domain.MedicationError

/** 오류 종류(MedicationError)에 맞는 토스트 문구를 strings.xml에서 고른다. (문구는 프로토타입 그대로) */
@StringRes
fun MedicationError.messageRes(): Int = when (this) {
    MedicationError.NAME_DOSAGE_TIME_INVALID -> R.string.s08_toast_check_basic
    MedicationError.NO_DAYS -> R.string.s08_toast_check_days
    MedicationError.DATES_INVALID -> R.string.s08_toast_check_dates
    MedicationError.DUPLICATE_TIME -> R.string.s08_toast_duplicate_time
    MedicationError.NOT_FOUND -> R.string.error_med_not_found
}
