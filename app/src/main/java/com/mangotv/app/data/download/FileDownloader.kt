package com.mangotv.app.data.download

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment

/**
 * Saves a file with Android's own download manager (it keeps going with the app closed, shows a notification and puts the file in Downloads).
 * Nothing goes through Arc TV's servers: the phone fetches the link straight from the debrid service.
 */
object FileDownloader {
    /** Starts the download and returns its id. Throws if the system refuses the link. */
    fun enqueue(context: Context, download: DownloadInfo): Long {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(download.url))
            .setTitle(download.filename)
            .setDescription("Arc TV")
            .setMimeType(mimeTypeFor(download.filename))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        // The shared Downloads folder needs no permission from Android 10; before that it needs one the app only has if it was granted, and
        // without it the file goes to the app's own folder instead.
        if (canWritePublicDownloads(context)) {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Arc TV/${download.filename}")
        } else {
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, download.filename)
        }
        return manager.enqueue(request)
    }

    private fun canWritePublicDownloads(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    internal fun mimeTypeFor(filename: String): String = when (filename.substringAfterLast('.', "").lowercase()) {
        "mkv" -> "video/x-matroska"
        "webm" -> "video/webm"
        "avi" -> "video/x-msvideo"
        "mov" -> "video/quicktime"
        "ts" -> "video/mp2t"
        else -> "video/mp4"
    }
}
