# FreeMC

Windows, Linux ve macOS'ta çalışan **offline (crack) Minecraft launcher'ı**. Oyun dosyalarını
Mojang'ın resmî sunucularından indirip doğrular, vanilla istemciyi başlatır. Hesap gerekmez.
Üstüne mod yükleyiciler, Modrinth entegrasyonu, mod paketleri ve otomatik Java kurulumu var.

```
FreeMC
├─ arayüz      Panel · Kütüphane · Profil · Ayarlar, canlı konsol
├─ çekirdek    sürüm indirme, inheritsFrom birleştirme, komut üretimi
├─ yükleyici   Fabric (meta API) · Forge / NeoForge (kendi installer'ı)
├─ modlar      Modrinth arama, tek tıkla kurulum, bağımlılıklar, güncelleme
└─ mod paketi  .mrpack indirme, overrides kopyalama, profil oluşturma
```

## Özellikler

**Oyun**
- Mojang `version_manifest_v2.json` → sürüm dosyası → client jar → platforma uygun kütüphaneler →
  natives → asset nesneleri; paralel indirme, SHA-1 doğrulama, yeniden deneme.
- `inheritsFrom` desteği: Fabric/Forge profilleri vanilla ebeveynin üstüne bindirilir; client jar
  profilin kendi adıyla kopyalanır (Forge'un modül ignore-list'i buna bağlı).
- Offline kimlik: `OfflinePlayer:<ad>` v3 UUID, `userType=legacy`.
- Eski sürümler: `minecraftArguments`, classifier natives, `virtual`/`map_to_resources` asset düzeni.

**Yükleyiciler**
- **Fabric**: en yeni stabil loader, meta API'den profil, `inheritsFrom` birleştirme.
- **Forge / NeoForge**: resmî installer headless çalıştırılır (`--installClient`), ürettiği sürüm
  dosyası aynı boru hattından geçer. Gerekli `launcher_profiles.json` otomatik yazılır.

**Java**
- Kurulu JVM'ler taranır; sürümün istediği Java yoksa **Adoptium'dan uygun JRE indirilir**,
  `~/.freemc/jvm/jre-<sürüm>` altına çıkarılır ve o kullanılır. Eski sürümler için kurulu Java 8
  tercih edilir; `libjawt` için JDK'nın lib dizini yola eklenir.

**Modlar ve paketler**
- Modrinth araması seçili sürüm + yükleyici ile filtrelenir; tek tıkla kurulum **zorunlu
  bağımlılıkları** da indirir.
- Kurulu modlar listesi, elle jar ekleme, silme ve **güncelleme denetimi** (`version_files/update`,
  hash karşılaştırmalı).
- `.mrpack` mod paketleri: `modrinth.index.json` okunur, tüm dosyalar indirilir, `overrides` ve
  `client-overrides` kopyalanır, paket kendi profili olarak açılır.

**Diğer**
- Hızlı katılma: profilde `host:port` varsa oyun doğrudan o sunucuya bağlanır.
- Çökme analizi: `crash-reports/` içindeki en yeni rapor Panel'de özetlenir.
- Profil yöneticisi: her profil kendi oyun klasörü, RAM'i, JVM argümanları ve modlarıyla.
- Arayüz: koyu/açık tema, Türkçe/İngilizce, kendi çizilmiş ikonlar, gömülü Archivo Narrow + Geist.
- `jpackage` ile kurulabilir paket (`package.sh` / `package.bat`), GitHub Actions matrisi
  (Ubuntu, Windows, macOS).

## Derleme

JDK 17+ gerekir (uygulama Java 21 ile derlenir ve çalışır).

```bash
./build.sh      # dist/freemc.jar
./run.sh        # arayüz

./package.sh    # dist/package/FreeMC  (jpackage app-image)
```

Windows: `build.bat`, `run.bat`, `package.bat`.

## Kullanım

1. **Panel** → profil yoksa "Yeni profil", varsa doğrudan **OYNA**.
2. **Kütüphane** → *Mod paketleri* veya *Modlar* sekmesi, ara, **Kur**.
3. **Profil** → kullanıcı adı (offline UUID otomatik türetilir).
4. **Ayarlar** → dil, tema, bellek, JVM argümanları, hızlı katılma sunucusu.

## Komut satırı

```bash
java -jar freemc.jar --cli --version 1.20.1 --user Kayra                 # kur
java -jar freemc.jar --cli --version 1.20.1 --loader fabric --mod jei    # Fabric + JEI
java -jar freemc.jar --cli --version 1.20.1 --loader forge --launch      # Forge kur + başlat
java -jar freemc.jar --cli --pack fabulously-optimized                   # mod paketi kur
java -jar freemc.jar --cli --list-versions
java -jar freemc.jar --cli --help
```

## Dosya düzeni

| Platform | Veri klasörü |
|---|---|
| Linux | `~/.freemc` |
| Windows | `%APPDATA%\FreeMC` |
| macOS | `~/.freemc` |

```
<veri klasörü>/
├─ versions/<sürüm>/            sürüm json, client jar, natives
├─ libraries/                   maven düzeninde kütüphaneler
├─ assets/                      asset indeksleri ve nesneleri
├─ jvm/jre-<sürüm>/             otomatik indirilen Java
├─ profiles/<profil>/           profile.json, mods/, dünyalar, ayarlar
├─ installers/                  Forge/NeoForge installer jar'ları
└─ freemc.properties            kullanıcı adı, aktif profil, dil, tema
```

## Notlar

- Oyun dosyaları yalnızca resmî kaynaklardan indirilir: `piston-meta.mojang.com`,
  `libraries.minecraft.net`, `resources.download.minecraft.net`, `meta.fabricmc.net`,
  `maven.minecraftforge.net`, `maven.neoforged.net`, `api.adoptium.net`, `api.modrinth.com`.
- Gömülü yazı tipleri Archivo Narrow ve Geist, SIL Open Font License ile gelir.
- Forge/NeoForge kurulumu resmî installer'ı çalıştırdığı için ilk seferde birkaç dakika sürebilir.
- Linux'ta oyun açılmazsa OpenGL 3.2+ sürücüsü gerekir.
