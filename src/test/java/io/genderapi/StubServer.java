package io.genderapi;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/** Local HTTP stub standing in for the API. Never contacts the real service. */
final class StubServer implements AutoCloseable {

    static final class Recorded {
        final String method;
        final String path;
        final String query;
        final Headers headers;
        final String body;

        Recorded(String method, String path, String query, Headers headers, String body) {
            this.method = method;
            this.path = path;
            this.query = query;
            this.headers = headers;
            this.body = body;
        }

        String header(String name) {
            return headers.getFirst(name);
        }
    }

    static final class Reply {
        int status = 200;
        String contentType = "application/json";
        String body = "{}";
        long delayMillis;
        final Map<String, String> headers = new LinkedHashMap<>();

        static Reply json(int status, String body) {
            Reply reply = new Reply();
            reply.status = status;
            reply.body = body;
            if (status >= 400) {
                reply.contentType = "application/problem+json";
            }
            return reply;
        }

        Reply header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        Reply contentType(String value) {
            contentType = value;
            return this;
        }

        Reply delay(long millis) {
            delayMillis = millis;
            return this;
        }
    }

    private final HttpServer server;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final List<Recorded> requests = Collections.synchronizedList(new ArrayList<>());
    private volatile Function<Recorded, Reply> responder = r -> Reply.json(200, "{}");

    StubServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        server.setExecutor(executor);
        server.start();
    }

    StubServer respond(Function<Recorded, Reply> responder) {
        this.responder = responder;
        return this;
    }

    StubServer respond(Reply reply) {
        return respond(r -> reply);
    }

    String origin() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    String baseUrl() {
        return origin() + "/api/v2";
    }

    List<Recorded> requests() {
        synchronized (requests) {
            return new ArrayList<>(requests);
        }
    }

    Recorded only() {
        List<Recorded> all = requests();
        if (all.size() != 1) {
            throw new AssertionError("expected exactly one request but got " + all.size());
        }
        return all.get(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body;
        try (InputStream in = exchange.getRequestBody()) {
            body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Headers copy = new Headers();
        copy.putAll(exchange.getRequestHeaders());
        Recorded recorded = new Recorded(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath(),
                exchange.getRequestURI().getRawQuery(), copy, body);
        requests.add(recorded);
        Reply reply = responder.apply(recorded);
        if (reply.delayMillis > 0) {
            try {
                Thread.sleep(reply.delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                exchange.close();
                return;
            }
        }
        byte[] bytes = reply.body.getBytes(StandardCharsets.UTF_8);
        if (reply.contentType != null) {
            exchange.getResponseHeaders().set("Content-Type", reply.contentType);
        }
        reply.headers.forEach((k, v) -> exchange.getResponseHeaders().set(k, v));
        try {
            exchange.sendResponseHeaders(reply.status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
        } catch (IOException ignored) {
            // client went away (e.g. timeout tests)
        } finally {
            exchange.close();
        }
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
