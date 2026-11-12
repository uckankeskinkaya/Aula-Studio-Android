#!/usr/bin/env python3
"""Single source of truth for UI strings. Generates values/, values-tr/ and values-zh-rCN/ strings.xml.
Run from the project root:  python3 tools/strings.py   (CI checks the generated files are up to date)."""
import os, sys

S = {  # key: (English, Turkish, Simplified Chinese)
 "app_name": ("AULA Studio", "AULA Studio", "AULA Studio"),
 "nav_overview": ("Overview", "Genel bakış", "概览"),
 "nav_lighting": ("Lighting", "Işık", "灯效"),
 "nav_per_key": ("Key colors", "Tuş renkleri", "单键灯光"),
 "nav_mapping": ("Key mapping", "Tuş atama", "按键映射"),
 "nav_settings": ("Settings", "Ayarlar", "设置"),
 "navs_overview": ("Home", "Özet", "概览"),
 "navs_lighting": ("Lighting", "Işık", "灯效"),
 "navs_per_key": ("Colors", "Renkler", "单键灯光"),
 "navs_mapping": ("Mapping", "Atama", "按键映射"),
 "navs_settings": ("Settings", "Ayarlar", "设置"),
 "status_connected": ("Connected", "Bağlı", "已连接"),
 "status_disconnected": ("Not connected", "Bağlı değil", "未连接"),
 "wired_usb": ("Wired USB", "Kablolu USB", "有线 USB"),
 "layout_ansi": ("ANSI layout", "ANSI düzen", "ANSI 布局"),
 "layout_iso": ("ISO layout", "ISO düzen", "ISO 布局"),
 "model_identity": ("Model identity %1$s · verified", "Model kimliği %1$s · doğrulandı", "型号标识 %1$s · 已验证"),
 "hero_body": ("Every change is backed up first and verified by reading the keyboard back.",
               "Her değişiklikten önce yedek alınır, ardından klavye geri okunarak doğrulanır.",
               "每次更改前都会先备份，写入后会回读键盘进行校验。"),
 "busy_connecting": ("Connecting…", "Bağlanıyor…", "正在连接…"),
 "busy_reading": ("Reading keyboard…", "Klavye okunuyor…", "正在读取键盘…"),
 "busy_writing": ("Writing and verifying…", "Yazılıyor ve doğrulanıyor…", "正在写入并校验…"),
 "busy_restoring": ("Restoring backup…", "Yedek geri yükleniyor…", "正在恢复备份…"),
 "msg_saved_verified": ("Saved to the keyboard and verified.", "Klavyeye kaydedildi ve doğrulandı.", "已写入键盘并校验通过。"),
 "msg_remap_saved": ("%1$d key assignment(s) saved and verified.", "%1$d tuş ataması kaydedildi ve doğrulandı.", "已保存并校验 %1$d 个按键映射。"),
 "msg_perkey_saved": ("Key colors saved. The keyboard is now in custom color mode.", "Tuş renkleri kaydedildi. Klavye artık özel renk modunda.", "单键灯光已保存，键盘已切换到自定义灯光模式。"),
 "msg_restored": ("Backup restored and verified.", "Yedek geri yüklendi ve doğrulandı.", "备份已恢复并校验通过。"),
 "action_ok": ("OK", "Tamam", "好"),
 "action_cancel": ("Cancel", "Vazgeç", "取消"),
 "action_connect": ("Connect", "Bağlan", "连接"),
 "action_reread": ("Read again", "Yeniden oku", "重新读取"),
 "action_apply": ("Apply", "Uygula", "应用"),
 "action_apply_to_keyboard": ("Apply to keyboard", "Klavyeye uygula", "应用到键盘"),
 "action_apply_keys": ("Apply %1$d key(s)", "%1$d tuşu uygula", "应用 %1$d 个按键"),
 "action_enable": ("Enable", "Aç", "启用"),
 "action_enable_experimental": ("Enable experimental features", "Deneysel özellikleri aç", "启用实验性功能"),
 "action_enable_custom": ("Switch to custom colors", "Özel renk moduna geç", "切换到自定义灯光"),
 "action_paint_selected": ("Paint selected keys", "Seçili tuşları boya", "为所选按键着色"),
 "action_select_all": ("Select all", "Tümünü seç", "全选"),
 "action_clear_selection": ("Clear selection", "Seçimi temizle", "清除选择"),
 "action_undo_paint": ("Undo painting", "Boyamayı geri al", "撤销着色"),
 "action_discard": ("Discard", "Vazgeç", "放弃"),
 "action_remove": ("Remove", "Kaldır", "移除"),
 "action_reset_key": ("Restore default", "Varsayılana döndür", "恢复默认"),
 "action_undo_change": ("Keep current", "Mevcut kalsın", "保持当前"),
 "action_restore": ("Restore", "Geri yükle", "恢复"),
 "action_share": ("Share", "Paylaş", "分享"),
 "action_delete": ("Delete", "Sil", "删除"),
 "action_scan_usb": ("Scan USB devices", "USB cihazlarını tara", "扫描 USB 设备"),
 "state_on": ("On", "Açık", "开"),
 "state_off": ("Off", "Kapalı", "关"),
 # connect
 "connect_title": ("Connect your AULA F75", "AULA F75'ini bağla", "连接你的 AULA F75"),
 "connect_subtitle": ("Configure lighting, key colors and key mapping without a computer.",
                      "Bilgisayar olmadan ışıkları, tuş renklerini ve tuş atamalarını ayarla.",
                      "无需电脑即可设置灯效、单键灯光和按键映射。"),
 "connect_step_wired": ("Set the mode switch on the keyboard to wired (USB)", "Klavyedeki mod anahtarını kabloya (USB) al", "将键盘模式开关拨到有线（USB）"),
 "connect_step_otg": ("Connect it with a USB-C cable or an OTG adapter", "USB-C kablo veya OTG adaptörle bağla", "使用 USB-C 数据线或 OTG 转接头连接"),
 "connect_step_permission": ("Tap Connect and allow USB access", "Bağlan'a dokun ve USB iznini ver", "点击“连接”并允许 USB 访问"),
 "connect_hint_restart": ("Device not found? Restarting the phone or tablet often fixes a stuck USB port.",
                          "Cihaz bulunamadı mı? Telefonu veya tableti yeniden başlatmak takılan USB portunu genelde düzeltir.",
                          "找不到设备？重启手机或平板通常可以修复无响应的 USB 端口。"),
 "not_connected_title": ("Keyboard not connected", "Klavye bağlı değil", "键盘未连接"),
 "not_connected_body": ("Connect the keyboard to change these settings.", "Bu ayarları değiştirmek için klavyeyi bağla.", "请先连接键盘以更改这些设置。"),
 # overview
 "stat_effect": ("Effect", "Efekt", "灯效"),
 "stat_brightness": ("Brightness", "Parlaklık", "亮度"),
 "stat_sleep": ("Sleep", "Uyku", "休眠"),
 "stat_layout": ("Layout", "Düzen", "布局"),
 "quick_brightness": ("Brightness", "Parlaklık", "亮度"),
 "quick_brightness_sub": ("The safest change: validated on real hardware upstream.", "En güvenli değişiklik: kaynak projede gerçek donanımda doğrulandı.", "最安全的更改：已在上游项目的真实硬件上验证。"),
 "quick_brightness_unavailable": ("The current mode (off or custom colors) has no brightness setting.", "Mevcut modun (kapalı veya özel renk) parlaklık ayarı yok.", "当前模式（关闭或自定义灯光）没有亮度设置。"),
 "notice_mapping_unavailable": ("Key mapping and key colors are disabled: the Fn key position was not recognised on this keyboard revision.",
                                "Tuş atama ve tuş renkleri kapalı: bu klavye revizyonunda Fn tuşunun konumu tanınmadı.",
                                "按键映射和单键灯光已禁用：无法识别此键盘版本的 Fn 键位置。"),
 # experimental
 "experimental_title": ("Experimental features", "Deneysel özellikler", "实验性功能"),
 "experimental_sub": ("Effects, colors, sleep, key colors and key mapping. Their protocol is reverse-engineered but not yet validated on hardware upstream.",
                      "Efektler, renkler, uyku, tuş renkleri ve tuş atama. Protokolleri tersine mühendislikle çıkarıldı ama kaynak projede donanımda henüz doğrulanmadı.",
                      "灯效、颜色、休眠、单键灯光和按键映射。其协议来自逆向工程，尚未在上游项目的硬件上验证。"),
 "experimental_locked": ("Turn on experimental features to use this editor.", "Bu düzenleyiciyi kullanmak için deneysel özellikleri aç.", "开启实验性功能后即可使用此编辑器。"),
 "experimental_dialog_title": ("Enable experimental features?", "Deneysel özellikler açılsın mı?", "启用实验性功能？"),
 "experimental_dialog_body": ("These writes follow the reverse-engineered protocol but have not been fully validated on a real keyboard. Firmware is never touched, so permanent damage is not expected; at worst lighting or keys are set wrongly and you can restore a backup or factory-reset the keyboard.",
                              "Bu yazmalar tersine mühendislikle çıkarılan protokolü izler ama gerçek bir klavyede tam doğrulanmadı. Firmware'e asla dokunulmaz, bu yüzden kalıcı hasar beklenmez; en kötü ihtimalle ışıklar veya tuşlar yanlış ayarlanır ve yedekten geri yükleyebilir ya da klavyeyi fabrika ayarına döndürebilirsin.",
                              "这些写入遵循逆向工程得出的协议，但尚未在真实键盘上完全验证。本应用从不触及固件，因此不会造成永久损坏；最坏情况是灯光或按键设置错误，你可以恢复备份或将键盘恢复出厂设置。"),
 # lighting
 "lighting_effects_title": ("Effects", "Efektler", "灯效"),
 "lighting_effects_sub": ("Previews are illustrative; the keyboard renders the real effect.", "Önizlemeler temsilidir; gerçek efekti klavye kendisi çizer.", "预览仅供参考，实际效果由键盘渲染。"),
 "badge_active": ("ACTIVE", "AKTİF", "当前"),
 "preview_note": ("Live preview of the selected settings.", "Seçili ayarların canlı önizlemesi.", "所选设置的实时预览。"),
 "label_brightness": ("Brightness", "Parlaklık", "亮度"),
 "label_speed": ("Speed", "Hız", "速度"),
 "label_colorful": ("Rainbow colors", "Gökkuşağı renkleri", "彩虹色"),
 "label_colorful_sub": ("Use the effect's own color cycle instead of one color", "Tek renk yerine efektin kendi renk döngüsünü kullan", "使用灯效自带的颜色循环，而非单一颜色"),
 "label_color": ("Color", "Renk", "颜色"),
 "label_color_sub": ("Tap or drag on the wheel", "Çarka dokun veya sürükle", "点击或拖动色轮"),
 "effect_fixed_colors": ("This effect uses its own colors.", "Bu efekt kendi renklerini kullanır.", "此灯效使用固定颜色。"),
 "effect_custom": ("Custom colors", "Özel renkler", "自定义灯光"),
 "color_intensity": ("Color brightness", "Renk parlaklığı", "颜色亮度"),
 "color_hex": ("Hex", "Hex", "十六进制"),
 "color_too_dark": ("This color is too dark: the keys would look off.", "Bu renk çok koyu: tuşlar kapalı gibi görünür.", "颜色过暗：按键看起来会像熄灭一样。"),
 "color_was_black": ("The color stored on the keyboard for this effect was black, so the effect looked off. Starting from white.",
                     "Klavyede bu efekt için kayıtlı renk siyahtı, bu yüzden efekt kapalı gibi görünüyordu. Beyazdan başlatıldı.",
                     "键盘中为此灯效保存的颜色是黑色，因此看起来像熄灭。已改为从白色开始。"),
 "sleep_title": ("Sleep", "Uyku", "休眠"),
 "sleep_sub": ("Lights turn off after this much idle time", "Bu kadar süre kullanılmazsa ışıklar kapanır", "闲置该时长后关闭灯光"),
 "sleep_never": ("Never", "Asla", "从不"),
 "duration_min": ("%1$d min", "%1$d dk", "%1$d 分钟"),
 "duration_min_sec": ("%1$d min %2$d s", "%1$d dk %2$d sn", "%1$d 分 %2$d 秒"),
 # per-key
 "perkey_title": ("Key colors", "Tuş renkleri", "单键灯光"),
 "perkey_sub": ("Tap keys to select them, pick a color and paint.", "Tuşlara dokunarak seç, bir renk belirle ve boya.", "点击按键进行选择，选择颜色后着色。"),
 "perkey_counts": ("%1$d selected · %2$d painted", "%1$d seçili · %2$d boyandı", "已选 %1$d · 已着色 %2$d"),
 "perkey_apply_title": ("Apply", "Uygula", "应用"),
 "perkey_apply_sub": ("Writing key colors switches the keyboard to custom color mode.", "Tuş renklerini yazmak klavyeyi özel renk moduna alır.", "写入单键灯光会将键盘切换到自定义灯光模式。"),
 # mapping
 "mapping_title": ("Key mapping", "Tuş atama", "按键映射"),
 "mapping_sub": ("Tap a key to choose what it does. F1–F12 and Fn are protected.", "Ne yapacağını seçmek için bir tuşa dokun. F1–F12 ve Fn korunur.", "点击按键选择其功能。F1–F12 和 Fn 受保护。"),
 "layer_base": ("Base layer", "Ana katman", "基础层"),
 "layer_fn": ("Fn layer", "Fn katmanı", "Fn 层"),
 "layer_base_help": ("What each key does when pressed on its own.", "Tuşa tek başına basıldığında ne yaptığı.", "单独按下按键时的功能。"),
 "layer_fn_help": ("What each key does while Fn is held. Keys marked ••• hold factory functions; keep them unless you are sure.",
                   "Fn basılıyken tuşun ne yaptığı. ••• ile işaretli tuşlarda fabrika işlevleri vardır; emin değilsen değiştirme.",
                   "按住 Fn 时按键的功能。标记为 ••• 的按键带有出厂功能，除非确定，否则请保留。"),
 "mapping_pending_title": ("%1$d pending change(s)", "%1$d bekleyen değişiklik", "%1$d 项待应用更改"),
 "mapping_pending_sub": ("Nothing is written until you apply.", "Uygulayana kadar hiçbir şey yazılmaz.", "在点击应用之前不会写入任何内容。"),
 "mapping_sheet_title": ("Assign key %1$s", "%1$s tuşuna ata", "设置按键 %1$s"),
 "mapping_current": ("Currently: %1$s", "Şu an: %1$s", "当前：%1$s"),
 "category_media": ("Media", "Medya", "媒体"),
 "category_keys": ("Keys", "Tuşlar", "按键"),
 "category_mouse": ("Mouse", "Fare", "鼠标"),
 "search_hint": ("Search", "Ara", "搜索"),
 "record_empty": ("(none)", "(boş)", "（无）"),
 "record_vendor": ("Factory function", "Fabrika işlevi", "出厂功能"),
 # settings
 "settings_language": ("Language", "Dil", "语言"),
 "language_system": ("System default", "Sistem varsayılanı", "跟随系统"),
 "settings_theme": ("Appearance", "Görünüm", "外观"),
 "theme_system": ("System", "Sistem", "系统"),
 "theme_light": ("Light", "Açık", "浅色"),
 "theme_dark": ("Dark", "Koyu", "深色"),
 "backups_title": ("Backups", "Yedekler", "备份"),
 "backups_sub": ("Taken automatically on connect and before every write. The original snapshot is never deleted.",
                 "Bağlanırken ve her yazmadan önce otomatik alınır. Orijinal yedek asla silinmez.",
                 "连接时及每次写入前自动创建。原始备份永不删除。"),
 "backups_empty": ("No backups yet.", "Henüz yedek yok.", "暂无备份。"),
 "backups_connect_hint": ("Connect the keyboard to restore a backup.", "Yedek geri yüklemek için klavyeyi bağla.", "连接键盘后即可恢复备份。"),
 "backup_original": ("Original settings", "Orijinal ayarlar", "原始设置"),
 "backup_connect": ("On connect", "Bağlantı anı", "连接时"),
 "backup_brightness": ("Before brightness change", "Parlaklık değişikliği öncesi", "亮度更改前"),
 "backup_lighting": ("Before lighting change", "Işık değişikliği öncesi", "灯效更改前"),
 "backup_sleep": ("Before sleep change", "Uyku değişikliği öncesi", "休眠更改前"),
 "backup_remap": ("Before key mapping", "Tuş ataması öncesi", "按键映射前"),
 "backup_perkey": ("Before key colors", "Tuş renkleri öncesi", "单键灯光前"),
 "backup_restore": ("Before restore", "Geri yükleme öncesi", "恢复前"),
 "backup_unknown": ("Backup", "Yedek", "备份"),
 "restore_dialog_title": ("Restore backup?", "Yedek geri yüklensin mi?", "恢复备份？"),
 "restore_dialog_body": ("Lighting, sleep, key colors and key mapping (except protected keys) return to “%1$s”. The current state is backed up first.",
                         "Işık, uyku, tuş renkleri ve tuş atamaları (korumalı tuşlar hariç) “%1$s” hâline döner. Önce mevcut durum yedeklenir.",
                         "灯效、休眠、单键灯光和按键映射（受保护按键除外）将恢复为“%1$s”。恢复前会先备份当前状态。"),
 "safety_title": ("Safety", "Güvenlik", "安全"),
 "safety_1": ("No firmware update, reset or erase command exists in this app.", "Bu uygulamada firmware güncelleme, sıfırlama veya silme komutu yok.", "本应用不包含任何固件升级、重置或擦除命令。"),
 "safety_2": ("Only the original F75 model identity is configured; anything else is refused.", "Yalnızca orijinal F75 model kimliği yapılandırılır; diğerleri reddedilir.", "仅对原版 F75 型号标识进行配置，其他设备一律拒绝。"),
 "safety_3": ("Before writing, the keyboard is read again; if it changed, nothing is written.", "Yazmadan önce klavye tekrar okunur; değişmişse hiçbir şey yazılmaz.", "写入前会再次读取键盘；如有变化则不写入。"),
 "safety_4": ("Every write is backed up first and verified byte-for-byte afterwards.", "Her yazmadan önce yedek alınır, sonra bayt bayt doğrulanır.", "每次写入前先备份，写入后逐字节校验。"),
 "safety_5": ("F1–F12 and Fn can never be remapped and no key can be disabled. Factory reset: usually hold Fn + Esc for 3–5 s (check your manual).",
              "F1–F12 ve Fn asla değiştirilemez, hiçbir tuş devre dışı bırakılamaz. Fabrika sıfırlama: genelde Fn + Esc'ye 3–5 sn basılı tut (kılavuzdan teyit et).",
              "F1–F12 和 Fn 永远不能被重新映射，也不能禁用任何按键。恢复出厂设置：通常长按 Fn + Esc 3–5 秒（请以说明书为准）。"),
 "usb_title": ("USB diagnostics", "USB teşhis", "USB 诊断"),
 "usb_sub": ("Lists every USB device the system can see.", "Sistemin gördüğü tüm USB cihazlarını listeler.", "列出系统可见的所有 USB 设备。"),
 "usb_none": ("No USB devices. The keyboard may only be charging: check the wired mode or restart the device.",
              "Hiç USB cihazı yok. Klavye sadece şarj oluyor olabilir: kablolu modu kontrol et veya cihazı yeniden başlat.",
              "没有 USB 设备。键盘可能只在充电：请检查有线模式或重启设备。"),
 "usb_kind_f75": ("Wired AULA F75", "Kablolu AULA F75", "有线 AULA F75"),
 "usb_kind_aula": ("AULA device, different product ID", "AULA cihazı, farklı ürün kimliği", "AULA 设备，产品 ID 不同"),
 "usb_kind_receiver": ("AULA 2.4 GHz receiver: configuration needs the cable", "AULA 2.4 GHz alıcı: ayar için kablo gerekir", "AULA 2.4 GHz 接收器：配置需使用有线连接"),
 "about_title": ("About", "Hakkında", "关于"),
 "about_version": ("Version %1$s", "Sürüm %1$s", "版本 %1$s"),
 "about_body": ("Android port of AULA Studio. The protocol is a faithful port of Dahuum/aula-studio (MIT), whose reverse-engineering work made this app possible.",
                "AULA Studio'nun Android portu. Protokol, tersine mühendislik çalışmasıyla bu uygulamayı mümkün kılan Dahuum/aula-studio (MIT) projesinden birebir aktarıldı.",
                "AULA Studio 的 Android 移植版。协议忠实移植自 Dahuum/aula-studio（MIT），正是其逆向工程工作使本应用成为可能。"),
 "about_disclaimer": ("Unofficial community software. Not affiliated with or endorsed by AULA.", "Resmî olmayan topluluk yazılımı. AULA ile bağlantılı değildir ve AULA tarafından onaylanmamıştır.", "非官方社区软件，与 AULA 无关联，也未获 AULA 认可。"),
 "about_source": ("Source code", "Kaynak kod", "源代码"),
 "about_upstream": ("aula-studio", "aula-studio", "aula-studio"),
 # errors
 "err_bad_reply": ("The keyboard sent an unexpected reply. Nothing was changed.", "Klavye beklenmeyen bir yanıt verdi. Hiçbir şey değiştirilmedi.", "键盘返回了意外的响应，未作任何更改。"),
 "err_bad_signature": ("A data signature from the keyboard was missing, so writing was blocked for safety.", "Klavyeden gelen verinin imzası eksikti; güvenlik için yazma engellendi.", "键盘数据缺少签名，为安全起见已阻止写入。"),
 "err_unknown_effect": ("The keyboard reports an unknown lighting effect.", "Klavye bilinmeyen bir ışık efekti bildiriyor.", "键盘报告了未知的灯效。"),
 "err_value_range": ("That value is not supported.", "Bu değer desteklenmiyor.", "不支持该数值。"),
 "err_effect_no_color": ("This effect has no adjustable color.", "Bu efektin rengi ayarlanamaz.", "此灯效无法调整颜色。"),
 "err_mode_no_brightness": ("The current mode has no brightness setting.", "Mevcut modun parlaklık ayarı yok.", "当前模式没有亮度设置。"),
 "err_protected_key": ("F1–F12 and Fn are protected and cannot be remapped.", "F1–F12 ve Fn korunur, değiştirilemez.", "F1–F12 和 Fn 受保护，无法重新映射。"),
 "err_unsupported_action": ("That action is not supported (keys cannot be disabled).", "Bu işlev desteklenmiyor (tuşlar devre dışı bırakılamaz).", "不支持该功能（按键不能被禁用）。"),
 "err_layout_unknown": ("The Fn key position was not recognised, so key mapping and key colors are disabled for safety.", "Fn tuşunun konumu tanınmadı; güvenlik için tuş atama ve tuş renkleri kapalı.", "无法识别 Fn 键位置，为安全起见已禁用按键映射和单键灯光。"),
 "err_stale": ("The keyboard changed since it was read. Nothing was written; it has been read again, please retry.", "Klavye okunduktan sonra değişmiş. Hiçbir şey yazılmadı; klavye yeniden okundu, tekrar dene.", "读取后键盘状态已改变。未写入任何内容；已重新读取，请重试。"),
 "err_write_interrupted": ("The write was interrupted. The keyboard may be partly changed: read it again and restore a backup if needed.", "Yazma yarıda kesildi. Klavye kısmen değişmiş olabilir: yeniden oku, gerekirse yedekten geri yükle.", "写入被中断，键盘可能已部分更改：请重新读取，必要时恢复备份。"),
 "err_readback": ("Verification failed: the keyboard did not read back as expected. A backup is available in Settings.", "Doğrulama başarısız: klavye beklendiği gibi geri okunmadı. Ayarlar'da bir yedek var.", "校验失败：回读结果与预期不符。可在设置中恢复备份。"),
 "err_backup_protected": ("This backup's protected keys differ from the keyboard (another revision?). Nothing was restored.", "Bu yedeğin korumalı tuşları klavyeden farklı (başka bir revizyon?). Hiçbir şey geri yüklenmedi.", "此备份的受保护按键与键盘不一致（可能是其他版本），未恢复任何内容。"),
 "err_backup_disables": ("This backup would disable a key, so it was not restored.", "Bu yedek bir tuşu devre dışı bırakırdı; geri yüklenmedi.", "此备份会禁用某个按键，因此未恢复。"),
 "err_backup_invalid": ("This backup file is not valid.", "Bu yedek dosyası geçerli değil.", "备份文件无效。"),
 "err_device_not_found": ("AULA F75 not found. Check that the keyboard is in wired mode and connected.", "AULA F75 bulunamadı. Klavyenin kablolu modda ve bağlı olduğunu kontrol et.", "未找到 AULA F75。请检查键盘是否处于有线模式并已连接。"),
 "err_no_usb": ("No USB device is visible. The keyboard may only be charging: set it to wired mode, or restart your phone/tablet if the USB port is stuck.",
                "Hiç USB cihazı görünmüyor. Klavye sadece şarj oluyor olabilir: kablolu moda al ya da USB portu takıldıysa telefonu/tableti yeniden başlat.",
                "看不到任何 USB 设备。键盘可能只在充电：请切换到有线模式，若 USB 端口无响应请重启手机/平板。"),
 "err_permission": ("USB permission was not granted.", "USB izni verilmedi.", "未授予 USB 权限。"),
 "err_not_connected": ("Connect the keyboard first.", "Önce klavyeyi bağla.", "请先连接键盘。"),
 "err_identity": ("This keyboard's model identity is not the original F75, so it is not configured for safety.", "Bu klavyenin model kimliği orijinal F75 değil; güvenlik için yapılandırılmıyor.", "此键盘的型号标识不是原版 F75，为安全起见不进行配置。"),
 "err_interface": ("The keyboard's configuration interface was not found. Is it in wired mode?", "Klavyenin yapılandırma arayüzü bulunamadı. Kablolu modda mı?", "未找到键盘的配置接口。是否处于有线模式？"),
 "err_open": ("Could not open the keyboard. Reconnect it and try again.", "Klavye açılamadı. Çıkarıp tekrar tak ve yeniden dene.", "无法打开键盘，请重新连接后重试。"),
 "err_transfer": ("USB communication failed. Reconnect the keyboard and try again.", "USB iletişimi başarısız. Klavyeyi çıkarıp tak ve tekrar dene.", "USB 通信失败，请重新连接键盘后重试。"),
}

ARRAYS = {
 "effect_names": (
   ["Off","Static","Breathing","Rainbow","Flash away","Raindrops","Color wheel","Ripple","Stars","Fade","Snake","Neon","Reaction","Sine wave","Scanning","Windmill","Waterfall","Blossom","Storm"],
   ["Kapalı","Sabit","Nefes","Gökkuşağı","Kayan iz","Yağmur damlası","Renk çarkı","Dalgalanma","Yıldızlar","Gölge","Yılan","Neon","Tepki","Sinüs dalgası","Tarama","Yel değirmeni","Şelale","Çiçeklenme","Fırtına"],
   ["关闭","常亮","呼吸","彩虹","流光","雨滴","色轮","涟漪","星光","渐隐","贪吃蛇","霓虹","触发响应","正弦波","扫描","风车","瀑布","绽放","风暴"]),
 "effect_descriptions": (
   ["All lighting off","One color on every key","A gentle pulse of light","The full spectrum flowing across","A trail sweeping across the board","Scattered drops of light","Colors rotating around the board","Waves spreading from each key press","A quiet field of twinkling lights","Soft color transitions","A winding trail of light","Shifting neon accents","Keys light up as you type","A flowing sine wave","A sweeping band of light","A rotating windmill pattern","Colors falling like a waterfall","Color blossoming outward","An energetic swirling pattern"],
   ["Tüm ışıklar kapalı","Her tuş aynı renkte, sabit","Yumuşak bir nabız gibi yanıp söner","Tüm renk yelpazesi akar","Klavye boyunca kayan bir iz","Dağınık ışık damlaları","Klavye etrafında dönen renkler","Bastığın tuştan dalga yayılır","Sakin, yıldız gibi parlayan noktalar","Yumuşak renk geçişleri","Kıvrılarak ilerleyen ışık izi","Değişen neon renk vurguları","Yazdıkça tuşlar yanar","Akan bir sinüs dalgası","Süpüren bir ışık bandı","Dönen bir yel değirmeni deseni","Şelale gibi akan renkler","Merkezden dışa açılan renk","Enerjik, dönen bir desen"],
   ["关闭所有灯光","所有按键同一颜色常亮","柔和的呼吸灯效","完整光谱流动","光带横扫键盘","零散的光点雨滴","颜色围绕键盘旋转","每次按键扩散涟漪","安静闪烁的星光","柔和的颜色过渡","蜿蜒前进的光迹","变换的霓虹色彩","打字时按键亮起","流动的正弦波","来回扫描的光带","旋转的风车图案","如瀑布般落下的颜色","由中心向外绽放的颜色","充满活力的旋涡图案"]),
 "media_names": (
   ["Play / pause","Stop","Previous track","Next track","Volume up","Volume down","Mute","Media player","Browser home","Calculator","Email","File explorer","Screen brightness up","Screen brightness down"],
   ["Oynat / duraklat","Durdur","Önceki parça","Sonraki parça","Ses +","Ses −","Sessiz","Medya oynatıcı","Tarayıcı ana sayfa","Hesap makinesi","E-posta","Dosya gezgini","Ekran parlaklığı +","Ekran parlaklığı −"],
   ["播放/暂停","停止","上一曲","下一曲","音量+","音量−","静音","媒体播放器","浏览器主页","计算器","电子邮件","文件管理器","屏幕亮度+","屏幕亮度−"]),
 "mouse_names": (
   ["Left click","Right click","Middle click","Back (button 4)","Forward (button 5)"],
   ["Sol tık","Sağ tık","Orta tık","Geri (4. tuş)","İleri (5. tuş)"],
   ["左键","右键","中键","后退（侧键 4）","前进（侧键 5）"]),
}

NON_TRANSLATABLE = {
 "url_source": "https://github.com/uckankeskinkaya/Aula-Studio-Android",
 "url_upstream": "https://github.com/Dahuum/aula-studio",
}

def esc(s):
    s = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "\\'").replace('"', '\\"')
    if s.startswith("@") or s.startswith("?"): s = "\\" + s
    return s

def write(folder, idx, include_nt):
    lines = ['<?xml version="1.0" encoding="utf-8"?>', '<!-- Generated by tools/strings.py. Edit that file, not this one. -->', "<resources>"]
    for k, v in S.items():
        if k == "app_name" and idx > 0: continue
        lines.append(f'    <string name="{k}">{esc(v[idx])}</string>')
    if include_nt:
        for k, v in NON_TRANSLATABLE.items():
            lines.append(f'    <string name="{k}" translatable="false">{esc(v)}</string>')
    for k, v in ARRAYS.items():
        lines.append(f'    <string-array name="{k}">')
        lines += [f"        <item>{esc(x)}</item>" for x in v[idx]]
        lines.append("    </string-array>")
    lines.append("</resources>")
    path = os.path.join("app/src/main/res", folder, "strings.xml")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "w", encoding="utf-8").write("\n".join(lines) + "\n")

for k, v in ARRAYS.items():
    n = {len(x) for x in v}
    assert len(n) == 1, k
write("values", 0, True)
write("values-tr", 1, False)
write("values-zh-rCN", 2, False)
print(f"{len(S)} strings, {len(ARRAYS)} arrays, 3 languages")
