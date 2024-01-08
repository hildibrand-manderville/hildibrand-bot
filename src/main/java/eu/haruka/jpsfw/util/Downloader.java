package eu.haruka.jpsfw.util;

import eu.haruka.jpsfw.logging.GlobalLogger;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class Downloader {

    private Proxy proxy;
    private String url;
    private H[] headers;
    private String method;
    private byte[] data;
    private boolean debugheaders;
    private String encoding;
    private boolean ignoreSslErrors;

    public Downloader(String url, H... headers) {
        this.url = url;
        this.headers = headers;
    }

    public Downloader(String url, Proxy p, H... headers) {
        this(url, headers);
        this.proxy = p;
    }

    public static class H {
        public String key;
        public String value;

        public H(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public void setData(String method, byte[] data) {
        this.method = method;
        this.data = data;
    }

    public void setIgnoreSslErrors(boolean ignoreSslErrors) {
        this.ignoreSslErrors = ignoreSslErrors;
    }

    public void downloadToFile(File file) throws IOException {
        byte[] data = downloadToBytes();
        Etc.writeFileBinary(file, data);
    }

    public String downloadToString() throws IOException {
        byte[] data = downloadToBytes();
        if (encoding != null) {
            return new String(data, Charset.forName(encoding));
        } else {
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    public void setPrintDebugHeaders(boolean b) {
        this.debugheaders = b;
    }

    public byte[] downloadToBytes() throws IOException {

        GlobalLogger.info("Downloader: Fetching file: " + url);
        URL urlobj = new URL(url);
        HttpURLConnection connection;
        if (proxy != null) {
            connection = (HttpURLConnection) urlobj.openConnection(proxy);
        } else {
            connection = (HttpURLConnection) urlobj.openConnection();
        }
        if (connection instanceof HttpsURLConnection) {
            HttpsURLConnection sconnection = (HttpsURLConnection) connection;
            if (ignoreSslErrors) {
                try {
                    TrustModifier.relaxHostChecking(sconnection);
                } catch (Exception ex) {
                    throw new IOException(ex);
                }
            }
        }
        connection.setRequestMethod(method != null ? method : "GET");
        for (H h : headers) {
            connection.setRequestProperty(h.key, h.value);
        }

        connection.setUseCaches(false);
        connection.setDoOutput(data != null);
        connection.setAllowUserInteraction(false);
        connection.setConnectTimeout(3000);

        if (data != null) {
            connection.getOutputStream().write(data);
        }

        InputStream is = connection.getInputStream();

        if (debugheaders) {
            for (Map.Entry<String, List<String>> e : connection.getHeaderFields().entrySet()) {
                System.out.println(e.getKey() + ": " + e.getValue());
            }
        }
        encoding = connection.getContentEncoding();

        int size = connection.getContentLength();

        byte[] buf;
        if (size > -1) {
            buf = new byte[size];

            int read = is.read(buf);
            if (read != buf.length && read > 0) {
                while (read < buf.length) {
                    int ret = is.read(buf, read, buf.length - read);
                    if (ret > -1) {
                        read += ret;
                    } else {
                        break;
                    }
                }
            } else if (read != size) {
                throw new IOException("Read " + read + "/" + size + " bytes only!");
            }
        } else {
            byte[] tmp = new byte[1024 * 100];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int read;
            while ((read = is.read(tmp)) >= 0) {
                bos.write(tmp, 0, read);
            }
            buf = bos.toByteArray();
        }

        return buf;

    }
}
