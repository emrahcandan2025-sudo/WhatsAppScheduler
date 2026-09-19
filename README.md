# WhatsApp Zamanlayıcı (Kişisel Kullanım)

Kendi Android telefonunda, kendi WhatsApp numaranla mesaj zamanlamak için basit bir uygulama.

## Nasıl çalışır?
1. Uygulamada numara + mesaj + tarih/saat girip **Zamanla**'ya basarsın.
2. Belirlenen saatte `AlarmManager` tetiklenir, WhatsApp'ı ilgili sohbeti ve mesajı hazır şekilde açar (`wa.me` linki ile).
3. Eğer **Erişilebilirlik izni** açıksa, `WhatsAppAutoSendService` WhatsApp ekranındaki "gönder" butonunu otomatik bulup tıklar — mesaj tamamen elden geçmeden gider.
4. İzin kapalıysa mesaj sadece hazır açılır, göndermek için sen dokunursun.

## Kurulum
1. [Android Studio](https://developer.android.com/studio) indir, "Open an existing project" ile bu klasörü aç.
2. Gradle senkronizasyonunun bitmesini bekle (ilk açılışta internet ister; Kotlin/Gradle eklentilerini indirir).
3. Telefonunu USB ile bağla (Geliştirici Seçenekleri > USB Hata Ayıklama açık olsun) veya bir emülatör kullan.
4. **Run ▶** ile uygulamayı telefonuna kur.

## Telefonda ilk çalıştırma
1. Uygulamayı aç, çıkan izin ekranında "Tam zamanlı alarm" iznini ver (Android 12+ bunu ister).
2. **"Erişilebilirlik İznini Aç"** butonuna bas, açılan sistem ayarlarında **WhatsApp Zamanlayıcı**'yı bulup aç. (Bu izni Android güvenlik gereği uygulama kendi kendine açamaz, elle onaylaman gerekir.)
3. Ana ekrandan numara (örnek: `905551234567` — başında + veya 0 olmadan, ülke koduyla), mesaj, tarih ve saat gir, **Zamanla**'ya bas.
4. Listede mesajın "Bekliyor" olarak göründüğünü görürsün. Silmek istersen **Sil**'e bas.

## Önemli sınırlamalar ve uyarılar
- **Bu, WhatsApp'ın resmi bir özelliği değil.** Uygulama, WhatsApp ekranını senin yerine "tıklayarak" kullanıyor (Erişilebilirlik servisi ile). WhatsApp bir güncelleme yaparsa gönder butonunun teknik kimliği (`resource-id`) değişebilir; bu durumda otomatik tıklama çalışmaz ama mesaj yine hazır açılır, elle gönderebilirsin. Böyle bir durumda `WhatsAppAutoSendService.kt` içindeki `candidateSendIds` listesine yeni id'yi eklemen yeterli olur.
- Telefon zamanlanan saatte **kapalıysa veya WhatsApp'tan çıkış yapılmışsa** mesaj o an gönderilemez; telefon açıldığında ıskalanan alarm tetiklenmeyebilir (Android'in pil optimizasyonu bazı üreticilerde arka plan alarmlarını kısıtlayabilir — Ayarlar > Pil > "Kısıtlama yok" verirsen daha güvenilir çalışır).
- Sadece kendi hesabın ve kendi telefonun için tasarlandı; başka birinin hesabında veya WhatsApp Business API üzerinden toplu/otomatik mesajlaşma için **kullanılmamalıdır** — bu, WhatsApp'ın hizmet şartlarını ihlal edebilir ve hesabının kısıtlanmasına yol açabilir.
- Uygulama Play Store'a değil, sadece kendi cihazına (APK olarak) kurmak için tasarlandı. Erişilebilirlik servisi kullanan uygulamalar Play Store incelemesinden genelde geçemez.

## Dosya yapısı
- `MainActivity.kt` — arayüz ve zamanlama mantığı
- `AlarmScheduler.kt` — AlarmManager kurulum/iptal
- `AlarmReceiver.kt` — zamanı gelince WhatsApp'ı açar
- `BootReceiver.kt` — telefon yeniden başlayınca alarmları geri kurar
- `WhatsAppAutoSendService.kt` — gönder butonuna otomatik basan Erişilebilirlik servisi
- `MessageStorage.kt` — mesajları telefonda saklar (SharedPreferences + JSON)
