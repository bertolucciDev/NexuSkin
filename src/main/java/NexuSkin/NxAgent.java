package NexuSkin;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class NxAgent {

    private static volatile String USER;
    private static volatile File CACHE_DIR;
    private static final ScheduledExecutorService POOL =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "NexuSkin-Resolver");
                t.setDaemon(true);
                return t;
            });

    public static void premain(String args, Instrumentation ins) {
        if (args == null || args.isEmpty()) {
            System.err.println("[NexuSkin] no args; agent inactive");
            return;
        }
        Map<String, String> map = parseArgs(args);
        USER = map.getOrDefault("user", "");
        String cachePath = map.getOrDefault("cache", "nexuskin_cache");
        if (!USER.isEmpty()) {
            CACHE_DIR = new File(cachePath);
            CACHE_DIR.mkdirs();
        }
        System.out.println("[NexuSkin] agent active. user=" + USER + " cache=" + CACHE_DIR);

        ins.addTransformer((ClassFileTransformer) NxAgent::transform, true);
        POOL.scheduleAtFixedRate(NxAgent::resolveSkinInBackground, 2, 60, TimeUnit.SECONDS);
    }

    private static Map<String, String> parseArgs(String s) {
        Map<String, String> m = new HashMap<>();
        for (String kv : s.split(",")) {
            int i = kv.indexOf('=');
            if (i > 0) m.put(kv.substring(0, i).trim(), kv.substring(i + 1).trim());
        }
        return m;
    }

    private static byte[] transform(ClassLoader loader, String className,
                                   Class<?> classBeingRedefined,
                                   ProtectionDomain pd, byte[] bytes) {
        if (className == null || USER == null || USER.isEmpty()) return null;
        if (className.endsWith(".AbstractClientPlayer")) {
            return SkinPatcher.patchPlayer(bytes);
        }
        if (className.endsWith(".NetworkPlayerInfo")) {
            return SkinPatcher.patchNetworkPlayerInfo(bytes);
        }
        return null;
    }

    private static void resolveSkinInBackground() {
        if (USER.isEmpty() || CACHE_DIR == null) return;
        File skin = new File(CACHE_DIR, USER.toLowerCase() + "_skin.png");
        File cape = new File(CACHE_DIR, USER.toLowerCase() + "_cape.png");
        if (!skin.exists()) {
            try { Http.get("https://crafatar.com/skins/" + uuid(), skin); }
            catch (Exception e) { /* silent */ }
        }
        if (!cape.exists()) {
            try { Http.get("https://crafatar.com/capes/" + uuid(), cape); }
            catch (Exception ignored) { }
        }
    }

    private static String uuid() {
        try {
            java.net.URL u = new java.net.URL("https://api.mojang.com/users/profiles/minecraft/" + USER);
            java.net.HttpURLConnection c = (java.net.HttpURLConnection) u.openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            if (c.getResponseCode() != 200) return offlineUuid();
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(c.getInputStream()))) {
                String id = null;
                String line;
                while ((line = br.readLine()) != null) {
                    int i = line.indexOf("\"id\":\"");
                    if (i >= 0) {
                        int s = i + 6;
                        int e = line.indexOf('"', s);
                        id = line.substring(s, e);
                        break;
                    }
                }
                if (id == null) return offlineUuid();
                return id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16)
                     + "-" + id.substring(16, 20) + "-" + id.substring(20, 32);
            }
        } catch (Exception e) { return offlineUuid(); }
    }

    private static String offlineUuid() {
        return java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + USER).getBytes()).toString();
    }

    public static File getCacheDir() { return CACHE_DIR; }
    public static String getUser()   { return USER; }
}
