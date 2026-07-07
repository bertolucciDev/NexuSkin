package NexuSkin;

public final class TextureLocator {
    public static Object locate(String modId, String path) {
        return locateInternal(modId, path, "_skin.png");
    }

    public static Object locateCape(String modId, String path) {
        return locateInternal(modId, path, "_cape.png");
    }

    private static Object locateInternal(String modId, String path, String suffix) {
        try {
            Class<?> resLocClass = Class.forName("net.minecraft.util.ResourceLocation");
            Object rl = resLocClass.getConstructor(String.class, String.class)
                    .newInstance(modId, path);
            Object texObj = loadTexture(modId, path, suffix);
            if (texObj != null) {
                Object tm = getTexManager();
                if (tm != null) bindTexture(tm, rl, texObj);
            }
            return rl;
        } catch (Throwable t) {
            return null;
        }
    }

    static Object getTexManagerPublic() { return getTexManager(); }

    private static Object getTexManager() {
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            Object mc = mcClass.getMethod("getMinecraft").invoke(null);
            return mcClass.getMethod("getTextureManager").invoke(mc);
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object loadTexture(String modId, String path, String suffix) {
        try {
            String user = NxAgent.getUser();
            if (user == null) return null;
            java.io.File file = new java.io.File(NxAgent.getCacheDir(),
                    user.toLowerCase() + suffix);
            if (!file.exists()) return null;

            Class<?> threadImgClass = Class.forName("net.minecraft.client.renderer.ThreadDownloadImageData");
            Class<?> callbackCls = Class.forName("java.util.function.BiConsumer");
            Object data = threadImgClass.getConstructor(
                    java.io.File.class, java.net.URL.class, callbackCls, Runnable.class)
                    .newInstance(file, null, null, null);
            return data;
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static void bindTexture(Object tm, Object rl, Object texObj) {
        try {
            Class<?> iTexture = Class.forName("net.minecraft.client.renderer.texture.ITextureObject");
            tm.getClass().getMethod("loadTexture",
                    Class.forName("net.minecraft.util.ResourceLocation"), iTexture)
                    .invoke(tm, rl, texObj);
        } catch (Throwable ignored) { }
    }
}
