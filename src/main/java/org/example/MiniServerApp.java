package org.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MiniServerApp {

    public static void main(String[] args) throws IOException {
        final int port = 8000;
        final int backlog = 0;

        HttpServer httpSrv = HttpServer.create(new InetSocketAddress(port), backlog);

        // Root context using a lambda handler
        httpSrv.createContext("/", exchange -> {
            String body = Stream.of(
                    "<h1>Yo! Mini Java server up and running.</h1>",
                    "<p>Try the <a href='/greet'>/greet</a> endpoint.</p>"
            ).collect(Collectors.joining(System.lineSeparator()));

            sendHtml(exchange, 200, body);
        });

        // Greet context, implemented as a method reference-style lambda
        httpSrv.createContext("/greet", MiniServerApp::handleGreet);

        // use a small thread pool for request handling
        ExecutorService pool = Executors.newCachedThreadPool();
        httpSrv.setExecutor(pool);

        httpSrv.start();
        System.out.println("MiniServerApp listening on http://localhost:" + port);

        // add a shutdown hook to cleanly stop the server and thread pool
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server...");
            httpSrv.stop(1);
            pool.shutdownNow();
        }));
    }

    // handler method for /greet
    private static void handleGreet(HttpExchange exchange) throws IOException {
        String body = Stream.of(
                "<h2>Hello there!</h2>",
                "<p>Welcome to a tiny corner of the web served by Java.</p>"
        ).collect(Collectors.joining());

        sendHtml(exchange, 200, body);
    }

    // small helper to set headers, write bytes and close stream safely
    private static void sendHtml(HttpExchange exchange, int statusCode, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
            os.flush();
        } finally {
            exchange.close();
        }
    }
}
