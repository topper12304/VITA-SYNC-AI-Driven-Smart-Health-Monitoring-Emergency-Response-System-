package com.vitasync.api;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.stream.Collectors;

/**
 * Exposes VITA-SYNC data via a RESTful API.
 */
public class VitaSyncApiServer {
    private HttpServer server;
    private final int PORT = 8081;

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            
            // Endpoint: System Status
            server.createContext("/api/status", exchange -> {
                String response = "{\"status\": \"UP\", \"message\": \"VITA-SYNC System is monitoring...\"}";
                sendJsonResponse(exchange, response);
            });

            // Endpoint: Critical Alerts List
            server.createContext("/api/alerts", exchange -> {
                String jsonAlerts = AlertStore.getInstance().getAllAlerts().stream()
                        .map(s -> "\"" + s + "\"")
                        .collect(Collectors.joining(",", "[", "]"));
                sendJsonResponse(exchange, jsonAlerts);
            });

            server.setExecutor(null); 
            server.start();
            System.out.println(">>> API Server started on http://localhost:" + PORT + "/api/status");
            
        } catch (IOException e) {
            System.err.println("Failed to start API Server: " + e.getMessage());
        }
    }

    private void sendJsonResponse(HttpExchange exchange, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); // Allow browser access
        byte[] bytes = response.getBytes();
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public void stop() {
        if (server != null) server.stop(0);
    }
}