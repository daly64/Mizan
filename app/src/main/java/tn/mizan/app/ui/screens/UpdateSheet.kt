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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import tn.mizan.app.ui.theme.Emerald
import tn.mizan.app.ui.theme.Muted
import tn.mizan.app.ui.theme.Slate

/** Panneau « Mise à jour disponible » : notes, téléchargement avec progression, installation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSheet(release: Updates.Release, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
            Text(
                "Le navigateur télécharge la nouvelle version : touche ensuite le fichier pour l’installer. Tes dépenses et réglages sont conservés.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Muted
            )
            Button(
                onClick = { Updates.openDownload(context, release); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color.White),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Télécharger la mise à jour", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
