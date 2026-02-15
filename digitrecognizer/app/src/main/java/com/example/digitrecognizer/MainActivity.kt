package com.example.digitrecognizer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private lateinit var drawingView: DrawingView
    private lateinit var btnRecognize: Button
    private lateinit var btnClear: Button
    private lateinit var txtResult: TextView
    private lateinit var classifier: DigitClassifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        btnRecognize = findViewById(R.id.btn_recognize)
        btnClear = findViewById(R.id.btn_clear)
        txtResult = findViewById(R.id.txt_result)

        classifier = DigitClassifier(this)
        classifier.initialize("mnist.tflite")

        btnRecognize.setOnClickListener {
            txtResult.text = "Распознавание..."
            val bmp = drawingView.getBitmap()
            classifier.classify(bmp) { result ->
                runOnUiThread {
                    txtResult.text = result
                }
            }
        }

        btnClear.setOnClickListener {
            drawingView.clear()
            txtResult.text = ""
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        classifier.close()
    }
}
