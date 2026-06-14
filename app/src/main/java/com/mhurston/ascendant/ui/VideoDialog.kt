package com.mhurston.ascendant.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mhurston.ascendant.domain.VideoCatalog
import com.mhurston.ascendant.domain.VideoLink
import com.mhurston.ascendant.ui.theme.AuraCyan
import com.mhurston.ascendant.ui.theme.ManaPurple
import com.mhurston.ascendant.ui.theme.TextDim
import com.mhurston.ascendant.ui.theme.XpGold

@Composable
fun VideoDialog(
    exerciseKey: String,
    state: UiState,
    onToggleFav: (String) -> Unit,
    onAddUrl: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val favs = state.favoriteVideoUrls
    val videos: List<VideoLink> = remember(exerciseKey, state.userVideos) {
        (VideoCatalog.defaultsFor(exerciseKey) +
            state.userVideos.filter { it.exerciseKey == exerciseKey })
            .sortedByDescending { it.url in state.favoriteVideoUrls }
    }
    var newUrl by remember { mutableStateOf("") }

    fun open(url: String) {
        // Open in an in-app Custom Tab so the back gesture returns to ASCENDANT
        // (instead of getting stranded in the YouTube app). Falls back to a normal
        // browser intent if no Custom Tabs provider is available.
        try {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(context, Uri.parse(url))
        } catch (e: Exception) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e2: Exception) {
                Toast.makeText(context, "No app to open this link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("${VideoCatalog.displayName(exerciseKey)} — Form videos",
                    fontWeight = FontWeight.Bold, color = ManaPurple)
            }
        },
        text = {
            Column {
                Text("Opens on YouTube. Tap ★ to favorite, or add your own link below.",
                    style = MaterialTheme.typography.labelMedium, color = TextDim)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { videos.randomOrNull()?.let { open(it.url) } },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = XpGold)
                ) { Text("🔀 Shuffle — surprise me") }
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 280.dp)) {
                    items(videos, key = { it.url }) { v ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (v.url in favs) "★" else "☆",
                                color = if (v.url in favs) XpGold else TextDim,
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .clickable { onToggleFav(v.url) }
                            )
                            Column(Modifier.weight(1f).clickable { open(v.url) }) {
                                Text("▶ " + v.title, color = AuraCyan,
                                    style = MaterialTheme.typography.bodyLarge)
                                if (v.userAdded) Text("your link",
                                    style = MaterialTheme.typography.labelMedium, color = TextDim)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newUrl,
                    onValueChange = { newUrl = it },
                    label = { Text("Paste a YouTube URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        val u = newUrl.trim()
                        if (u.startsWith("http")) {
                            onAddUrl(exerciseKey, "My video", u)
                            newUrl = ""
                        } else {
                            Toast.makeText(context, "Enter a valid http(s) link", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ManaPurple)
                ) { Text("Add to my videos") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}
