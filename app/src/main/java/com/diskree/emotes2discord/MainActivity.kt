package com.diskree.emotes2discord

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.diskree.emotes2discord.databinding.ActivityMainBinding
import com.google.android.material.slider.Slider
import com.google.android.material.slider.Slider.OnSliderTouchListener
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import okio.IOException
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.*

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val handler: Handler by lazy { Handler(mainLooper) }
    private var emoteId: String? = null
    private var originalFile: File? = null
    private var isGif = false
    private var optimizedFile: File? = null
    private var lossyLevel = 0
    private var colorsLimit = 0
    private var scaleFactor = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.inputBar.addTextChangedListener {
            if (binding.inputBar.text.isNotEmpty()) {
                binding.searchButton.isInvisible = false
                binding.clipboardButton.isInvisible = true
            } else {
                binding.searchButton.isInvisible = true
                binding.clipboardButton.isInvisible = false
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
        binding.lossyLevelSeekBar.addOnSliderTouchListener(object : OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
                if (lossyLevel == slider.value.toInt()) {
                    return
                }
                lossyLevel = slider.value.toInt()
                optimizeGif()
            }
        })
        binding.colorsLimitSeekBar.addOnSliderTouchListener(object : OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
                if (colorsLimit == slider.value.toInt()) {
                    return
                }
                colorsLimit = slider.value.toInt()
                optimizeGif()
            }
        })
        binding.scaleFactorSeekBar.addOnSliderTouchListener(object : OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }

            override fun onStopTrackingTouch(slider: Slider) {
                if (scaleFactor == slider.value) {
                    return
                }
                scaleFactor = slider.value
                optimizeGif()
            }
        })
        binding.saveButton.setOnClickListener {
            saveToGallery(false)
        }
    }

    private fun loadPreview(url: String, asGif: Boolean) {
        showLoading()
        emoteId = url.split("emotes/")[1]
        val fileExtension = if (asGif) GIF_EXT else PNG_EXT
        OkHttpClient().newCall(Request.Builder().url("https://cdn.7tv.app/emote/$emoteId/4x$fileExtension").build()).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                showError(R.string.error_loading_internet)
            }

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
                isGif = asGif
                originalFile = File(externalCacheDir, emoteId + fileExtension)
                resetOptimization()
                val stream = FileOutputStream(originalFile)
                stream.write(response.body.bytes())
                stream.close()
                handler.post {
                    showPreview()
                }
            }
        })
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun optimizeGif() {
        val file = originalFile ?: return
        optimizedFile = File(externalCacheDir, "$emoteId-optimized$GIF_EXT")
        showLoading()
        GlobalScope.launch(Dispatchers.IO) {
            runCommand("${file.path} --output=${optimizedFile!!.path} --lossy=$lossyLevel --colors=$colorsLimit --scale=$scaleFactor".split(" ").toTypedArray())
            handler.post {
                showPreview()
            }
        }
    }

    private fun resetOptimization() {
        optimizedFile = null
        lossyLevel = 0
        colorsLimit = 256
        scaleFactor = 1.0f
    }

    private fun saveToGallery(force: Boolean) {
        val file = optimizedFile ?: originalFile ?: return
        if (!force && file.length() >= 1024 * 256) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.save_to_gallery_title)
                    .setMessage(R.string.save_to_gallery_description)
                    .setPositiveButton(android.R.string.ok) { _, _ -> saveToGallery(true) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            return
        }
        val dir = File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}/7TV")
        dir.mkdirs()
        file.copyTo(File(dir, "$emoteId.${file.extension}"), true)
        showPlaceholder()
    }

    private fun formatFileSize(size: Long): String = when {
        size < 1024 -> String.format("%d B", size)
        size < 1024 * 1024 -> {
            val value = size / 1024.0f
            String.format("%.1f KB", value)
        }
        else -> {
            val value = size / 1024.0f / 1024.0f
            String.format("%.1f MB", value)
        }
    }

    private fun is7TVEmoteLink(url: String): Boolean = try {
        val uri = URL(url).toURI()
        uri.host == "7tv.app" && uri.path.startsWith("/emotes/")
    } catch (e: Exception) {
        false
    }

    private fun showPlaceholder() {
        binding.placeholderContainer.isVisible = true
        binding.progressBar.isVisible = false
        binding.errorText.isVisible = false
        binding.previewImage.isVisible = false
        binding.searchContainer.isVisible = true
        binding.previewActionsContainer.isVisible = false
        binding.loadingBlockMask.isVisible = false
        binding.loadingBlockContainer.alpha = 1f
    }

    private fun showError(resId: Int) {
        binding.placeholderContainer.isVisible = false
        binding.progressBar.isVisible = false
        binding.errorText.isVisible = true
        binding.errorText.setText(resId)
        binding.previewImage.isVisible = false
        binding.searchContainer.isVisible = true
        binding.previewActionsContainer.isVisible = false
        binding.loadingBlockMask.isVisible = false
        binding.loadingBlockContainer.alpha = 1f
    }

    private fun showLoading() {
        binding.placeholderContainer.isVisible = false
        binding.progressBar.isVisible = true
        binding.errorText.isVisible = false
        binding.previewImage.isVisible = false
        binding.loadingBlockMask.isVisible = true
        binding.loadingBlockContainer.alpha = 0.5f
    }

    private fun showPreview() {
        val file = optimizedFile ?: originalFile ?: return
        binding.placeholderContainer.isVisible = false
        binding.progressBar.isVisible = false
        binding.errorText.isVisible = false
        binding.previewImage.setImageDrawable(null)
        binding.previewImage.isVisible = true
        binding.saveButtonText.text = String.format(getString(R.string.save_to_gallery), formatFileSize(file.length()))
        binding.searchContainer.isVisible = false
        binding.previewActionsContainer.isVisible = true
        binding.optimizationContainer.isVisible = isGif
        binding.loadingBlockMask.isVisible = false
        binding.loadingBlockContainer.alpha = 1f
        binding.lossyLevelSeekBar.value = lossyLevel.toFloat()
        binding.colorsLimitSeekBar.value = colorsLimit.toFloat()
        binding.scaleFactorSeekBar.value = scaleFactor
        binding.lossyLevelValue.text = String.format(getString(R.string.lossy_level_title), lossyLevel)
        binding.colorsLimitValue.text = String.format(getString(R.string.color_limit_title), colorsLimit)
        binding.scaleFactorValue.text = String.format(Locale.ENGLISH, getString(R.string.scale_factor_title), scaleFactor)
        binding.previewImage.scaleX = scaleFactor
        binding.previewImage.scaleY = scaleFactor
        if (isGif) {
            Glide.with(this)
                    .asGif()
                    .load(file.path)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(binding.previewImage)
        } else {
            Glide.with(this)
                    .load(file.path)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(binding.previewImage)
        }
    }

    private external fun runCommand(args: Array<String>): Int

    companion object {
        init {
            System.loadLibrary("gifsicle")
        }

        private const val GIF_EXT = ".gif"
        private const val PNG_EXT = ".png"
    }
}