package com.diskree.emotes2discord

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.diskree.emotes2discord.databinding.ActivityMainBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.urlInputBar.addTextChangedListener {
            if (is7TVEmoteLink(binding.urlInputBar.text.toString())) {
                binding.processButton.setImageResource(R.drawable.ic_done)
            } else {
                binding.processButton.setImageResource(R.drawable.ic_copy)
            }
        }

        binding.processButton.setOnClickListener {
            if (is7TVEmoteLink(binding.urlInputBar.text.toString())) {
                startConversion()
            } else {
                val clipboardManager =
                    getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = clipboardManager.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val clipData = clip.getItemAt(0)
                    if (clipData != null && !TextUtils.isEmpty(clipData.text)) {
                        binding.urlInputBar.setText(clipData.text)
                        binding.urlInputBar.setSelection(clipData.text.length)
                    }
                }
            }
        }
    }

    private fun startConversion() {
        val id = binding.urlInputBar.text.toString().split("emotes/")[1]
        val baseUrl = "https://cdn.7tv.app/emote/$id/4x.webp"
        val client = OkHttpClient()
        val request: Request = Request.Builder().url(baseUrl).build()
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: java.io.IOException) {
            }

            override fun onResponse(call: Call, response: Response) {
                val fos = FileOutputStream(File(Environment.getDownloadCacheDirectory(), "emote.webp"))
                fos.write(response.body!!.bytes())
                fos.close()
            }
        })

    }

    private fun is7TVEmoteLink(text: String): Boolean {
        return text.startsWith("http://7tv.app/emotes/") || text.startsWith("https://7tv.app/emotes/")
    }
}