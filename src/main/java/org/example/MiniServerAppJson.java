package org.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MiniServerAppJson {

    public static void main(String[] args) throws IOException {
        final int port = 8000;
        final int backlog = 0;

        HttpServer httpSrv = HttpServer.create(new InetSocketAddress(port), backlog);

        // Root endpoint with HTML response
        httpSrv.createContext("/", exchange -> {
            String body = Stream.of(
                    "<h1>Yo! Mini Java server up and running.</h1>",
                    "<p>Try the <a href='/greet'>/greet</a> endpoint for JSON output.</p>"
            ).collect(Collectors.joining(System.lineSeparator()));

            sendResponse(exchange, 200, "text/html; charset=utf-8", body);
        });

        // Greet endpoint with JSON response
        httpSrv.createContext("/greet", MiniServerAppJson::handleGreet);

        ExecutorService pool = Executors.newCachedThreadPool();
        httpSrv.setExecutor(pool);
        httpSrv.start();

        System.out.println("MiniServerApp listening on http://localhost:" + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server...");
            httpSrv.stop(1);
            pool.shutdownNow();
        }));
    }

    private static void handleGreet(HttpExchange exchange) throws IOException {
        // Create a small JSON manually (since no external libs like Gson/Jackson)
        Map<String, String> data = Map.of(
                "message", "Hello there!",
                "description", "Welcome to a tiny JSON-powered corner of the web.",
                "author", "MiniServerApp"
        );

        String json = data.entrySet().stream()
                .map(e -> String.format("\"%s\": \"%s\"", e.getKey(), e.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));

        sendResponse(exchange, 200, "application/json; charset=utf-8", json);
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
            os.flush();
        } finally {
            exchange.close();
        }
    }
}
