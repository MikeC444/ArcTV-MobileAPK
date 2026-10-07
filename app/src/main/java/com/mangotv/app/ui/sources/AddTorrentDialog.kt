package com.mangotv.app.ui.sources

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mangotv.app.ui.components.MangoButton
import com.mangotv.app.ui.components.MangoButtonStyle
import com.mangotv.app.ui.theme.ArcAccent
import com.mangotv.app.ui.theme.ErrorCoral
import com.mangotv.app.ui.theme.MangoBackgroundElevated
import com.mangotv.app.ui.theme.MangoSurface
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary
import com.mangotv.app.ui.theme.TextTertiary

/**
 * "Add a torrent" on Select a Source, as a sheet that rises from the bottom of the phone: a magnet link (or its info hash) or the address of a
 * .torrent file typed or pasted in (a Paste button reads the clipboard, since a link is almost always copied from a browser), or a .torrent file
 * picked from the phone. The source then appears in the list like any other. [error] is the reason the last attempt was refused; the sheet closes
 * by itself when it worked (the caller closes it).
 */
@Composable
fun AddTorrentDialog(
    error: String?,
    onSubmitText: (String) -> Unit,
    onPickFile: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) onPickFile(uri) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // Tapping the dimmed area above the sheet closes it.
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xB308080A)).clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
                    .background(MangoBackgroundElevated, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 14.dp)
                        .background(TextTertiary, RoundedCornerShape(50))
                        .fillMaxWidth(0.12f)
                        .height(4.dp)
                )
                Text(text = "Add a torrent", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Paste a magnet link or the address of a .torrent file. It plays inside Arc TV, with nothing else to install.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(14.dp))
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("magnet:?xt=urn:btih:…  or  https://…/file.torrent") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MangoSurface,
                        unfocusedContainerColor = MangoSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = ArcAccent,
                        focusedIndicatorColor = ArcAccent,
                        unfocusedIndicatorColor = TextTertiary
                    )
                )
                Spacer(Modifier.height(8.dp))
                MangoButton(
                    text = "Paste from clipboard",
                    icon = Icons.Filled.ContentPaste,
                    onClick = { clipboard.getText()?.text?.trim()?.takeIf { it.isNotEmpty() }?.let { text = it } },
                    compact = true
                )
                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(text = error, color = ErrorCoral, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MangoButton(
                        text = "Add",
                        icon = Icons.Filled.Add,
                        onClick = { onSubmitText(text) },
                        style = MangoButtonStyle.FILLED,
                        borderColor = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MangoButton(
                        text = "Choose file",
                        icon = Icons.Filled.FolderOpen,
                        onClick = { runCatching { picker.launch(arrayOf("application/x-bittorrent", "application/octet-stream", "*/*")) } },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Only add torrents you have the right to watch.",
                    color = TextTertiary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
