@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.aulaf75.android.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aulaf75.android.KeyboardController
import com.aulaf75.android.KeyboardData
import com.aulaf75.android.MainViewModel
import com.aulaf75.android.protocol.F75
import com.aulaf75.android.protocol.F75Layout
import com.aulaf75.android.protocol.KeyAction
import com.aulaf75.android.protocol.KeyNames
import com.aulaf75.android.protocol.Layer
import com.aulaf75.android.protocol.LightingSettings
import com.aulaf75.android.protocol.Rgb
import java.io.File

@Composable
fun AppScreen(vm: MainViewModel) {
    val ui = vm.ui
    var tab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Durum", "Işık", "Tuş RGB", "Tuş Atama", "Yedekler")
    Scaffold(topBar = { TopAppBar(title = { Text("AULA F75") }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
                tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
            }
            if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            ui.error?.let { MessageCard(it, true, vm::dismissMessages) }
            ui.message?.let { MessageCard(it, false, vm::dismissMessages) }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (tab) {
                    0 -> StatusTab(vm)
                    else -> {
                        val data = ui.data
                        if (!ui.connected || data == null) {
                            Text("Önce 'Durum' sekmesinden klavyeye bağlan.")
                            if (tab == 4) BackupTab(vm, null)
                        } else when (tab) {
                            1 -> LightingTab(vm, data)
                            2 -> PerKeyTab(vm, data)
                            3 -> RemapTab(vm, data)
                            4 -> BackupTab(vm, data)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageCard(text: String, error: Boolean, onClose: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
        ),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Tamam") }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ExperimentalGate(vm: MainViewModel, what: String, content: @Composable () -> Unit) {
    if (vm.ui.experimental) content()
    else Text(
        "$what deneysel bir özellik. Kullanmak için 'Durum' sekmesindeki \"Deneysel yazma özellikleri\" anahtarını aç.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ------------------------------------------------------------------ Durum

@Composable
private fun StatusTab(vm: MainViewModel) {
    val ui = vm.ui
    var confirmExperimental by remember { mutableStateOf(false) }
    Section("Bağlantı") {
        Text(ui.status)
        ui.identity?.let { Text("Model kimliği: $it (orijinal F75 ✓)") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = vm::connect, enabled = !ui.busy) { Text(if (ui.connected) "Yeniden bağlan" else "Bağlan") }
            OutlinedButton(onClick = vm::reread, enabled = ui.connected && !ui.busy) { Text("Yeniden oku") }
        }
        Text(
            "Klavyeyi USB-C kablo ve OTG adaptörle telefona tak, klavyeyi kablolu moda al. 2.4G alıcı veya Bluetooth ile ayar yapılamaz.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
    Section("USB teşhis") {
        OutlinedButton(onClick = vm::scanUsb) { Text("USB cihazlarını tara") }
        when (val list = ui.usbDevices) {
            null -> Text("Tabletin gördüğü USB cihazlarını listelemek için dokun.", style = MaterialTheme.typography.bodySmall)
            else -> if (list.isEmpty()) Text("Hiç USB cihazı görünmüyor (klavye sadece şarj oluyor olabilir).")
            else list.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
    ui.data?.let { d ->
        Section("Klavyedeki ayarlar") {
            val effect = d.config.effectId
            Text("Efekt: " + (F75.EFFECT_NAMES[effect] ?: if (effect == 21) "Özel (tuş başına RGB)" else "Bilinmiyor ($effect)"))
            runCatching { d.config.lighting() }.getOrNull()?.takeIf { effect in 1..18 }?.let {
                Text("Parlaklık: ${it.brightness}/9 · Hız: ${it.speed}/4 · ${if (it.colorful) "Renkli" else "Tek renk"}")
            }
            Text("Uyku: " + (d.config.sleepSeconds()?.let(::sleepLabel) ?: "bilinmiyor"))
            Text(
                if (d.mappingAllowed) "ANSI düzen doğrulandı: tuş atama ve tuş RGB kullanılabilir."
                else "Tuş atama / tuş RGB kapalı: ${d.ansiError}"
            )
            d.readErrors.forEach { Text("Uyarı: $it", color = MaterialTheme.colorScheme.error) }
        }
    }
    Section("Deneysel yazma özellikleri") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Efekt/renk, uyku, tuş RGB ve tuş atama", Modifier.weight(1f))
            Switch(checked = ui.experimental, onCheckedChange = { if (it) confirmExperimental = true else vm.setExperimental(false) })
        }
        Text(
            "Kapalıyken yalnızca okuma ve parlaklık değişikliği yapılır. Parlaklık yazımı, kaynak projede gerçek bir F75'te test edilmiş tek yazma işlemidir.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
    Section("Güvenlik") {
        Text(
            "• Firmware güncelleme, sıfırlama veya silme komutu bu uygulamada yok.\n" +
                "• Her yazmadan önce otomatik yedek alınır, sonra geri okunup doğrulanır.\n" +
                "• Fn ve F1–F12 tuşları değiştirilemez, hiçbir tuş devre dışı bırakılamaz.\n" +
                "• Bir sorun olursa: 'Yedekler' sekmesinden \"İlk yedek\"i geri yükle. Klavyenin kendi fabrika " +
                "sıfırlaması genelde Fn + Esc'ye 3–5 sn basılı tutmaktır (kılavuzdan teyit et).\n" +
                "• İşin bitince kabloyu çıkarıp tak; klavye normale döner.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (confirmExperimental) AlertDialog(
        onDismissRequest = { confirmExperimental = false },
        title = { Text("Deneysel özellikler") },
        text = {
            Text(
                "Bu yazma işlemlerinin protokolü kaynak projede tersine mühendislikle çıkarıldı ama gerçek klavyede tam doğrulanmadı. " +
                    "Kalıcı hasar beklenmez (firmware'e dokunulmaz), en kötü ihtimalle ışık/tuş ayarları yanlış olur; " +
                    "yedekten geri yükleyebilir veya klavyeyi fabrika ayarına döndürebilirsin. Açılsın mı?"
            )
        },
        confirmButton = { TextButton(onClick = { vm.setExperimental(true); confirmExperimental = false }) { Text("Aç") } },
        dismissButton = { TextButton(onClick = { confirmExperimental = false }) { Text("Vazgeç") } },
    )
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
    Section("Parlaklık (güvenli)") {
        if (current in 1..18) {
            val stored = d.config.storedSettings(current).brightness
            var b by remember(d) { mutableStateOf(stored) }
            Text("Seçili efekt: ${F75.EFFECT_NAMES[current]} · Parlaklık: $b / 9")
            Slider(value = b.toFloat(), onValueChange = { b = it.toInt() }, valueRange = 0f..9f, steps = 8)
            Button(onClick = { vm.applyBrightness(b) }, enabled = !busy && b != stored) { Text("Parlaklığı yaz") }
        } else {
            Text("Klavye şu an ${if (current == 0) "ışık kapalı" else "özel (tuş başına) RGB"} modunda; bu modun parlaklık alanı yok. Aşağıdan bir efekt seç.")
        }
    }
    Section("Efekt ve renk") {
        ExperimentalGate(vm, "Efekt/renk değiştirme") {
            var effect by remember(d) { mutableStateOf(if (current in 0..18) current else 1) }
            val stored = d.config.storedSettings(effect)
            var brightness by remember(d, effect) { mutableStateOf(if (effect in 1..18) stored.brightness else 5) }
            var speed by remember(d, effect) { mutableStateOf(stored.speed) }
            var colorful by remember(d, effect) { mutableStateOf(stored.colorful) }
            var color by remember(d, effect) { mutableStateOf(if (effect in 1..18) d.palette.effectColor(effect) else Rgb(255, 255, 255)) }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                F75.EFFECT_NAMES.forEach { (id, name) ->
                    FilterChip(selected = effect == id, onClick = { effect = id }, label = { Text(name) })
                }
            }
            if (effect != 0) {
                Text("Parlaklık: $brightness / 9")
                Slider(value = brightness.toFloat(), onValueChange = { brightness = it.toInt() }, valueRange = 0f..9f, steps = 8)
                if (F75.effectSupportsSpeed(effect)) {
                    Text("Hız: $speed / 4")
                    Slider(value = speed.toFloat(), onValueChange = { speed = it.toInt() }, valueRange = 0f..4f, steps = 3)
                }
                if (F75.effectSupportsColor(effect)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Renkli (gökkuşağı)", Modifier.weight(1f))
                        Switch(checked = colorful, onCheckedChange = { colorful = it })
                    }
                    if (!colorful) ColorPicker(color) { color = it }
                }
            }
            val result = runCatching {
                val cfg = d.config.withLighting(LightingSettings(effect, brightness, speed, colorful))
                val pal = if (F75.effectSupportsColor(effect) && !colorful && color != d.palette.effectColor(effect))
                    d.palette.withEffectColor(effect, color) else d.palette
                cfg to pal
            }
            result.exceptionOrNull()?.let { Text("Hata: ${it.message}", color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = { result.getOrNull()?.let { (c, p) -> vm.applyLighting(c, p) } },
                enabled = !busy && result.isSuccess,
            ) { Text("Klavyeye yaz") }
        }
    }
    Section("Uyku (otomatik ışık kapanma)") {
        ExperimentalGate(vm, "Uyku süresi") {
            val cur = d.config.sleepSeconds()
            var sel by remember(d) { mutableStateOf(cur ?: 300) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                F75.SLEEP_SECONDS.forEach { s -> FilterChip(selected = sel == s, onClick = { sel = s }, label = { Text(sleepLabel(s)) }) }
            }
            Button(onClick = { vm.applySleep(sel) }, enabled = !busy && sel != cur) { Text("Uyku süresini yaz") }
        }
    }
}

// ----------------------------------------------------------------- Tuş RGB

@Composable
private fun PerKeyTab(vm: MainViewModel, d: KeyboardData) {
    Section("Tuş başına RGB") {
        ExperimentalGate(vm, "Tuş başına RGB") {
            val perKey = d.perKey
            when {
                perKey == null -> Text("Tuş renkleri okunamadı.")
                !d.mappingAllowed -> Text("Kapalı: ${d.ansiError}")
                else -> {
                    val pending = remember(d) { mutableStateMapOf<Int, Rgb>() }
                    val selected = remember(d) { mutableStateMapOf<Int, Boolean>() }
                    var color by remember { mutableStateOf(Rgb(0, 160, 255)) }
                    Text("Tuşlara dokunarak seç, renk belirle, \"Seçilileri boya\" de, sonra klavyeye yaz. Yazınca klavye özel (tuş başına) moda geçer.", style = MaterialTheme.typography.bodySmall)
                    KeyboardView(
                        fill = { k -> (pending[k.matrixIndex] ?: perKey.get(k.matrixIndex)).toColor() },
                        label = { it.label },
                        selected = { selected[it.matrixIndex] == true },
                        enabled = { true },
                        onKey = { k -> if (selected[k.matrixIndex] == true) selected.remove(k.matrixIndex) else selected[k.matrixIndex] = true },
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { F75Layout.KEYS.forEach { selected[it.matrixIndex] = true } }) { Text("Tümünü seç") }
                        OutlinedButton(onClick = { selected.clear() }) { Text("Seçimi temizle") }
                        OutlinedButton(onClick = { pending.clear() }, enabled = pending.isNotEmpty()) { Text("Değişiklikleri iptal") }
                    }
                    ColorPicker(color) { color = it }
                    Button(onClick = { selected.keys.forEach { pending[it] = color } }, enabled = selected.isNotEmpty()) {
                        Text("Seçilileri boya (${selected.size})")
                    }
                    HorizontalDivider()
                    Button(onClick = { vm.applyPerKey(pending.toMap()) }, enabled = !vm.ui.busy && (pending.isNotEmpty() || d.config.effectId != 21)) {
                        Text(if (pending.isEmpty()) "Özel modu etkinleştir" else "Klavyeye yaz (${pending.size} tuş)")
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------- Tuş Atama

@Composable
private fun RemapTab(vm: MainViewModel, d: KeyboardData) {
    Section("Tuş atama") {
        ExperimentalGate(vm, "Tuş atama") {
            if (!d.mappingAllowed) {
                Text("Kapalı: ${d.ansiError}")
                return@ExperimentalGate
            }
            var layer by remember { mutableStateOf(Layer.BASE) }
            val map = d.keymap(layer)
            val pending = remember(d, layer) { mutableStateMapOf<Int, KeyAction>() }
            var editing by remember { mutableStateOf<Int?>(null) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = layer == Layer.BASE, onClick = { layer = Layer.BASE }, label = { Text("Ana katman") })
                FilterChip(selected = layer == Layer.FN, onClick = { layer = Layer.FN }, label = { Text("Fn katmanı") })
            }
            if (map == null) {
                Text("Bu katman okunamadı.")
                return@ExperimentalGate
            }
            Text("Değiştirmek istediğin tuşa dokun. Soluk tuşlar (Fn, F1–F12) korumalıdır. Turuncu tuşlar bekleyen değişikliklerdir.", style = MaterialTheme.typography.bodySmall)
            KeyboardView(
                fill = { k -> if (pending.containsKey(k.matrixIndex)) Color(0xFFFF9800) else Color(0xFF37474F) },
                label = { k -> pending[k.matrixIndex]?.let(KeyNames::actionName)?.take(7) ?: KeyNames.shortLabel(map.raw(k.matrixIndex)) },
                selected = { editing == it.matrixIndex },
                enabled = { !it.isProtected },
                onKey = { editing = it.matrixIndex },
            )
            if (pending.isNotEmpty()) {
                pending.forEach { (i, a) ->
                    val key = F75Layout.KEYS.first { it.matrixIndex == i }
                    Text("${key.label}: ${KeyNames.describeRecord(map.raw(i))} → ${KeyNames.actionName(a)}")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pending.clear() }) { Text("İptal") }
                    Button(onClick = { vm.applyRemap(layer, pending.toMap()) }, enabled = !vm.ui.busy) { Text("Klavyeye yaz (${pending.size})") }
                }
            }
            editing?.let { idx ->
                val key = F75Layout.KEYS.first { it.matrixIndex == idx }
                ActionPicker(
                    title = "${key.label} tuşu (${if (layer == Layer.BASE) "ana" else "Fn"}) · şu an: ${KeyNames.describeRecord(map.raw(idx))}",
                    original = key.usage?.let { KeyAction(0, it) },
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
            original?.let { add(it to "Kendi işlevi (${KeyNames.actionName(it)})") }
            KeyNames.ASSIGNABLE_KEYBOARD.forEach { add(KeyAction(0, it) to "Klavye: ${KeyNames.keyboardName(it)}") }
            KeyNames.MEDIA.forEach { (u, n) -> add(KeyAction(2, u) to "Medya: $n") }
            KeyNames.MOUSE.forEach { (u, n) -> add(KeyAction(1, u) to "Fare: $n") }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleSmall) },
        text = {
            Column {
                OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Ara") }, singleLine = true)
                Spacer(Modifier.width(4.dp))
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(options.filter { it.second.contains(query, ignoreCase = true) }) { (action, name) ->
                        Text(
                            name,
                            Modifier
                                .fillMaxWidth()
                                .selectable(selected = false, onClick = { onPick(action) })
                                .padding(vertical = 10.dp)
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
    Section("Yedekler") {
        Text(
            "Her bağlantıda ve her yazmadan önce otomatik yedek alınır. \"0000-ilk-yedek\" klavyenin uygulamaya ilk bağlandığı andaki ayarlarıdır ve silinmez. " +
                "Uygulamayı kaldırırsan yedekler de silinir; önemli olanları \"Paylaş\" ile kendine gönder.",
            style = MaterialTheme.typography.bodySmall,
        )
        if (vm.ui.backups.isEmpty()) Text("Henüz yedek yok.")
        vm.ui.backups.forEach { f ->
            Column {
                Text(f.nameWithoutExtension, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { confirm = f }, enabled = d != null && !vm.ui.busy) { Text("Geri yükle") }
                    OutlinedButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_SUBJECT, f.name)
                            .putExtra(Intent.EXTRA_TEXT, f.readText())
                        context.startActivity(Intent.createChooser(send, "Yedeği paylaş"))
                    }) { Text("Paylaş") }
                    if (!f.name.startsWith("0000")) TextButton(onClick = { vm.deleteBackup(f) }) { Text("Sil") }
                }
                HorizontalDivider(Modifier.padding(top = 6.dp))
            }
        }
    }
    confirm?.let { f ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Yedeği geri yükle") },
            text = {
                Text(
                    "\"${f.nameWithoutExtension}\" geri yüklensin mi? Işık/renk/uyku ayarları, tuş renkleri ve " +
                        "(F1–F12/Fn hariç) tuş atamaları bu yedekteki hâline döner. Önce mevcut durumun da yedeği alınır."
                )
            },
            confirmButton = { TextButton(onClick = { vm.restore(f); confirm = null }) { Text("Geri yükle") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Vazgeç") } },
        )
    }
}

@Suppress("unused")
private val sourceCount = KeyboardController.sourceIndices.size
