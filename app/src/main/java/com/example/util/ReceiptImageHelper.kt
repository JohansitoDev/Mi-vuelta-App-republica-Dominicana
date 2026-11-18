package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ReceiptImageHelper {
  private const val MAX_DIMENSION_PX = 760
  private const val MAX_BASE64_LENGTH = 230_000

  fun createTempCameraUri(context: Context): Uri {
    val receiptsDir = File(context.cacheDir, "receipts").apply { mkdirs() }
    val photoFile = File(receiptsDir, "cam_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      photoFile,
    )
  }

  suspend fun uriToCompressedBase64(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
      val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, boundsOptions)
      }

      var sampleSize = 1
      val maxOrig = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
      while (maxOrig / sampleSize > MAX_DIMENSION_PX * 2) {
        sampleSize *= 2
      }

      val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOptions)
      } ?: return@withContext null

      val rotationDegrees = readExifRotation(context, uri)
      val oriented = if (rotationDegrees != 0f) {
        val matrix = Matrix().apply { postRotate(rotationDegrees) }
        Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
      } else {
        rawBitmap
      }

      val scaled = scaleBitmap(oriented, MAX_DIMENSION_PX)
      var quality = 70
      var encoded = ""
      while (quality >= 30) {
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        if (b64.length <= MAX_BASE64_LENGTH || quality == 30) {
          encoded = b64
          break
        }
        quality -= 15
      }
      encoded.takeIf { it.isNotBlank() && it.length <= 245_000 }
    } catch (_: Exception) {
      null
    }
  }

  suspend fun decodeBase64ToImageBitmap(base64: String?): ImageBitmap? = withContext(Dispatchers.Default) {
    if (base64.isNullOrBlank()) return@withContext null
    try {
      val bytes = Base64.decode(base64, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Exception) {
      null
    }
  }

  suspend fun writeShareableReceiptUri(context: Context, base64: String?, vueltaId: String): Uri? =
    withContext(Dispatchers.IO) {
      if (base64.isNullOrBlank()) return@withContext null
      try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(dir, "share_${vueltaId.ifBlank { "vuelta" }}.jpg")
        FileOutputStream(file).use { it.write(bytes) }
        FileProvider.getUriForFile(
          context,
          "${context.packageName}.fileprovider",
          file,
        )
      } catch (_: Exception) {
        null
      }
    }

  private fun readExifRotation(context: Context, uri: Uri): Float {
    return try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        val exif = ExifInterface(stream)
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
          ExifInterface.ORIENTATION_ROTATE_90 -> 90f
          ExifInterface.ORIENTATION_ROTATE_180 -> 180f
          ExifInterface.ORIENTATION_ROTATE_270 -> 270f
          else -> 0f
        }
      } ?: 0f
    } catch (_: Exception) {
      0f
    }
  }

  private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= maxDimension && height <= maxDimension) return bitmap
    val ratio = minOf(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
    val newWidth = (width * ratio).toInt().coerceAtLeast(1)
    val newHeight = (height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
  }
}
