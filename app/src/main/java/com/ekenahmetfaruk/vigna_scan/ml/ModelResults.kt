package com.ekenahmetfaruk.vigna_scan.ml

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ModelResult(
    val modelName: String,
    val predictedClass: String,
    val confidence: Float,
    val inferenceTimeMs: Long,
    val isReliable: Boolean
) : Parcelable