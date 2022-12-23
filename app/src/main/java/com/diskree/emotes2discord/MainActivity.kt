package com.diskree.emotes2discord

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.diskree.emotes2discord.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.urlInputBar.addTextChangedListener {
            if (is7TVEmoteLinkInInput(binding.urlInputBar.text.toString())) {
                binding.processButton.setImageResource(R.drawable.ic_done)
            } else {
                binding.processButton.setImageResource(R.drawable.ic_copy)
            }
        }

        binding.processButton.setOnClickListener {
            if (is7TVEmoteLinkInInput(binding.urlInputBar.text.toString())) {
                startConversion()
            } else {
                val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
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

    }

    private fun is7TVEmoteLinkInInput(text: String): Boolean {
        return text.startsWith("http://7tv.app/emotes/") || text.startsWith("https://7tv.app/emotes/")
    }
}