package com.ekenahmetfaruk.vigna_scan

import android.content.Context
import android.graphics.Bitmap
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DiseaseClassifier(context: Context, modelName: String) {

    private var module: Module? = null

    // 🌟 Colab'daki Eğitimde Kullandığımız Normalizasyon Değerleri! (Çok Kritik)
    private val NORM_MEAN_RGB = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val NORM_STD_RGB = floatArrayOf(0.229f, 0.224f, 0.225f)

    // Colab'daki class_names listesinin aynısı (Alfabetik sırada olmalı)
    private val classes = arrayOf(
        "Cercospora leaf spot",
        "Healthy",
        "Insect",
        "Leaf Crinkle",
        "Yellow Mosaic"
    )

    init {
        // Model dosyasını yükle
        module = LiteModuleLoader.load(assetFilePath(context, modelName))
    }

    /**
     * Kameradan veya Galeriden gelen Bitmap'i alır, işler ve sonucu String olarak döner.
     */
    fun predict(bitmap: Bitmap): String {
        if (module == null) return "Model Yüklenemedi!"

        // 1. Görüntüyü Modelin Beklediği 224x224 Boyutuna Küçült
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

        // 2. Görüntüyü PyTorch Tensörüne Çevir ve Normalize Et
        // (Eklediğin torchvision-lite kütüphanesi sayesinde bu işlemi manuel yapmıyoruz)
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            resizedBitmap,
            NORM_MEAN_RGB,
            NORM_STD_RGB
        )

        // 3. Tensörü Modele Ver (İleri Besleme / Forward Pass)
        val inputs = IValue.from(inputTensor)
        val outputTensor = module?.forward(inputs)?.toTensor()

        // 4. Sonuçları Çözümle (Hangi sınıf en yüksek skoru aldı?)
        val scores = outputTensor?.dataAsFloatArray
        var maxScoreIdx = -1
        var maxScore = -Float.MAX_VALUE

        if (scores != null) {
            for (i in scores.indices) {
                if (scores[i] > maxScore) {
                    maxScore = scores[i]
                    maxScoreIdx = i
                }
            }
        }

        // En yüksek skoru alan sınıfın ismini geri dön
        return if (maxScoreIdx != -1) classes[maxScoreIdx] else "Bilinmiyor"
    }

    /**
     * Assets klasöründeki modeli cihazın dahili hafızasına kopyalayarak PyTorch'un okumasını sağlar.
     */
    @Throws(IOException::class)
    private fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        context.assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }
        }
        return file.absolutePath
    }
}