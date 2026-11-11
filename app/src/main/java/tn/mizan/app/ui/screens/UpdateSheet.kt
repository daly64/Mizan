package tn.mizan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import tn.mizan.app.data.Updates
import tn.mizan.app.ui.theme.CardWhite
import tn.mizan.app.ui.theme.Danger
import tn.mizan.app.ui.theme.Emerald
import tn.mizan.app.ui.theme.Line
import tn.mizan.app.ui.theme.Muted
import tn.mizan.app.ui.theme.Slate
import java.io.File

/** Panneau « Mise à jour disponible » : notes, téléchargement avec progression, installation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSheet(release: Updates.Release, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var progress by remember { mutableFloatStateOf(0f) }
    var downloading by remember { mutableStateOf(false) }
    var apk by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun installOrAsk(file: File) {
        if (Updates.canInstall(context)) Updates.install(context, file)
        else {
            error = "Autorise AL MIZAN à installer des applications, puis touche « Installer »."
            Updates.openInstallPermission(context)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = CardWhite) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(52.dp)
                        .background(Emerald, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.SystemUpdate, contentDescription = null, tint = Color.White) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Mise à jour disponible", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Slate)
                    val mo = if (release.sizeBytes > 0) " · %.1f Mo".format(release.sizeBytes / 1_048_576f) else ""
                    Text("Version ${release.versionName}$mo · tu as ${Updates.currentVersionName}", fontSize = 13.sp, color = Muted)
                }
            }
            if (release.notes.isNotBlank()) {
                Text(release.notes.take(600), fontSize = 14.sp, lineHeight = 20.sp, color = Slate)
            }
            if (downloading || apk != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row {
                        Text(if (apk != null) "Téléchargée" else "Téléchargement…", fontSize = 12.sp, color = Muted, modifier = Modifier.weight(1f))
                        Text("${(progress * 100).toInt()} %", fontSize = 12.sp, color = Muted)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        color = Emerald,
                        trackColor = Line,
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                }
            }
            error?.let { Text(it, fontSize = 13.sp, color = Danger) }
            Text(
                "Tes dépenses et réglages sont conservés. Android te demandera de confirmer l’installation.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Muted
            )
            Button(
                onClick = {
                    error = null
                    val ready = apk
                    if (ready != null) installOrAsk(ready)
                    else if (!downloading) scope.launch {
                        downloading = true
                        val file = Updates.download(context, release) { progress = it }
                        downloading = false
                        if (file == null) error = "Le téléchargement a échoué. Vérifie ta connexion et réessaie."
                        else { progress = 1f; apk = file; installOrAsk(file) }
                    }
                },
                enabled = !downloading,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color.White),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (apk != null) "Installer" else if (downloading) "Téléchargement…" else "Télécharger et installer", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            TextButton(
                onClick = {
                    Updates.skip(context, release)
                    scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text("Plus tard", color = Emerald, fontWeight = FontWeight.Medium) }
        }
    }
}
