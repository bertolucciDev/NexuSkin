package NexuSkin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

final class Http {
    static void get(String urlStr, File dest) throws Exception {
        dest.getParentFile().mkdirs();
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent", "NexuSkin/1.0");
        c.connect();
        if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
        File tmp = new File(dest.getAbsolutePath() + ".part");
        try (InputStream in = c.getInputStream();
             FileOutputStream out = new FileOutputStream(tmp)) {
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
        }
        if (dest.exists()) dest.delete();
        tmp.renameTo(dest);
    }
}
