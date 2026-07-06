package NexuSkin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class NetworkSkinHook {

    private static final ScheduledExecutorService POOL =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "NexuSkin-Net");
                t.setDaemon(true);
                return t;
            });

    private static final ConcurrentHashMap<String, Object> CACHE_SKIN = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Object> CACHE_CAPE = new ConcurrentHashMap<>();

    public static Object resolveForNetwork(Object info) {
        try {
            String name = readUsername(info);
            if (name == null) return null;
            String key = name.toLowerCase();

            File f = new File(NxAgent.getCacheDir(), key + "_skin.png");
            if (!f.exists()) {
                UUID uuid = fetchOrOfflineUuid(name);
                Http.get("https://crafatar.com/skins/" + uuid.toString(), f);
            }
            return cacheOrReturn("skin", key, f);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object resolveCapeForNetwork(Object info) {
        try {
            String name = readUsername(info);
            if (name == null) return null;
            String key = name.toLowerCase();

            File f = new File(NxAgent.getCacheDir(), key + "_cape.png");
            if (!f.exists()) {
                UUID uuid;
                try { uuid = fetchOrOfflineUuid(name); }
                catch (Throwable e) { return null; }
                try { Http.get("https://crafatar.com/capes/" + uuid.toString(), f); }
                catch (Throwable e) { return null; }
            }
            Object rl = cacheOrReturn("cape", key, f);
            return rl;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object cacheOrReturn(String type, String key, File file) {
        try {
            if (!file.exists()) return null;
            ConcurrentHashMap<String, Object> map = "cape".equals(type) ? CACHE_CAPE : CACHE_SKIN;
            Object rl = map.get(key);
            if (rl != null) return rl;

            Class<?> resLocClass = Class.forName("net.minecraft.util.ResourceLocation");
            Object newRl = resLocClass.getConstructor(String.class, String.class)
                    .newInstance("nexuskin",
                            ("cape".equals(type) ? "capes/" : "skins/") + key + (type.equals("cape") ? "_cape" : "_skin"));
            map.put(key, newRl);

            POOL.submit(() -> preloadTexture(newRl, file));
            return newRl;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void preloadTexture(Object rl, File file) {
        try {
            Object tm = TextureLocator.getTexManagerPublic();
            if (tm == null) return;
            Class<?> threadImgClass = Class.forName("net.minecraft.client.renderer.ThreadDownloadImageData");
            Class<?> callbackCls = Class.forName("java.util.function.BiConsumer");
            Object data = threadImgClass.getConstructor(
                    File.class, java.net.URL.class, callbackCls, Runnable.class)
                    .newInstance(file, null, null, null);
            if (data == null) return;
            Class<?> iTexture = Class.forName("net.minecraft.client.renderer.texture.ITextureObject");
            tm.getClass().getMethod("loadTexture",
                    Class.forName("net.minecraft.util.ResourceLocation"), iTexture)
                    .invoke(tm, rl, data);
        } catch (Throwable ignored) { }
    }

    private static String readUsername(Object info) {
        try {
            for (String field : new String[] { "gameProfile", "field_178860_i", "profile" }) {
                try {
                    Field f = info.getClass().getDeclaredField(field);
                    f.setAccessible(true);
                    Object profile = f.get(info);
                    if (profile != null) {
                        try {
                            String name = (String) profile.getClass().getMethod("getName").invoke(profile);
                            if (name != null && !name.isEmpty()) return name;
                        } catch (Throwable ignored) { }
                        try {
                            Object idObj = profile.getClass().getMethod("getId").invoke(profile);
                            if (idObj != null) return idObj.toString();
                        } catch (Throwable ignored) { }
                    }
                } catch (Throwable ignored) { }
            }
        } catch (Throwable ignored) { }
        return null;
    }

    private static UUID fetchOrOfflineUuid(String name) throws Exception {
        try {
            java.net.URL u = new java.net.URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            java.net.HttpURLConnection c = (java.net.HttpURLConnection) u.openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            if (c.getResponseCode() == 200) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(c.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        int i = line.indexOf("\"id\":\"");
                        if (i >= 0) {
                            int s = i + 6;
                            int e = line.indexOf('"', s);
                            String id = line.substring(s, e);
                            return UUID.fromString(
                                id.substring(0,8)+"-"+id.substring(8,12)+"-"+id.substring(12,16)+"-"+id.substring(16,20)+"-"+id.substring(20,32));
                        }
                    }
                }
            }
        } catch (Throwable ignored) { }
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
    }
}
