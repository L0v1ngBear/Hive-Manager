package my.management.printbridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import my.management.printbridge.service.PrinterBridgeService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class PrintBridgeApplication {

    public static final String HOST = "127.0.0.1";
    public static final int PORT = 13528;

    public static void main(String[] args) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        PrinterBridgeService printerBridgeService = new PrinterBridgeService();

        HttpServer server = HttpServer.create(new InetSocketAddress(HOST, PORT), 0);
        server.createContext("/health", new PrintBridgeHttpHandler(objectMapper, printerBridgeService, PrintBridgeRoute.HEALTH));
        server.createContext("/printers", new PrintBridgeHttpHandler(objectMapper, printerBridgeService, PrintBridgeRoute.PRINTERS));
        server.createContext("/print/raw", new PrintBridgeHttpHandler(objectMapper, printerBridgeService, PrintBridgeRoute.RAW_PRINT));
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        System.out.printf("本地打印桥接已启动: http://%s:%d%n", HOST, PORT);
        System.out.println("按 Ctrl+C 可停止服务。");
    }
}
