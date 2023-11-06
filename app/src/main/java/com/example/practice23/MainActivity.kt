package com.example.practice23

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {
    private var job: Job = Job()
    private var isRun = false

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private lateinit var textViewSum: TextView
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button: Button = findViewById(R.id.button)

        textViewSum = findViewById(R.id.textView)
        textView = findViewById(R.id.textView2)

        button.setOnClickListener {
            isRun = true
            button.isEnabled = false
            button.setText(R.string.btn_off)
            launch {
                calculate()

                button.post{
                    button.setText(R.string.btn_on)
                    button.isEnabled = true
                    isRun = false
                }
            }
        }
    }

    private fun updateProgressBar(processedBytes: Int, sizeBytes: Int) {
        var loadingProgressBar : ProgressBar = findViewById(R.id.progressBar)
        val progress = processedBytes * 100 / sizeBytes
        loadingProgressBar.progress = progress
    }

    suspend fun calculate() {
//16777216
        val random = Random()
        val byteArraySize = 167772
        val byteArray = ByteArray(byteArraySize)
        random.nextBytes(byteArray)

        var sum = this.async {
            crc16(byteArray).toHex()
        }
        var crcResult = sum.await()

        if(isRun == true){
            runOnUiThread {
                textViewSum.text = "SUM: "
                textView.text = "BYTES: "
            }

        }
        runOnUiThread {
            textViewSum.text = "SUM: $crcResult"
        }

        var text : String = ""
        val stringBuilder = StringBuilder()

        for (i in 0 until byteArraySize){
            stringBuilder.append(byteArray[i])
            stringBuilder.append("; ")

            if (i % 10000 == 0) {
                text = stringBuilder.toString()
                runOnUiThread {
                    textView.append(text)
                }
                stringBuilder.clear()
            }
        }
        text = stringBuilder.toString()
        runOnUiThread {
            textView.append(text)
        }
    }

    suspend fun crc16(byteArray: ByteArray) : ByteArray {
        var crc = 0xffff
        var processedBytes = 0
        val sizeBytes = byteArray.size
            byteArray.forEach {byte ->
                crc = (crc ushr 8 or crc shl 8) and 0xffff
                crc = crc xor (byte.toInt() and 0xff)
                crc = crc xor ((crc and 0xff) shr 4)
                crc = crc xor ((crc shl 12) and 0xffff)
                crc = crc xor (((crc and 0xff) shl 5) and 0xffff)

                processedBytes++
                withContext(Dispatchers.Main){
                    updateProgressBar(processedBytes, sizeBytes)
                }
            }
            crc = crc and 0xffff

        return crc.to2ByteArray()
    }
    fun Int.to2ByteArray() : ByteArray = byteArrayOf(toByte(), shr(8).toByte())
    fun ByteArray.toHex(): String = joinToString("") { eachByte -> "%02x".format(eachByte) }
}