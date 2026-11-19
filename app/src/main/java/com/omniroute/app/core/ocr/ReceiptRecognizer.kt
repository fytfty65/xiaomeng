package com.omniroute.app.core.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * 小票文字识别。使用 ML Kit 中文 bundled 模型：模型随 APK 分发，
 * 不依赖 Google Play 服务，页面不上传图片（只有识别出的文本会发给网关解析金额）。
 */
object ReceiptRecognizer {

    /** 小票字号小，分辨率要比头像高一些，同时避免超大图拖慢识别。 */
    private const val MAX_EDGE = 2048

    private val recognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    /** 识别失败或图片不可读时返回 null，调用方回退到手动记账。 */
    suspend fun recognize(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        val bitmap = decodeBitmap(context, uri) ?: return@withContext null
        try {
            val text = suspendCancellableCoroutine<String?> { continuation ->
                recognizer.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener { result -> continuation.resume(result.text) }
                    .addOnFailureListener { continuation.resume(null) }
            }
            text?.takeIf { it.isNotBlank() }
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeBitmap(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / sample > MAX_EDGE || bounds.outHeight / sample > MAX_EDGE) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }
}
