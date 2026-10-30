package dolly.chat;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;

/** Bounded image reads, decoded off the game thread. No automatic redirects. */
public final class LinkImage {
    static final int MAX_BYTES = 4 * 1024 * 1024;
    public static URI compatible(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme(), path = uri.getPath();
            if (scheme == null || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))
                    || uri.getHost() == null || uri.getUserInfo() != null || value.length() > 2048
                    || uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443 || path == null) return null;
            return path.toLowerCase(Locale.ROOT).matches(".*\\.(png|jpe?g|gif)") ? uri : null;
        } catch (URISyntaxException e) { return null; }
    }
    public static BufferedImage fetch(URI uri) throws IOException {
        for (int redirects = 0; redirects <= 3; redirects++) {
            if (compatible(uri.toString()) == null) throw new IOException("Link incompatível");
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || address.isMulticastAddress()
                        || (address.getAddress().length == 16 && (address.getAddress()[0] & 0xfe) == 0xfc))
                    throw new IOException("Endereço local indisponível para prévia");
            }
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setInstanceFollowRedirects(false); connection.setConnectTimeout(3000); connection.setReadTimeout(3000);
            connection.setRequestProperty("User-Agent", "Dolly-ImagePreview/1");
            try {
                int code = connection.getResponseCode();
                if (code >= 300 && code < 400) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("Redirecionamento inválido");
                    uri = uri.resolve(location); continue;
                }
                if (code != 200 || connection.getContentLengthLong() > MAX_BYTES) throw new IOException("Imagem indisponível ou muito grande");
                try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    byte[] chunk = new byte[8192]; int count;
                    while ((count = input.read(chunk)) != -1) {
                        if (output.size() + count > MAX_BYTES) throw new IOException("Imagem excede 4 MiB");
                        output.write(chunk, 0, count);
                    }
                    return decode(output.toByteArray());
                }
            } finally { connection.disconnect(); }
        }
        throw new IOException("Redirecionamentos demais");
    }
    public static BufferedImage decode(byte[] bytes) throws IOException {
        if (bytes.length > MAX_BYTES) throw new IOException("Imagem excede 4 MiB");
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Formato de imagem inválido");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > 16_000_000L) throw new IOException("Dimensões de imagem excessivas");
                BufferedImage original = reader.read(0);
                double scale = Math.min(1d, Math.min(320d / width, 180d / height));
                BufferedImage result = new BufferedImage(Math.max(1, (int) (width * scale)), Math.max(1, (int) (height * scale)), BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = result.createGraphics();
                try { graphics.drawImage(original, 0, 0, result.getWidth(), result.getHeight(), null); }
                finally { graphics.dispose(); original.flush(); }
                return result;
            } finally { reader.dispose(); }
        }
    }
}
