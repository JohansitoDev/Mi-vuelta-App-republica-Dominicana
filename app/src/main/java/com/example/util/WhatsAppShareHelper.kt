package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.example.data.model.Vuelta
import com.example.data.model.VueltaDateUtils

object WhatsAppShareHelper {
  fun shareVuelta(
    context: Context,
    vuelta: Vuelta,
    receiptUri: Uri?,
  ) {
    val statusLine = if (vuelta.completed) "ya está lista ✅" else "vence el *${VueltaDateUtils.formatFullDate(vuelta.dueDateMillis)}*"
    val noteLine = if (vuelta.notes.isNotBlank()) "\nNota: ${vuelta.notes}" else ""
    val message = buildString {
      append("Oye, recuerda esta vuelta: *${vuelta.title}* $statusLine.")
      append(noteLine)
      append("\n¡Que no nos cobren mora! — Enviado desde IAEstudio")
    }
    launchWhatsAppOrChooser(context, message, receiptUri)
  }

  fun shareFamilyInvite(context: Context, familyName: String, inviteCode: String) {
    val message = "Únete a nuestro grupo \"$familyName\" en IAEstudio para compartir pagos y diligencias. Usa el código: *$inviteCode*"
    launchWhatsAppOrChooser(context, message, null)
  }

  private fun launchWhatsAppOrChooser(context: Context, message: String, imageUri: Uri?) {
    val baseIntent = Intent(Intent.ACTION_SEND).apply {
      putExtra(Intent.EXTRA_TEXT, message)
      if (imageUri != null) {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      } else {
        type = "text/plain"
      }
    }

    val whatsAppPackage = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
      isPackageInstalled(context, pkg)
    }

    if (whatsAppPackage != null) {
      val waIntent = Intent(baseIntent).apply {
        setPackage(whatsAppPackage)
      }
      try {
        context.startActivity(waIntent)
        return
      } catch (_: Exception) {
        // Fallback to system share chooser
      }
    }

    val chooser = Intent.createChooser(baseIntent, "Compartir vuelta").apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
  }

  private fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
      context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
      true
    } catch (_: Exception) {
      try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(packageName, 0)
        true
      } catch (_: Exception) {
        false
      }
    }
  }
}
