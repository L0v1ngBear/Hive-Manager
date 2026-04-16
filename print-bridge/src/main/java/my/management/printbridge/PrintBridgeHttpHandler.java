package my.management.printbridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import my.management.printbridge.model.BridgeResponse;
import my.management.printbridge.model.PrinterInfo;
import my.management.printbridge.model.RawPrintRequest;
import my.management.printbridge.service.PrinterBridgeService;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class PrintBridgeHttpHandler implements HttpHandler {

    private final ObjectMapper objectMapper;
    private final PrinterBridgeService printerBridgeService;
    private final PrintBridgeRoute route;

    public PrintBridgeHttpHandler(ObjectMapper objectMapper, PrinterBridgeService printerBridgeService, PrintBridgeRoute route) {
        this.objectMapper = objectMapper;
        this.printerBridgeService = printerBridgeService;
        this.route = route;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        applyCors(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            switch (route) {
                case HEALTH -> handleHealth(exchange);
                case PRINTERS -> handlePrinters(exchange);
                case RAW_PRINT -> handleRawPrint(exchange);
                default -> writeJson(exchange, 404, BridgeResponse.fail("未找到接口"));
            }
        } catch (Exception exception) {
            writeJson(exchange, 500, BridgeResponse.fail(exception.getMessage() == null ? "本地打印桥接异常" : exception.getMessage()));
        } finally {
            exchange.close();
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        Map<String, Object> data = Map.of(
                "service", "print-bridge",
                "version", "0.0.1",
                "defaultPrinter", printerBridgeService.getDefaultPrinterName()
        );
        writeJson(exchange, 200, BridgeResponse.success(data));
    }

    private void handlePrinters(HttpExchange exchange) throws IOException {
        ensureMethod(exchange, "GET");
        List<PrinterInfo> printers = printerBridgeService.listPrinters();
        writeJson(exchange, 200, BridgeResponse.success(printers));
    }

    private void handleRawPrint(HttpExchange exchange) throws IOException {
        ensureMethod(exchange, "POST");
        RawPrintRequest request = objectMapper.readValue(exchange.getRequestBody(), RawPrintRequest.class);
        PrinterInfo printer = printerBridgeService.printRaw(request);
        writeJson(exchange, 200, BridgeResponse.success(Map.of(
                "printerName", printer.getName(),
                "defaultPrinter", printer.isDefaultPrinter()
        )));
    }

    private void writeJson(HttpExchange exchange, int statusCode, BridgeResponse response) throws IOException {
        byte[] body = objectMapper.writeValueAsBytes(response);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }

    private void ensureMethod(HttpExchange exchange, String method) {
        if (!method.equalsIgnoreCase(exchange.getRequestMethod())) {
            throw new IllegalArgumentException("请求方法不支持");
        }
    }

    private void applyCors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
    }
}
