package com.ekenahmetfaruk.vigna_scan.ml

import android.content.Context
import android.graphics.Bitmap
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.graphics.scale

@Singleton
class ModelManager @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val MODEL_1_NAME = "mobilenetv3_small_tiny_vit_pso.ptl"
        private const val MODEL_2_NAME = "squeezenet_tiny_vit_android_gwo.ptl"
        private const val CONFIDENCE_THRESHOLD = 0.60f
        private const val INPUT_SIZE = 224

        // ImageNet normalization values
        private val NORM_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val NORM_STD = floatArrayOf(0.229f, 0.224f, 0.225f)

        // Sınıf isimleri — veri setindeki klasör sırasına göre
        val CLASS_NAMES = listOf(
            "Cercospora Yaprak Lekesi",
            "Sağlıklı",
            "Böcek Hasarı",
            "Yaprak Buruşukluğu",
            "Sarı Mozaik"
        )

        val CLASS_NAMES_TR = mapOf(
            "Cercospora Yaprak Lekesi" to "Cercospora Yaprak Lekesi",
            "Sağlıklı" to "Sağlıklı",
            "Böcek Hasarı" to "Böcek Hasarı",
            "Yaprak Buruşukluğu" to "Yaprak Buruşukluğu",
            "Sarı Mozaik" to "Sarı Mozaik"
        )
    }

    private var model1: Module? = null
    private var model2: Module? = null

    fun loadModels() {
        if (model1 == null) {
            model1 = LiteModuleLoader.loadModuleFromAsset(context.assets, MODEL_1_NAME)
        }
        if (model2 == null) {
            model2 = LiteModuleLoader.loadModuleFromAsset(context.assets, MODEL_2_NAME)
        }
    }

    fun runBothModels(bitmap: Bitmap): Pair<ModelResult, ModelResult> {
        // Bitmap'i her zaman ARGB_8888'e çevir
        val safeBitmap = if (bitmap.config == Bitmap.Config.ARGB_8888) {
            bitmap
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        }

        val resizedBitmap = safeBitmap.scale(INPUT_SIZE, INPUT_SIZE)

        val result1 = runModel(
            model = model1!!,
            bitmap = resizedBitmap,
            modelName = "MobileNetV3-Small + Tiny-ViT"
        )
        val result2 = runModel(
            model = model2!!,
            bitmap = resizedBitmap,
            modelName = "SqueezeNet + Tiny-ViT"
        )
        return Pair(result1, result2)
    }

    private fun runModel(
        model: Module,
        bitmap: Bitmap,
        modelName: String
    ): ModelResult {
        val startTime = System.currentTimeMillis()

        // Bitmap → Tensor
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            bitmap,
            NORM_MEAN,
            NORM_STD
        )

        // Model çalıştır
        val outputTensor = model.forward(IValue.from(inputTensor)).toTensor()
        val scores = outputTensor.dataAsFloatArray

        // Softmax uygula
        val softmaxScores = softmax(scores)

        // En yüksek skoru bul
        val maxIndex = softmaxScores.indices.maxByOrNull { softmaxScores[it] } ?: 0
        val maxScore = softmaxScores[maxIndex]

        val inferenceTime = System.currentTimeMillis() - startTime

        val predictedClass = if (maxIndex < CLASS_NAMES.size) {
            CLASS_NAMES[maxIndex]
        } else {
            "Bilinmeyen"
        }

        return ModelResult(
            modelName = modelName,
            predictedClass = predictedClass,
            confidence = maxScore,
            inferenceTimeMs = inferenceTime,
            isReliable = maxScore >= CONFIDENCE_THRESHOLD
        )
    }

    private fun softmax(scores: FloatArray): FloatArray {
        val maxScore = scores.max()
        val expScores = scores.map { Math.exp((it - maxScore).toDouble()).toFloat() }
        val sumExp = expScores.sum()
        return expScores.map { it / sumExp }.toFloatArray()
    }

    fun release() {
        model1?.destroy()
        model2?.destroy()
        model1 = null
        model2 = null
    }
}