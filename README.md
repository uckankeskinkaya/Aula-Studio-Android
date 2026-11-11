# AULA F75 – Android

Bilgisayar olmadan, telefondan **AULA F75** klavyeyi ayarlamak için Android uygulaması (Kotlin + Jetpack Compose).

Protokol, [Dahuum/aula-studio](https://github.com/Dahuum/aula-studio) (MIT lisanslı) projesindeki tersine mühendislik çalışmasından birebir Kotlin'e aktarıldı. Güvenlik kuralları da aynı şekilde korundu.

## Kurulum (telefondan)

1. Bu deponun **Releases** sayfasını aç ve en yeni `AULA F75 Android v0.1.x` sürümündeki `AULA-F75-v0.1.x.apk` dosyasını indir.
2. İndirilen dosyaya dokun. Telefon "bilinmeyen kaynaklardan yükleme" izni isterse, tarayıcıya veya dosya yöneticisine bu izni ver.
3. Yeni sürümler aynı imzayla derleniyor, bu yüzden eski sürümün üstüne kurulur. Uygulamayı kaldırman gerekmez.

## Kullanım

1. Klavyeyi **USB-C kablo + OTG adaptörle** telefona tak ve klavyeyi **kablolu moda** al. 2.4G alıcı veya Bluetooth üzerinden ayar yapılamaz.
2. Telefon "AULA F75 açılsın mı?" diye sorarsa **Tamam** de. Sormazsa uygulamayı aç ve **Bağlan**'a dokun, sonra USB iznini ver.
3. Uygulama klavyenin model kimliğini kontrol eder, tüm ayarları okur ve **ilk yedeği** alır.
4. Sekmeler:
   - **Durum:** bağlantı, mevcut ayarlar, deneysel özellik anahtarı
   - **Işık:** parlaklık (her zaman açık), efekt/renk/hız ve uyku süresi (deneysel)
   - **Tuş RGB:** her tuşa ayrı renk (deneysel)
   - **Tuş Atama:** ana katman ve Fn katmanında tuşlara klavye, medya veya fare işlevi atama (deneysel)
   - **Yedekler:** geri yükleme ve paylaşma
5. İşin bitince kabloyu çıkar. Ayarlar klavyenin kendi hafızasında kalır.

## Güvenlik: neler olabilir, neler olamaz?

- Uygulamada **firmware güncelleme, bootloader, sıfırlama veya silme komutu yok**. Klavyeye sadece normal ayar komutları (ışık, renk, uyku, tuş haritası) gönderilir.
- Ayar yapılmadan önce model kimliği doğrulanır. Sadece orijinal ANSI F75 (`03 00 00 00 00 CD`) kabul edilir, başka bir cihaza hiçbir şey yazılmaz.
- **Her yazmadan önce** klavye yeniden okunur. Ekrandaki durumla aynı değilse hiçbir şey yazılmaz.
- Her yazmadan önce otomatik yedek alınır. Yazmadan sonra klavye geri okunup bayt bayt doğrulanır.
- Sadece değişen baytlar değişir. Bilinmeyen alanlar olduğu gibi korunur.
- **Fn ve F1–F12 tuşları değiştirilemez**, hiçbir tuş devre dışı bırakılamaz. Böylece klavyenin Fn kombinasyonları her zaman çalışır.
- Fn tuşu ANSI konumunda bulunmazsa (farklı revizyon veya ISO düzen) tuş atama ve tuş RGB tamamen kapanır.

**Deneysel ne demek?** Kaynak projede gerçek klavyede test edilmiş tek yazma işlemi parlaklık değiştirme. Okumalar da test edilmiş. Efekt, renk, uyku, tuş RGB ve tuş atama yazmaları tersine mühendislikle çıkarıldı ama donanımda tam doğrulanmadı. Bu yüzden ayrı bir anahtarın arkasındalar. En kötü ihtimal ışıkların veya tuşların yanlış ayarlanması olur. Bunu geri almanın iki yolu var:

1. **Yedekler → "0000-ilk-yedek" → Geri yükle.** Bu yedek klavyeyi uygulamaya ilk taktığındaki hâlidir ve silinmez.
2. Klavyenin fabrika sıfırlaması. F75'te genelde **Fn + Esc'ye 3–5 saniye basılı tutmak**, kılavuzdan teyit et.

Uygulamayı kaldırırsan yedekler de silinir. Önemli yedekleri **Paylaş** ile kendine gönderebilirsin.

## Sorun giderme

| Durum | Ne yapmalı |
|---|---|
| "AULA F75 bulunamadı" | OTG adaptörünü ve kabloyu kontrol et, klavyeyi kablolu moda al. Bazı telefonlarda ayarlardan OTG'yi açman gerekir. |
| "Model kimliği eşleşmiyor" | Klavyen farklı bir model veya revizyon olabilir. Uygulama güvenlik için hiçbir şey yazmaz. Ekrandaki kimliği bana ilet. |
| "Arayüz 1 alınamadı" | Klavyeyi çıkarıp tak, uygulamayı yeniden aç. |
| Bağlantıdan sonra medya tuşları çalışmıyor | Uygulama yapılandırma arayüzünü kullanırken bu normal. Kabloyu çıkarıp takınca düzelir. |
| "Geri okuma eşleşmedi" | Yedekler sekmesinden son "once-…" yedeğini geri yükle ve hatayı bildir. |

## Geliştirici notları

- `app/src/main/java/com/aulaf75/android/protocol/`: saf Kotlin protokol (Android'e bağımlı değil)
- `KeyboardController.kt`: okuma, yedek, yazma ve geri okuma işlem sözleşmesi
- `usb/F75Usb.kt`: `UsbDeviceConnection.controlTransfer` ile HID `SET_REPORT`/`GET_REPORT` (Feature, ID 6, 520 bayt, arayüz 1). Okumada 20 ms, yazmada 70 ms bekleme yapılır.
- Testler: `./gradlew testReleaseUnitTest`. Protokol testleri ve sahte F75 cihazıyla işlem testleri burada.
- APK: GitHub Actions (`.github/workflows/aula-f75-android.yml`) her push'ta derler ve Release olarak yayınlar.
- `app/aula-f75-sideload.jks`: sadece kişisel yan yükleme için sabit imza anahtarı. Bilerek depoda tutuluyor, Play Store için kullanma.

Lisans: protokol bilgisi aula-studio'dan alındı (MIT, © AULA Studio contributors).
