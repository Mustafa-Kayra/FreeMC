package dev.freemc;

import java.util.LinkedHashMap;
import java.util.Map;

/** Tiny two-language table so the interface can run in Turkish or English. */
public final class Lang {

    private static final Map<String, String[]> TABLE = new LinkedHashMap<>();
    private static boolean english;

    static {
        row("app.tagline", "offline Minecraft launcher", "offline Minecraft launcher");
        row("nav.dashboard", "Panel", "Dashboard");
        row("nav.library", "Kütüphane", "Library");
        row("nav.profile", "Profil", "Profile");
        row("nav.settings", "Ayarlar", "Settings");
        row("nav.navigate", "Gezinme", "Navigate");
        row("header.search", "Mod, mod paketi veya profil ara", "Search mods, modpacks or profiles");
        row("header.ready", "Başlatmaya hazır", "Ready to launch");
        row("header.working", "Çalışıyor", "Working");
        row("header.online", "Çevrimiçi", "Online");
        row("dash.active", "Aktif profil", "Active profile");
        row("dash.play", "OYNA", "PLAY NOW");
        row("dash.mods", "mod", "mods");
        row("dash.mods.none", "mod yok", "no mods");
        row("dash.recent", "Son profiller", "Recent profiles");
        row("dash.new", "Yeni profil", "New profile");
        row("dash.stats", "Kütüphane durumu", "Library status");
        row("dash.profiles", "profil", "profiles");
        row("dash.hours", "saat oynandı", "hours played");
        row("dash.welcome", "Başlamak için bir profil oluştur", "Create a profile to get started");
        row("dash.create", "Profil oluştur", "Create profile");
        row("updates.available", "MOD GÜNCELLEMESİ VAR", "MOD UPDATE AVAILABLE");
        row("updates.body", "adlı modun yeni sürümü yayında.", "has a newer release on Modrinth.");
        row("updates.apply", "Güncelle", "Update");
        row("updates.dismiss", "Yoksay", "Dismiss");
        row("crash.title", "SON ÇÖKME RAPORU", "LAST CRASH REPORT");
        row("crash.detail", "Ayrıntı", "Details");
        row("lib.modpacks", "Mod paketleri", "Modpacks");
        row("lib.mods", "Modlar", "Mods");
        row("lib.installed", "Kurulu", "Installed");
        row("lib.search", "Ara", "Search");
        row("lib.search.ph", "Modrinth'te ara — örnek: sodium, jei, create", "Search Modrinth — e.g. sodium, jei, create");
        row("lib.install", "Kur", "Install");
        row("lib.installed.tag", "Kuruldu", "Installed");
        row("lib.empty", "Arama sonucu yok.", "No results.");
        row("lib.need.modded", "Modlar yalnızca Fabric, Forge veya NeoForge profilinde yüklenir.",
                "Mods only load in a Fabric, Forge or NeoForge profile.");
        row("lib.need.profile", "Önce Panel'den bir profil seç.", "Pick a profile on the dashboard first.");
        row("lib.downloads", "indirme", "downloads");
        row("lib.back", "Geri", "Back");
        row("lib.detail.about", "Açıklama", "About");
        row("lib.detail.versions", "Sürümler", "Versions");
        row("lib.detail.added", "Profilde yüklü", "Installed in profile");
        row("lib.detail.add", "Profile kur", "Install to profile");
        row("lib.detail.open", "Modrinth'te aç", "Open on Modrinth");
        row("lib.installedmods", "Kurulu modlar", "Installed mods");
        row("lib.addfile", "Dosyadan ekle", "Add file");
        row("lib.refresh", "Yenile", "Refresh");
        row("lib.remove", "Sil", "Remove");
        row("lib.checkupdates", "Güncellemeleri denetle", "Check updates");
        row("prof.title", "Kullanıcı", "Profile");
        row("prof.name", "Kullanıcı adı", "Username");
        row("prof.uuid", "Offline UUID", "Offline UUID");
        row("prof.save", "Kaydet", "Save");
        row("prof.export", "Profili dışa aktar", "Export profile");
        row("prof.stats", "İstatistik", "Statistics");
        row("prof.hours", "toplam saat", "total hours");
        row("prof.mods", "kurulu mod", "installed mods");
        row("prof.packs", "mod paketi", "modpacks");
        row("prof.recent", "Son oynanan", "Recently played");
        row("set.title", "Ayarlar", "Settings");
        row("set.language", "Dil", "Language");
        row("set.theme", "Tema", "Theme");
        row("set.dark", "Koyu", "Dark");
        row("set.light", "Açık", "Light");
        row("set.memory", "Bellek ayırma", "Memory allocation");
        row("set.memory.hint", "Minecraft'ın kullanabileceği en yüksek RAM.", "Maximum heap the game may use.");
        row("set.jvm", "Ek JVM argümanları", "Extra JVM arguments");
        row("set.server", "Hızlı katılma sunucusu", "Quick-connect server");
        row("set.server.hint", "Oyun açılınca doğrudan bu sunucuya bağlanır (host:port).",
                "Joins this server right after launch (host:port).");
        row("set.java", "Java", "Java");
        row("set.java.auto", "Gerekirse otomatik indir", "Download automatically when needed");
        row("set.data", "Veri klasörü", "Data folder");
        row("set.open", "Aç", "Open");
        row("set.save", "Kaydet", "Save");
        row("set.saved", "Kaydedildi", "Saved");
        row("status.install", "Kuruluyor", "Installing");
        row("status.launch", "Başlatılıyor", "Launching");
        row("status.running", "Oyun çalışıyor", "Game running");
        row("status.closed", "Oyun kapandı", "Game closed");
        row("status.error", "Hata", "Error");
        row("common.close", "Kapat", "Close");
        row("common.clear", "Temizle", "Clear");
        row("common.console", "Konsol", "Console");
        row("common.cancel", "İptal", "Cancel");
        row("common.delete", "Sil", "Delete");
        row("common.ok", "Tamam", "OK");
    }

    private Lang() {
    }

    private static void row(String key, String turkish, String englishText) {
        TABLE.put(key, new String[]{turkish, englishText});
    }

    public static void english(boolean value) {
        english = value;
    }

    public static boolean isEnglish() {
        return english;
    }

    public static String t(String key) {
        String[] values = TABLE.get(key);
        if (values == null) {
            return key;
        }
        return english ? values[1] : values[0];
    }
}
