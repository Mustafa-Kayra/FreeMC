# FreeMC

Windows ve Linux'ta çalışan, **offline (crack) modda** gerçek Minecraft başlatan launcher.
Simülasyon değil: oyun dosyalarını Mojang'ın resmî sunucularından indirir, doğrular ve
vanilla Minecraft istemcisini ayağa kaldırır. Hesap, Microsoft girişi veya premium gerekmez —
bir kullanıcı adı yeter.

```
FreeMC
├─ launcher (Swing arayüz)   → sürüm seç, ad yaz, OYNA
├─ indirici                  → version.json, client.jar, kütüphaneler, natives, assetler
└─ başlatıcı                 → doğru java komutunu kurup oyunu çalıştırır
```

## Ne yapıyor

- **Sürüm listesi**: Mojang'ın `version_manifest_v2.json` dosyasından alınır, 30 dakika önbelleklenir.
- **Kurulum**: seçilen sürümün `version.json`, client jar, platforma uygun kütüphaneler
  (Windows / Linux / macOS kuralları), natives ve ~3.600 asset nesnesi indirilir; SHA-1 ile doğrulanır.
- **Eski sürüm desteği**: 1.7 ve öncesi için `virtual` / `map_to_resources` asset düzeni,
  `minecraftArguments` şablonu ve sınıflandırıcılı (classifier) natives jar'ları desteklenir.
- **Offline kimlik**: `OfflinePlayer:<ad>` üzerinden v3 UUID üretir — vanilla'nın offline modda
  kullandığı UUID'nin aynısı. Tek oyunculu, LAN ve offline sunucular bu kimliği kabul eder.
- **Başlatma**: sürüm dosyasındaki JVM argümanları, `-Djava.library.path`, classpath ve oyun
  argümanları şablonlardan üretilir; sürümün istediği Java sürümü kurulu değilse daha yenisini arar.
- **Konsol**: oyunun tüm çıktısı launcher penceresindeki günlük alanına akar.

## Derleme

JDK 17+ gerekir (yeni sürümler — 1.20.5 ve sonrası — Java 21 ister).

**Linux / macOS**

```bash
./build.sh          # dist/freemc.jar
./run.sh            # arayüzü açar
```

**Windows**

```bat
build.bat
run.bat
```

Elle derlemek istersen:

```bash
javac -encoding UTF-8 -d build/classes $(find src/main/java -name '*.java')
jar --create --file dist/freemc.jar --main-class dev.freemc.Main -C build/classes .
```

## Kullanım

1. `run.sh` / `run.bat` (veya `java -jar dist/freemc.jar`)
2. Sürüm seç (varsayılan: en yeni release), kullanıcı adını yaz, belleği ayarla.
3. **OYNA**. İlk seferde dosyalar iner (sürüme göre 200–500 MB), sonraki açılışlar hızlıdır.

Sürüm listesi ilk açılışta internet ister; sonrasında önbellekten çalışır.

## Komut satırı

Arayüz olmadan da kullanılabilir — sunucu, test veya otomasyon için:

```bash
java -jar dist/freemc.jar --cli --version 1.20.1 --user Kayra --ram 4096          # kur + komutu yaz
java -jar dist/freemc.jar --cli --version 1.20.1 --user Kayra --ram 4096 --launch # kur + başlat
java -jar dist/freemc.jar --cli --list-versions                                   # sürümleri listele
```

Kurulan sürümün tam java komutu `<veri klasörü>/last-command.txt` dosyasına yazılır.

## Dosya düzeni

| Platform | Veri klasörü |
|---|---|
| Linux | `~/.freemc` |
| Windows | `%APPDATA%\FreeMC` |

```
<veri klasörü>/
├─ versions/<sürüm>/<sürüm>.json         sürüm tanımı
├─ versions/<sürüm>/<sürüm>.jar          client jar
├─ versions/<sürüm>/natives/             çıkarılmış yerel kütüphaneler
├─ libraries/                            maven düzeninde kütüphaneler
├─ assets/indexes, assets/objects        varlıklar (hash'e göre)
├─ game/                                 oyun klasörü: dünyalar, ayarlar, modlar
└─ freemc.properties                     kullanıcı adı, RAM, son sürüm
```

## Notlar

- Oyun dosyaları indirilir, yeniden dağıtılmaz; launcher yalnızca resmî CDN adreslerini kullanır:
  `piston-meta.mojang.com`, `libraries.minecraft.net`, `resources.download.minecraft.net`.
- Aynı hash'e sahip dosya tekrar indirilmez; yarım kalan indirmeler `.part` olarak yazılıp yeniden denenir.
- Mod yükleyicileri (Fabric/Forge) bu sürümde yok; vanilla sürümler desteklenir.
- Linux'ta oyun açılmazsa OpenGL 3.2+ sürücüsü gerektiğini unutma (`mesa` / ekran kartı sürücüsü).
