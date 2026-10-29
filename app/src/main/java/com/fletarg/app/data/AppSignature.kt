package com.fletarg.app.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * Huella SHA-1 del certificado con el que está firmada la app. Se manda en los pedidos a Google
 * (cabeceras X-Android-Package / X-Android-Cert) para que la clave pueda restringirse a esta app.
 */
object AppSignature {
    @Suppress("DEPRECATION")
    fun sha1(context: Context): String = try {
        val pm = context.packageManager
        val pkg = context.packageName
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
            info.signingInfo?.apkContentsSigners
        } else {
            pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures
        }
        val first = signatures?.firstOrNull() ?: return ""
        val digest = MessageDigest.getInstance("SHA-1").digest(first.toByteArray())
        digest.joinToString("") { "%02X".format(it) }
    } catch (_: Exception) {
        ""
    }
}
