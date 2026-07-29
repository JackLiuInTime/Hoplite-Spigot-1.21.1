package com.hoplite.global.resourcepack;

import com.hoplite.HoplitePlugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Minimal embedded HTTP server for serving a local resource-pack file.
 */
public final class LocalResourcePackServer {

    private final HoplitePlugin plugin;
    private final Path filePath;
    private final String route;
    private final String publicUrl;
    private final ServerSocket serverSocket;
    private final Thread acceptThread;

    private volatile boolean running = true;

    private LocalResourcePackServer(
            HoplitePlugin plugin,
            Path filePath,
            String route,
            String publicUrl,
            ServerSocket serverSocket,
            Thread acceptThread
    ) {
        this.plugin = plugin;
        this.filePath = filePath;
        this.route = route;
        this.publicUrl = publicUrl;
        this.serverSocket = serverSocket;
        this.acceptThread = acceptThread;
    }

    public static LocalResourcePackServer start(
            HoplitePlugin plugin,
            String localFilePath,
            String bindHost,
            int port,
            String route,
            String publicHost,
            String publicScheme
    ) throws IOException {
        Path packPath = Path.of(localFilePath).toAbsolutePath().normalize();
        if (!Files.exists(packPath) || !Files.isRegularFile(packPath)) {
            throw new IOException("Local resource-pack file not found: " + packPath);
        }

        String normalizedRoute = normalizeRoute(route);
        String normalizedScheme = normalizeScheme(publicScheme);

        InetAddress bindAddress = bindHost == null || bindHost.isBlank()
                ? InetAddress.getByName("0.0.0.0")
                : InetAddress.getByName(bindHost.trim());

        ServerSocket socket = new ServerSocket();
        socket.bind(new InetSocketAddress(bindAddress, port));

        String safePublicHost = (publicHost == null || publicHost.isBlank())
                ? "127.0.0.1"
                : publicHost.trim();

        String url = normalizedScheme + "://" + safePublicHost + ":" + port + normalizedRoute;

        LocalResourcePackServer server = new LocalResourcePackServer(
                plugin,
                packPath,
                normalizedRoute,
                url,
                socket,
                new Thread(() -> serverLoop(plugin, socket, normalizedRoute, packPath), "hoplite-rp-http")
        );

        server.acceptThread.setDaemon(true);
        server.acceptThread.start();

        plugin.getLogger().info("Local resource-pack server started on " + bindAddress.getHostAddress() + ":" + port + " route=" + normalizedRoute);
        plugin.getLogger().info("Resource-pack public URL resolved to: " + url);

        return server;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void stop() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }
    }

    private static void serverLoop(HoplitePlugin plugin, ServerSocket socket, String route, Path filePath) {
        while (!socket.isClosed()) {
            try {
                Socket client = socket.accept();
                handleClient(plugin, client, route, filePath);
            } catch (IOException ex) {
                if (!socket.isClosed()) {
                    plugin.getLogger().warning("Local resource-pack server accept failed: " + ex.getMessage());
                }
            }
        }
    }

    private static void handleClient(HoplitePlugin plugin, Socket client, String route, Path filePath) {
        try (client) {
            client.setSoTimeout(5000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.US_ASCII));

            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isBlank()) {
                return;
            }

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                writeResponse(client.getOutputStream(), 400, "text/plain; charset=utf-8", "Bad Request".getBytes(StandardCharsets.UTF_8));
                return;
            }

            String method = parts[0].toUpperCase(Locale.ROOT);
            String path = parts[1];

            while (true) {
                String headerLine = reader.readLine();
                if (headerLine == null || headerLine.isEmpty()) {
                    break;
                }
            }

            if (!"GET".equals(method) && !"HEAD".equals(method)) {
                writeResponse(client.getOutputStream(), 405, "text/plain; charset=utf-8", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
                return;
            }

            String requestPath = path;
            int queryIndex = requestPath.indexOf('?');
            if (queryIndex >= 0) {
                requestPath = requestPath.substring(0, queryIndex);
            }

            if (!route.equals(requestPath)) {
                writeResponse(client.getOutputStream(), 404, "text/plain; charset=utf-8", "Not Found".getBytes(StandardCharsets.UTF_8));
                return;
            }

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                writeResponse(client.getOutputStream(), 404, "text/plain; charset=utf-8", "Resource pack file missing".getBytes(StandardCharsets.UTF_8));
                return;
            }

            byte[] body = Files.readAllBytes(filePath);
            if ("HEAD".equals(method)) {
                writeHeadOnly(client.getOutputStream(), 200, "application/zip", body.length);
                return;
            }

            writeResponse(client.getOutputStream(), 200, "application/zip", body);
        } catch (IOException ex) {
            plugin.getLogger().fine("Local resource-pack client handling failed: " + ex.getMessage());
        }
    }

    private static void writeHeadOnly(OutputStream out, int statusCode, String contentType, int length) throws IOException {
        String reason = reasonPhrase(statusCode);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.US_ASCII));
        writer.write("HTTP/1.1 " + statusCode + " " + reason + "\\r\\n");
        writer.write("Content-Type: " + contentType + "\\r\\n");
        writer.write("Content-Length: " + length + "\\r\\n");
        writer.write("Cache-Control: no-cache\\r\\n");
        writer.write("Connection: close\\r\\n");
        writer.write("\\r\\n");
        writer.flush();
    }

    private static void writeResponse(OutputStream out, int statusCode, String contentType, byte[] body) throws IOException {
        String reason = reasonPhrase(statusCode);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.US_ASCII));
        writer.write("HTTP/1.1 " + statusCode + " " + reason + "\\r\\n");
        writer.write("Content-Type: " + contentType + "\\r\\n");
        writer.write("Content-Length: " + body.length + "\\r\\n");
        writer.write("Cache-Control: no-cache\\r\\n");
        writer.write("Connection: close\\r\\n");
        writer.write("\\r\\n");
        writer.flush();
        out.write(body);
        out.flush();
    }

    private static String reasonPhrase(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            default -> "Error";
        };
    }

    private static String normalizeRoute(String route) {
        if (route == null || route.isBlank()) {
            return "/hoplite-pack.zip";
        }
        String normalized = route.trim();
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private static String normalizeScheme(String value) {
        if (value == null || value.isBlank()) {
            return "http";
        }
        String scheme = value.trim().toLowerCase(Locale.ROOT);
        return "https".equals(scheme) ? "https" : "http";
    }
}
