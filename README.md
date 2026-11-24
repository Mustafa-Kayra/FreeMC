# FreeMC

Windows ve Linux'ta çalışan, **offline (crack) modda** gerçek Minecraft başlatan launcher.
Simülasyon değil: oyun dosyalarını Mojang'ın resmî sunucularından indirir, doğrular ve
vanilla Minecraft istemcisini ayağa kaldırır. Hesap, Microsoft girişi veya premium gerekmez —
bir kullanıcı adı yeter. Üstüne **Fabric mod yükleyici** ve **tek tıkla Modrinth mod kurulumu** var.

```
FreeMC
├─ arayüz      Oyna / Modlar sekmeleri, canlı konsol
├─ indirici    version.json, client.jar, kütüphaneler, natives, assetler
├─ yükleyici   Fabric profili → inheritsFrom ile vanilla sürümün üstüne bindirilir
└─ modlar      Modrinth arama, bağımlılıklarla kurulum, elle jar ekleme/silme
```

## Ne yapıyor

- **Sürüm listesi**: Mojang'ın `version_manifest_v2.json` dosyasından, 30 dakika önbellekli.
- **Kurulum**: `version.json`, client jar, platforma uygun kütüphaneler, natives ve ~3.600 asset
  nesnesi indirilir; SHA-1 ile doğrulanır, yarım kalan indirmeler yeniden denenir.
- **Fabric**: `meta.fabricmc.net` üzerinden en yeni stabil loader seçilir, profili indirilir ve
  `inheritsFrom` zinciri vanilla sürümün üstüne bindirilir (kütüphaneler, JVM argümanları ve
  asset indeksi ebeveynden miras alınır).
- **Modrinth**: arama sürüm + yükleyici ile filtrelenir; tek tıkla en uygun dosya ve **zorunlu
  bağımlılıkları** indirilir (`jei` → `mezz_config` + `fabric-api`). Elle jar ekleme ve silme de var.
- **Eski sürümler**: 1.7 öncesi `virtual` / `map_to_resources` asset düzeni, `minecraftArguments`
  şablonu, classifier'lı natives jar'ları.
- **Offline kimlik**: `OfflinePlayer:<ad>` üzerinden v3 UUID — vanilla'nın offline modda kullandığı
  UUID'nin aynısı; tek oyunculu, LAN ve offline sunucular bu kimliği kabul eder.
- **Java seçimi**: sürümün istediği Java aranır; 1.16 ve öncesi için kurulu Java 8 tercih edilir.
  Eski LWJGL'nin istediği `libjawt`/`jawt.dll` için JDK'nın kendi lib dizini yola eklenir.

## Derleme

JDK 17+ gerekir (1.20.5 ve sonrası Java 21, çok yeni sürümler Java 25 ister).

**Linux / macOS**

```bash
./build.sh          # dist/freemc.jar
./run.sh
```

**Windows**

```bat
build.bat
run.bat
```

## Kullanım

1. **Oyna** sekmesi: Minecraft sürümünü, yükleyiciyi (Vanilla / Fabric), kullanıcı adını ve belleği seç.
2. **OYNA** — ilk seferde dosyalar iner (sürüme göre 200–500 MB), sonraki açılışlar hızlıdır.
3. **Modlar** sekmesi: sürüm Fabric ise Modrinth'te ara, **Kur**'a bas. Modlar
   `<veri>/instances/<sürüm>-fabric/mods` altına iner; sağdaki listeden silebilir veya
   "Dosyadan ekle" ile kendi jar'ını koyabilirsin.

Her sürüm/yükleyici kombinasyonu kendi oyun klasörünü kullanır: dünyalar, ayarlar ve modlar
birbirine karışmaz.

## Komut satırı

```bash
java -jar dist/freemc.jar --cli --version 1.20.1 --user Kayra --ram 4096                    # kur
java -jar dist/freemc.jar --cli --version 1.20.1 --user Kayra --ram 4096 --launch           # kur + başlat
java -jar dist/freemc.jar --cli --version 1.20.1 --loader fabric --mod jei --user Kayra     # Fabric + JEI
java -jar dist/freemc.jar --cli --list-versions                                             # sürümleri listele
```

Üretilen tam java komutu `<veri klasörü>/last-command.txt` dosyasına yazılır.

## Dosya düzeni

| Platform | Veri klasörü |
|---|---|
| Linux | `~/.freemc` |
| Windows | `%APPDATA%\FreeMC` |

```
<veri klasörü>/
├─ versions/<sürüm>/<sürüm>.json      sürüm tanımı (Fabric profili dahil)
├─ versions/<sürüm>/<sürüm>.jar       client jar
├─ versions/<sürüm>/natives/          çıkarılmış yerel kütüphaneler
├─ libraries/                         maven düzeninde kütüphaneler
├─ assets/indexes, assets/objects     varlıklar (hash'e göre)
├─ instances/<sürüm>[-fabric]/        oyun klasörü: dünyalar, ayarlar, options.txt
│   └─ mods/                          mod jar'ları
└─ freemc.properties                  kullanıcı adı, RAM, son sürüm ve yükleyici
```

## Notlar

- Oyun dosyaları indirilir, yeniden dağıtılmaz; yalnızca resmî adresler kullanılır:
  `piston-meta.mojang.com`, `libraries.minecraft.net`, `resources.download.minecraft.net`,
  `meta.fabricmc.net`, `api.modrinth.com`.
- Aynı hash'e sahip dosya tekrar indirilmez.
- Forge/NeoForge desteklenmiyor; mod yükleyici olarak Fabric var.
- Linux'ta oyun açılmazsa OpenGL 3.2+ sürücüsü gerekir (`mesa` / ekran kartı sürücüsü).
