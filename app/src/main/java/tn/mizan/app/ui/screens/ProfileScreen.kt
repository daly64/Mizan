package tn.mizan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.mizan.app.ui.MonthState
import tn.mizan.app.ui.components.IconCircle
import tn.mizan.app.ui.components.MizanCard
import tn.mizan.app.ui.components.MoneyText
import tn.mizan.app.ui.theme.Emerald
import tn.mizan.app.ui.theme.Line
import tn.mizan.app.ui.theme.Muted
import tn.mizan.app.ui.theme.Slate

@Composable
fun ProfileScreen(state: MonthState) {
    var reminder by remember { mutableStateOf(true) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Profil", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Slate)

        MizanCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).background(Emerald, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Mon profil", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Slate)
                    Text("Données sur ce téléphone", fontSize = 12.sp, color = Muted)
                }
            }
        }

        Group("Préférences") {
            SettingRow(Icons.Outlined.Payments, "Devise") { Value("Dinar tunisien") }
            Divider()
            SettingRow(Icons.Outlined.CreditCard, "Budget mensuel") {
                MoneyText(state.budgetTotal, 13.sp, Muted)
            }
            Divider()
            SettingRow(Icons.Outlined.CalendarMonth, "Début du mois") { Value("Le 1er") }
            Divider()
            SettingRow(Icons.Outlined.Notifications, "Rappel quotidien", chevron = false) {
                Switch(
                    checked = reminder,
                    onCheckedChange = { reminder = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Emerald)
                )
            }
            Divider()
            SettingRow(Icons.Outlined.LightMode, "Thème") { Value("Clair") }
        }

        Group("Données") {
            SettingRow(Icons.Outlined.FileDownload, "Exporter en CSV") { Value("Bientôt") }
            Divider()
            SettingRow(Icons.Outlined.CloudUpload, "Sauvegarde") { Value("Bientôt") }
            Divider()
            SettingRow(Icons.Outlined.Info, "À propos de Mizan") { Value("v0.5") }
        }
    }

}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Muted)
    MizanCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) { content() }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    chevron: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircle(icon, 32.dp)
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Slate, modifier = Modifier.weight(1f))
        trailing()
        if (chevron) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun Value(text: String) {
    Text(text, fontSize = 13.sp, color = Muted)
}

@Composable
private fun Divider() {
    HorizontalDivider(color = Line)
}
