package com.omniroute.app.core.ocr

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * 拍照小票的临时文件管理：只落在应用 cache 下，并保证识别结束后立即删除，
 * 避免长期占用存储或把用户的小票照片留在设备上。
 */
object ReceiptCache {

    private const val DIR = "receipts"

    private fun dir(context: Context): File = File(context.cacheDir, DIR).apply { mkdirs() }

    fun newFile(context: Context): File = File(dir(context), "receipt_${System.currentTimeMillis()}.jpg")

    /** 只处理自家 FileProvider 的 uri，相册选中的 media uri 不会被误删。 */
    fun delete(context: Context, uri: Uri) {
        if (uri.authority != "${context.packageName}.fileprovider") return
        val name = uri.lastPathSegment ?: return
        runCatching { File(dir(context), name).delete() }
    }
}
