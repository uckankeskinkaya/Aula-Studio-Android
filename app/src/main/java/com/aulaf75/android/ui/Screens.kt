@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.aulaf75.android.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aulaf75.android.KeyboardData
import com.aulaf75.android.MainViewModel
import com.aulaf75.android.protocol.F75
import com.aulaf75.android.protocol.KeyAction
import com.aulaf75.android.protocol.KeyNames
import com.aulaf75.android.protocol.KeyboardLayout
import com.aulaf75.android.protocol.Layer
import com.aulaf75.android.protocol.LightingSettings
import com.aulaf75.android.protocol.Rgb
import java.io.File

private data class Dest(val title: String, val icon: ImageVector)

private val DESTS = listOf(
    Dest("Ana sayfa", Icons.Filled.Home),
    Dest("Işık", Icons.Filled.Star),
    Dest("Tuş RGB", Icons.Filled.Edit),
    Dest("Tuş atama", Icons.Filled.Build),
    Dest("Yedekler", Icons.Filled.Lock),
)

private val EFFECT_EMOJI = mapOf(
    0 to "⚫", 1 to "💡", 2 to "🌬️", 3 to "🌈", 4 to "⚡", 5 to "💧", 6 to "🎡", 7 to "🌊", 8 to "⭐",
    9 to "🌫️", 10 to "🐍", 11 to "🟣", 12 to "⌨️", 13 to "〰️", 14 to "📡", 15 to "🌀", 16 to "🏞️",
    17 to "🌸", 18 to "⛈️",
)

private val EFFECT_DESC = mapOf(
    0 to "Tüm ışıklar kapalı",
    1 to "Her tuş aynı renkte, sabit",
    2 to "Yumuşak bir nabız gibi yanıp söner",
    3 to "Tüm renk yelpazesi akar",
    4 to "Klavye boyunca kayan bir iz",
    5 to "Dağınık ışık noktaları",
    6 to "Dönen renk çarkı",
    7 to "Bastığın tuştan dalga yayılır",
    8 to "Sakin, yıldız gibi parlayan noktalar",
    9 to "Yumuşak renk geçişleri / solma",
    10 to "Kıvrılarak ilerleyen ışık izi",
    11 to "Değişen neon renk vurguları",
    12 to "Yazdıkça tuşlar tepki verir",
    13 to "Akan bir sinüs dalgası",
    14 to "Süpüren bir ışık bandı",
    15 to "Dönen bir desen",
    16 to "Aşağı akan renkli ışık",
    17 to "Merkezden dışa yayılan renk",
    18 to "Enerjik, dönen bir desen",
)

@Composable
fun AppScreen(vm: MainViewModel) {
    val ui = vm.ui
    var tab by rememberSaveable { mutableStateOf(0) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                DESTS.forEachIndexed { i, d ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(d.icon, contentDescription = d.title) },
                        label = { Text(d.title, maxLines = 1, fontSize = 11.sp) },
                    )
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Header(vm)
            if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            ui.error?.let { Banner(it, true, vm::dismissMessages) }
            ui.message?.let { Banner(it, false, vm::dismissMessages) }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val data = ui.data
                when {
                    tab == 0 -> HomeTab(vm)
                    tab == 4 -> BackupTab(vm, data)
                    !ui.connected || data == null -> NotConnected(vm)
                    tab == 1 -> LightingTab(vm, data)
                    tab == 2 -> PerKeyTab(vm, data)
                    tab == 3 -> RemapTab(vm, data)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Header(vm: MainViewModel) {
    val ui = vm.ui
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("AULA F75", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(if (ui.connected) Color(0xFF4ADE80) else MaterialTheme.colorScheme.outline, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (ui.connected) "Bağlı" + (ui.data?.layout?.let { " · ${it.title}" } ?: "") else ui.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (ui.connected) IconButton(onClick = vm::reread, enabled = !ui.busy) {
            Icon(Icons.Filled.Refresh, contentDescription = "Yeniden oku")
        }
    }
}

@Composable
private fun Banner(text: String, error: Boolean, onClose: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                (if (error) "⚠️  " else "✅  ") + text,
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            )
            TextButton(onClick = onClose) { Text("Tamam") }
        }
    }
}

@Composable
private fun NotConnected(vm: MainViewModel) {
    SectionCard("Klavye bağlı değil", "Ayar yapmak için önce klavyeye bağlan.") {
        Button(onClick = vm::connect, enabled = !vm.ui.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Bağlan") }
    }
}

@Composable
private fun ExperimentalGate(vm: MainViewModel, what: String, content: @Composable () -> Unit) {
    if (vm.ui.experimental) content()
    else Text(
        "🔒 $what deneysel bir özellik. Ana sayfadaki \"Deneysel özellikler\" anahtarını açınca kullanabilirsin.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun Expandable(title: String, initiallyOpen: Boolean = false, content: @Composable () -> Unit) {
    var open by rememberSaveable { mutableStateOf(initiallyOpen) }
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Icon(if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
            }
            AnimatedVisibility(open) {
                Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
            }
        }
    }
}

// ------------------------------------------------------------- Ana sayfa

@Composable
private fun HomeTab(vm: MainViewModel) {
    val ui = vm.ui
    val d = ui.data
    var confirmExperimental by remember { mutableStateOf(false) }

    // Hero kart: mevcut efekt rengiyle degrade
    val accent = d?.let { data ->
        val e = data.config.effectId
        if (e in 1..18 && F75.effectSupportsColor(e)) data.palette.effectColor(e).takeIf { !it.isDark() }?.toColor() else null
    } ?: MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(accent.copy(alpha = 0.85f), MaterialTheme.colorScheme.tertiary.copy(alpha = 0.75f))),
                RoundedCornerShape(28.dp),
            )
            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("⌨️", fontSize = 34.sp)
            Text(
                if (ui.connected) "Klavyen hazır" else "Klavyeni bağla",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White,
            )
            Text(
                if (ui.connected) "Işıkları, tuş renklerini ve atamaları buradan yönetebilirsin."
                else "USB-C kablo + OTG ile tak, klavyeyi kablolu moda al ve Bağlan'a dokun.",
                color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = if (ui.connected) vm::reread else vm::connect,
                enabled = !ui.busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF26185A)),
            ) { Text(if (ui.connected) "Yeniden oku" else "Bağlan", fontWeight = FontWeight.SemiBold) }
        }
    }

    d?.let { data ->
        val effect = data.config.effectId
        val lighting = runCatching { data.config.lighting() }.getOrNull()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoTile("Efekt", (EFFECT_EMOJI[effect] ?: "🎨") + " " + (F75.EFFECT_NAMES[effect] ?: if (effect == 21) "Özel RGB" else "#$effect"))
            if (effect in 1..18 && lighting != null) InfoTile("Parlaklık", "${lighting.brightness} / 9")
            InfoTile("Uyku", data.config.sleepSeconds()?.let(::sleepLabel) ?: "?")
            InfoTile("Düzen", data.layout?.title ?: "Tanınmadı")
        }
        if (!data.mappingAllowed) Text(
            "Tuş atama ve tuş RGB kapalı: ${data.ansiError}",
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
        )
        data.readErrors.forEach { Text("Uyarı: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }

    SectionCard("Deneysel özellikler", "Efekt, renk, uyku, tuş RGB ve tuş atama. Kapalıyken yalnızca okuma ve parlaklık yapılır.") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (ui.experimental) "Açık" else "Kapalı", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = ui.experimental, onCheckedChange = { if (it) confirmExperimental = true else vm.setExperimental(false) })
        }
    }

    Expandable("USB teşhis") {
        OutlinedButton(onClick = vm::scanUsb) { Text("USB cihazlarını tara") }
        when (val list = ui.usbDevices) {
            null -> Text("Tabletin gördüğü USB cihazlarını listeler.", style = MaterialTheme.typography.bodySmall)
            else -> if (list.isEmpty()) Text("Hiç USB cihazı görünmüyor. Tableti yeniden başlatmayı dene.")
            else list.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }

    Expandable("Güvenlik ve yardım") {
        Text(
            "• Firmware güncelleme, sıfırlama veya silme komutu bu uygulamada yok.\n" +
                "• Her yazmadan önce otomatik yedek alınır, sonra geri okunup doğrulanır.\n" +
                "• Fn ve F1–F12 tuşları değiştirilemez, hiçbir tuş devre dışı bırakılamaz.\n" +
                "• Sorun olursa: Yedekler → \"0000-ilk-yedek\" → Geri yükle. Klavyenin kendi fabrika " +
                "sıfırlaması genelde Fn + Esc'ye 3–5 sn basılı tutmaktır (kılavuzdan teyit et).\n" +
                "• Tablet klavyeyi görmezse tableti yeniden başlat.",
            style = MaterialTheme.typography.bodySmall,
        )
    }

    if (confirmExperimental) AlertDialog(
        onDismissRequest = { confirmExperimental = false },
        title = { Text("Deneysel özellikler") },
        text = {
            Text(
                "Bu yazma işlemlerinin protokolü kaynak projede tersine mühendislikle çıkarıldı ama gerçek klavyede tam doğrulanmadı. " +
                    "Kalıcı hasar beklenmez (firmware'e dokunulmaz); en kötü ihtimalle ışık/tuş ayarları yanlış olur ve " +
                    "yedekten geri yükleyebilirsin. Açılsın mı?"
            )
        },
        confirmButton = { TextButton(onClick = { vm.setExperimental(true); confirmExperimental = false }) { Text("Aç") } },
        dismissButton = { TextButton(onClick = { confirmExperimental = false }) { Text("Vazgeç") } },
    )
}

@Composable
private fun InfoTile(title: String, value: String) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

fun sleepLabel(s: Int) = when {
    s == 0 -> "Kapalı"
    s < 60 -> "$s sn"
    s % 60 == 0 -> "${s / 60} dk"
    else -> "${s / 60} dk ${s % 60} sn"
}

// ------------------------------------------------------------------- Işık

@Composable
private fun LightingTab(vm: MainViewModel, d: KeyboardData) {
    val busy = vm.ui.busy
    val current = d.config.effectId

    if (!vm.ui.experimental) {
        SectionCard("Parlaklık", "Seçili efektin parlaklığı. Bu, kaynak projede gerçek klavyede test edilmiş güvenli yazmadır.") {
            if (current in 1..18) {
                val stored = d.config.storedSettings(current).brightness
                var b by remember(d) { mutableStateOf(stored) }
                Text("${EFFECT_EMOJI[current]} ${F75.EFFECT_NAMES[current]}", style = MaterialTheme.typography.bodyLarge)
                LabeledSlider("Parlaklık", b, 9) { b = it }
                Button(onClick = { vm.applyBrightness(b) }, enabled = !busy && b != stored, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("Parlaklığı uygula")
                }
            } else {
                Text("Klavye şu an ${if (current == 0) "ışık kapalı" else "özel (tuş başına) RGB"} modunda; bu modda parlaklık ayarı yok.")
            }
        }
        SectionCard("Efekt, renk ve uyku") { ExperimentalGate(vm, "Efekt, renk ve uyku ayarı") {} }
        return
    }

    var effect by remember(d) { mutableStateOf(if (current in 0..18) current else 1) }
    val stored = d.config.storedSettings(effect)
    val storedColor = if (effect in 1..18) d.palette.effectColor(effect) else Rgb(255, 255, 255)
    var brightness by remember(d, effect) { mutableStateOf(if (effect in 1..18 && stored.brightness == 0) 6 else stored.brightness) }
    var speed by remember(d, effect) { mutableStateOf(stored.speed) }
    var colorful by remember(d, effect) { mutableStateOf(stored.colorful) }
    // Klavyede kayıtlı renk siyahsa efekt "çalışmıyor" gibi görünür; beyazdan başlat.
    var color by remember(d, effect) { mutableStateOf(if (storedColor.isDark()) Rgb(255, 255, 255) else storedColor) }

    SectionCard("Efekt") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            F75.EFFECT_NAMES.forEach { (id, name) -> EffectTile(id, name, effect == id, current == id) { effect = id } }
        }
        Text(
            "${EFFECT_EMOJI[effect]}  ${F75.EFFECT_NAMES[effect]}: ${EFFECT_DESC[effect]}",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (effect in setOf(7, 12)) Text("İpucu: Bu efekt tuşlara bastıkça görünür.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }

    if (effect != 0) SectionCard("Ayarlar") {
        LabeledSlider("Parlaklık", brightness, 9) { brightness = it }
        if (F75.effectSupportsSpeed(effect)) LabeledSlider("Hız", speed, 4) { speed = it }
        if (F75.effectSupportsColor(effect)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🌈 Renkli mod", style = MaterialTheme.typography.bodyLarge)
                    Text("Açıkken efekt kendi renk geçişlerini kullanır", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = colorful, onCheckedChange = { colorful = it })
            }
        } else {
            Text("Bu efektin rengi ayarlanamaz; kendi renklerini kullanır.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (effect != 0 && F75.effectSupportsColor(effect) && !colorful) SectionCard("Renk", "Çarktan dokunarak veya sürükleyerek seç") {
        if (storedColor.isDark()) Text(
            "Klavyede bu efekt için kayıtlı renk siyahtı (ışık görünmezdi); beyazdan başlatıldı.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
        )
        ColorPicker(color) { color = it }
    }

    val needsColor = effect != 0 && F75.effectSupportsColor(effect) && !colorful
    val result = runCatching {
        val cfg = d.config.withLighting(LightingSettings(effect, brightness, speed, colorful))
        val pal = if (needsColor && color != d.palette.effectColor(effect)) d.palette.withEffectColor(effect, color) else d.palette
        cfg to pal
    }
    result.exceptionOrNull()?.let { Text("Hata: ${it.message}", color = MaterialTheme.colorScheme.error) }
    Button(
        onClick = { result.getOrNull()?.let { (c, p) -> vm.applyLighting(c, p) } },
        enabled = !busy && result.isSuccess && !(needsColor && color.isDark()),
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(18.dp),
    ) { Text("Klavyeye uygula", fontWeight = FontWeight.SemiBold) }

    SectionCard("Uyku", "Klavye bu süre boyunca kullanılmazsa ışıklar kapanır") {
        val cur = d.config.sleepSeconds()
        var sel by remember(d) { mutableStateOf(cur ?: 300) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            F75.SLEEP_SECONDS.forEach { s -> FilterChip(selected = sel == s, onClick = { sel = s }, label = { Text(sleepLabel(s)) }) }
        }
        FilledTonalButton(onClick = { vm.applySleep(sel) }, enabled = !busy && sel != cur) { Text("Uyku süresini uygula") }
    }
}

@Composable
private fun EffectTile(id: Int, name: String, selected: Boolean, active: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .width(96.dp)
            .background(if (selected) scheme.primaryContainer else scheme.surfaceContainerHigh, RoundedCornerShape(18.dp))
            .border(if (selected) 2.dp else 0.dp, if (selected) scheme.primary else Color.Transparent, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(EFFECT_EMOJI[id] ?: "🎨", fontSize = 24.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2,
            color = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
        )
        if (active) Text("şu an", style = MaterialTheme.typography.labelSmall, color = scheme.primary)
    }
}

// ----------------------------------------------------------------- Tuş RGB

@Composable
private fun PerKeyTab(vm: MainViewModel, d: KeyboardData) {
    SectionCard("Tuş başına RGB", "Tuşlara dokunarak seç, rengi belirle, boya ve klavyeye uygula") {
        ExperimentalGate(vm, "Tuş başına RGB") {
            val perKey = d.perKey
            val layout = d.layout
            when {
                perKey == null -> Text("Tuş renkleri okunamadı.")
                layout == null || !d.mappingAllowed -> Text("Kapalı: ${d.ansiError}")
                else -> {
                    val pending = remember(d) { mutableStateMapOf<Int, Rgb>() }
                    val selected = remember(d) { mutableStateMapOf<Int, Boolean>() }
                    var color by remember { mutableStateOf(Rgb(124, 92, 255)) }
                    KeyboardView(
                        layout = layout,
                        fill = { k -> (pending[k.matrixIndex] ?: perKey.get(k.matrixIndex)).toColor() },
                        label = { it.label },
                        selected = { selected[it.matrixIndex] == true },
                        enabled = { true },
                        onKey = { k -> if (selected[k.matrixIndex] == true) selected.remove(k.matrixIndex) else selected[k.matrixIndex] = true },
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = false, onClick = { layout.keys.forEach { selected[it.matrixIndex] = true } }, label = { Text("Tümünü seç") })
                        FilterChip(selected = false, onClick = { selected.clear() }, label = { Text("Seçimi temizle") })
                        FilterChip(selected = false, onClick = { pending.clear() }, enabled = pending.isNotEmpty(), label = { Text("Boyamayı geri al") })
                    }
                    Text("${selected.size} tuş seçili · ${pending.size} tuş boyandı", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ColorPicker(color) { color = it }
                    FilledTonalButton(onClick = { selected.keys.forEach { pending[it] = color } }, enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                        Text("🎨 Seçilileri boya")
                    }
                    Button(
                        onClick = { vm.applyPerKey(pending.toMap()) },
                        enabled = !vm.ui.busy && (pending.isNotEmpty() || d.config.effectId != 21),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                    ) { Text(if (pending.isEmpty()) "Özel RGB modunu aç" else "Klavyeye uygula (${pending.size} tuş)", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

// --------------------------------------------------------------- Tuş Atama

@Composable
private fun RemapTab(vm: MainViewModel, d: KeyboardData) {
    SectionCard("Tuş atama", "Bir tuşa dokun ve yeni işlevini seç. Soluk tuşlar (Fn, F1–F12, AltGr/sağ Ctrl) korumalıdır.") {
        ExperimentalGate(vm, "Tuş atama") {
            val layout = d.layout
            if (layout == null || !d.mappingAllowed) {
                Text("Kapalı: ${d.ansiError}")
                return@ExperimentalGate
            }
            var layer by remember { mutableStateOf(Layer.BASE) }
            val map = d.keymap(layer)
            val pending = remember(d, layer) { mutableStateMapOf<Int, KeyAction>() }
            var editing by remember { mutableStateOf<Int?>(null) }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = layer == Layer.BASE, onClick = { layer = Layer.BASE }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Ana katman") }
                SegmentedButton(selected = layer == Layer.FN, onClick = { layer = Layer.FN }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Fn katmanı") }
            }
            if (map == null) {
                Text("Bu katman okunamadı.")
                return@ExperimentalGate
            }
            val scheme = MaterialTheme.colorScheme
            KeyboardView(
                layout = layout,
                fill = { k -> if (pending.containsKey(k.matrixIndex)) scheme.tertiary else scheme.surfaceContainerHighest },
                label = { k -> pending[k.matrixIndex]?.let(KeyNames::actionName)?.take(7) ?: KeyNames.shortLabel(map.raw(k.matrixIndex)) },
                selected = { editing == it.matrixIndex },
                enabled = { !it.isProtected },
                onKey = { editing = it.matrixIndex },
            )
            if (pending.isNotEmpty()) {
                HorizontalDivider()
                pending.forEach { (i, a) ->
                    val key = layout.keyAt(i)
                    Text("• ${key?.label ?: i}: ${KeyNames.describeRecord(map.raw(i))} → ${KeyNames.actionName(a)}", style = MaterialTheme.typography.bodyMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pending.clear() }) { Text("İptal") }
                    Button(onClick = { vm.applyRemap(layer, pending.toMap()) }, enabled = !vm.ui.busy, modifier = Modifier.weight(1f)) {
                        Text("Klavyeye uygula (${pending.size})")
                    }
                }
            }
            editing?.let { idx ->
                val key = layout.keyAt(idx)
                ActionPicker(
                    title = "${key?.label ?: idx} (${if (layer == Layer.BASE) "ana" else "Fn"} katman)\nŞu an: ${KeyNames.describeRecord(map.raw(idx))}",
                    original = key?.usage?.let { KeyAction(0, it) },
                    onPick = { a ->
                        if (a == null || map.matches(idx, a)) pending.remove(idx) else pending[idx] = a
                        editing = null
                    },
                    onDismiss = { editing = null },
                )
            }
        }
    }
}

@Composable
private fun ActionPicker(title: String, original: KeyAction?, onPick: (KeyAction?) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val options = remember {
        buildList {
            original?.let { add(it to "↩️ Kendi işlevi (${KeyNames.actionName(it)})") }
            KeyNames.MEDIA.forEach { (u, n) -> add(KeyAction(2, u) to "🎵 $n") }
            KeyNames.MOUSE.forEach { (u, n) -> add(KeyAction(1, u) to "🖱️ $n") }
            KeyNames.ASSIGNABLE_KEYBOARD.forEach { add(KeyAction(0, it) to "⌨️ ${KeyNames.keyboardName(it)}") }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleSmall) },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, label = { Text("Ara (ör. ses, F13, Ctrl)") },
                    singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(options.filter { it.second.contains(query, ignoreCase = true) }) { (action, name) ->
                        Text(
                            name,
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(action) }
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(null) }) { Text("Değişikliği kaldır") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Kapat") } },
    )
}

// ----------------------------------------------------------------- Yedekler

@Composable
private fun BackupTab(vm: MainViewModel, d: KeyboardData?) {
    val context = LocalContext.current
    var confirm by remember { mutableStateOf<File?>(null) }
    SectionCard(
        "Yedekler",
        "Her bağlantıda ve her yazmadan önce otomatik yedek alınır. \"0000-ilk-yedek\" klavyenin ilk hâlidir ve silinmez. " +
            "Uygulamayı kaldırırsan yedekler de silinir; önemlileri Paylaş ile kendine gönder.",
    ) {
        if (d == null) Text("Geri yüklemek için önce klavyeye bağlan.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (vm.ui.backups.isEmpty()) Text("Henüz yedek yok.")
    }
    vm.ui.backups.forEach { f ->
        val first = f.name.startsWith("0000")
        Card(
            Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = if (first) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
            ),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text((if (first) "⭐ " else "🗂️ ") + backupTitle(f), fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = { confirm = f }, enabled = d != null && !vm.ui.busy) { Text("Geri yükle") }
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_SUBJECT, f.name)
                            .putExtra(Intent.EXTRA_TEXT, f.readText())
                        context.startActivity(Intent.createChooser(send, "Yedeği paylaş"))
                    }) { Icon(Icons.Filled.Share, contentDescription = "Paylaş") }
                    Spacer(Modifier.weight(1f))
                    if (!first) TextButton(onClick = { vm.deleteBackup(f) }) { Text("Sil") }
                }
            }
        }
    }
    confirm?.let { f ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Yedeği geri yükle") },
            text = {
                Text(
                    "\"${backupTitle(f)}\" geri yüklensin mi? Işık/renk/uyku ayarları, tuş renkleri ve " +
                        "(korumalı tuşlar hariç) tuş atamaları bu yedekteki hâline döner. Önce mevcut durumun da yedeği alınır."
                )
            },
            confirmButton = { TextButton(onClick = { vm.restore(f); confirm = null }) { Text("Geri yükle") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Vazgeç") } },
        )
    }
}

/** "20261111-153000-once-isik" → "11.11.2026 15:30 · Işık değişikliği öncesi" */
private fun backupTitle(f: File): String {
    val n = f.nameWithoutExtension
    if (n.startsWith("0000")) return "İlk yedek (orijinal ayarlar)"
    val m = Regex("""(\d{4})(\d{2})(\d{2})-(\d{2})(\d{2})(\d{2})-(.*)""").matchEntire(n) ?: return n
    val (y, mo, da, h, mi, _, label) = m.destructured
    val what = when (label) {
        "baglanti" -> "Bağlantı anı"
        "once-parlaklik" -> "Parlaklık değişikliği öncesi"
        "once-isik" -> "Işık değişikliği öncesi"
        "once-uyku" -> "Uyku değişikliği öncesi"
        "once-tus-atama" -> "Tuş ataması öncesi"
        "once-tus-rgb" -> "Tuş RGB öncesi"
        "once-geri-yukleme" -> "Geri yükleme öncesi"
        else -> label
    }
    return "$da.$mo.$y $h:$mi · $what"
}

@Suppress("unused")
private val keepLayoutImport = KeyboardLayout.ANSI
