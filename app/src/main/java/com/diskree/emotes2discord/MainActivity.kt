package com.diskree.emotes2discord

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.diskree.emotes2discord.databinding.ActivityMainBinding
import okhttp3.*
import okio.IOException
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val handler: Handler by lazy { Handler(mainLooper) }
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private var currentFile: File? = null
    private var currentEmoteId: String? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.inputBar.addTextChangedListener {
            if (binding.inputBar.text.isNotEmpty()) {
                binding.searchButton.isVisible = true
                binding.clipboardButton.isVisible = false
            } else {
                binding.searchButton.isVisible = false
                binding.clipboardButton.isVisible = true
            }
        }
        binding.siteButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://7tv.app/emotes")))
        }
        binding.clipboardButton.setOnClickListener {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val clipData = clip.getItemAt(0)
                if (clipData != null && !TextUtils.isEmpty(clipData.text)) {
                    if (is7TVEmoteLink(clipData.text.toString())) {
                        loadPreview(clipData.text.toString(), true)
                    } else {
                        showError(R.string.error_loading_url)
                    }
                }
            }
        }
        binding.searchButton.setOnClickListener {
            if (is7TVEmoteLink(binding.inputBar.text.toString())) {
                loadPreview(binding.inputBar.text.toString(), true)
                return@setOnClickListener
            }

        }
        binding.closeButton.setOnClickListener {
            showPlaceholder()
            binding.inputBar.setText("")
        }
        binding.optimizeButton.setOnClickListener {

        }
        binding.saveButton.setOnClickListener {
            val file = currentFile ?: return@setOnClickListener
            val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString() + "/" + "7TV")
            } else {
                File(Environment.getExternalStorageDirectory().toString() + "/" + "7TV")
            }
            if (!dir.exists()) {
                dir.mkdirs()
            }
            file.copyTo(File(dir, "$currentEmoteId.${file.extension}"), true)
            val uri = Uri.fromFile(file);
            try {
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.data = uri
                sendBroadcast(mediaScanIntent)
            } catch (_: Exception) {
            }
        }
    }

    private fun loadPreview(url: String, asGif: Boolean) {
        showLoading()
        currentEmoteId = url.split("emotes/")[1]
        val ext = if (asGif) "gif" else "png"
        val baseUrl = "https://cdn.7tv.app/emote/$currentEmoteId/4x.$ext"
        val client = OkHttpClient()
        val request: Request = Request.Builder().url(baseUrl).build()
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                showError(R.string.error_loading_internet)
            }

            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call, response: Response) {
                if (response.code == 404) {
                    handler.post {
                        if (asGif) {
                            loadPreview(url, false)
                        } else {
                            showError(R.string.error_loading_url)
                        }
                    }
                    return
                }

                currentFile = File(externalCacheDir, "$currentEmoteId.$ext")
                val stream = FileOutputStream(currentFile)
                stream.write(response.body.bytes())
                stream.close()
                handler.post {
                    showPreview()
                }
            }
        })
    }

    private fun showPlaceholder() {
        binding.placeholderContainer.isVisible = true
        binding.progressBar.isVisible = false
        binding.errorText.isVisible = false
        binding.previewImage.isVisible = false
        binding.searchContainer.isVisible = true
        binding.previewActionsContainer.isVisible = false
    }

    private fun showError(resId: Int) {
        binding.placeholderContainer.isVisible = false
        binding.progressBar.isVisible = false
        binding.errorText.isVisible = true
        binding.errorText.setText(resId)
        binding.previewImage.isVisible = false
        binding.searchContainer.isVisible = true
        binding.previewActionsContainer.isVisible = false
    }

    private fun showLoading() {
        binding.placeholderContainer.isVisible = false
        binding.progressBar.isVisible = true
        binding.errorText.isVisible = false
        binding.previewImage.isVisible = false
    }

    @SuppressLint("SetTextI18n")
    private fun showPreview() {
        val file = currentFile ?: return
        binding.placeholderContainer.isVisible = false
        binding.progressBar.isVisible = false
        binding.errorText.isVisible = false
        binding.previewImage.setImageDrawable(null)
        binding.previewImage.isVisible = true
        binding.saveButtonText.text = getString(R.string.save_to_gallery) + "\n(" + formatFileSize(file.length(), false) + ")"
        binding.searchContainer.isVisible = false
        binding.previewActionsContainer.isVisible = true
        if (file.path.endsWith(".gif")) {
            Glide.with(this)
                    .asGif()
                    .load(file.path)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(binding.previewImage)
        } else {
            Glide.with(this)
                    .load(file.path)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(binding.previewImage)
        }
    }

    private fun formatFileSize(size: Long, removeZero: Boolean): String = when {
        size < 1024 -> String.format("%d B", size)
        size < 1024 * 1024 -> {
            val value = size / 1024.0f
            if (removeZero && (value - value.toInt()) * 10 == 0f) {
                String.format("%d KB", value.toInt())
            } else {
                String.format("%.1f KB", value)
            }
        }
        else -> {
            val value = size / 1024.0f / 1024.0f
            if (removeZero && (value - value.toInt()) * 10 == 0f) {
                String.format("%d MB", value.toInt())
            } else {
                String.format("%.1f MB", value)
            }
        }
    }

    private fun is7TVEmoteLink(url: String): Boolean = try {
        val uri = URL(url).toURI()
        uri.host == "7tv.app" && uri.path.startsWith("/emotes/")
    } catch (e: Exception) {
        false
    }
}