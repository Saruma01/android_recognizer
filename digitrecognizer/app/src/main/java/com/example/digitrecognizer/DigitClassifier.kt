package com.example.digitrecognizer

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Executors

class DigitClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var inputImageWidth: Int = 28
    private var inputImageHeight: Int = 28
    private var modelInputSize: Int = 0
    private val FLOAT_SIZE = 4
    private val PIXEL_SIZE = 1
    private val OUTPUT_CLASSES = 10

    private val executor = Executors.newSingleThreadExecutor()

    fun initialize(modelPathInAssets: String = "mnist.tflite") {
        val model = loadModelFile(context.assets, modelPathInAssets)
        val options = Interpreter.Options()
        interpreter = Interpreter(model, options)
        // Получаем форму входного тензора, например [1,28,28,1]
        val inputShape = interpreter!!.getInputTensor(0).shape()
        inputImageWidth = inputShape[1]
        inputImageHeight = inputShape[2]
        modelInputSize = FLOAT_SIZE * inputImageWidth * inputImageHeight * PIXEL_SIZE
    }

    // Выполняем классификацию в отдельном потоке, callback возвращает строку результата
    fun classify(bitmap: Bitmap, callback: (String) -> Unit) {
        executor.execute {
            // 1) Resize
            val resized = Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, true)
            // 2) Convert to ByteBuffer
            val byteBuffer = convertBitmapToByteBuffer(resized)
            // 3) Run inference
            val output = Array(1) { FloatArray(OUTPUT_CLASSES) }
            val start = System.currentTimeMillis()
            interpreter?.run(byteBuffer, output)
            val time = System.currentTimeMillis() - start
            // 4) Post-process
            val probs = output[0]
            var maxIdx = 0
            var maxProb = probs[0]
            for (i in 1 until probs.size) {
                if (probs[i] > maxProb) {
                    maxProb = probs[i]; maxIdx = i
                }
            }
            val result = "Prediction: $maxIdx\nConfidence: ${"%.2f".format(maxProb)}\nTime: ${time}ms"
            callback(result)
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until inputImageWidth) {
            for (j in 0 until inputImageHeight) {
                val value = intValues[pixel++]
                val r = (value shr 16) and 0xFF
                val g = (value shr 8) and 0xFF
                val b = value and 0xFF
                val gray = (r + g + b) / 3
                // ВНИМАНИЕ: если вы рисуете чёрным на белом фоне, то надо инвертировать:
                // MNIST ожидает цифру светлой на тёмном фоне (часто), поэтому:
                val normalized = (255 - gray) / 255.0f
                byteBuffer.putFloat(normalized)
            }
        }
        return byteBuffer
    }

    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        val afd = assetManager.openFd(modelPath)
        FileInputStream(afd.fileDescriptor).use { input ->
            val fc = input.channel
            return fc.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
        }
    }

    fun close() {
        executor.execute {
            interpreter?.close()
            interpreter = null
        }
        executor.shutdown()
    }
}