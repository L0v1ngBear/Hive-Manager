package my.hive.domain.print.receipt.service;

import my.hive.domain.print.receipt.model.vo.OutboundPrintDetailVO;
import my.hive.domain.print.receipt.model.vo.OutboundPrintItemVO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ReceiptPrinterCommandUtil {

    public static final Charset PRINT_CHARSET = Charset.forName("GB18030");

    private ReceiptPrinterCommandUtil() {
    }

    public static byte[] buildTriplicateCommand(OutboundPrintDetailVO detail) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            writeBytes(outputStream, 0x1B, 0x40);
            writeLine(outputStream, "            产品出库单");
            writeLine(outputStream, "----------------------------------------");
            writeLine(outputStream, "单号: " + safe(detail.getOrderNo()));
            writeLine(outputStream, "客户: " + safe(detail.getCustomerName()));
            writeLine(outputStream, "日期: " + formatDate(detail.getCreateTime() == null ? null : detail.getCreateTime().toString()));
            writeLine(outputStream, "经办: " + safe(detail.getOperator()));
            writeLine(outputStream, "----------------------------------------");
            writeLine(outputStream, pad("序", 4) + pad("型号", 12) + pad("规格", 8) + pad("米数", 6) + pad("金额", 10));
            writeLine(outputStream, "----------------------------------------");

            List<OutboundPrintItemVO> items = detail.getItems();
            for (int index = 0; index < items.size(); index++) {
                OutboundPrintItemVO item = items.get(index);
                writeLine(outputStream,
                        pad(String.valueOf(index + 1), 4)
                                + pad(safe(item.getModelCode()), 12)
                                + pad(item.getSpec() == null ? "--" : stripTrailingZero(item.getSpec()), 8)
                                + pad(stripTrailingZero(item.getMeters()), 6)
                                + pad(money(item.getTotalAmount()), 10));
                writeLine(outputStream, "    条码: " + safe(item.getBarcode()));
            }

            writeLine(outputStream, "----------------------------------------");
            writeLine(outputStream, "合计米数: " + stripTrailingZero(detail.getTotalMeters()));
            writeLine(outputStream, "合计金额: " + money(detail.getTotalAmount()));
            writeLine(outputStream, "制单人: " + safe(detail.getOperator()));
            writeLine(outputStream, "库管员: __________  送货人: __________");
            writeLine(outputStream, "签收人: ______________________________");
            writeLine(outputStream, "");
            writeLine(outputStream, "第一联:存根  第二联:客户  第三联:财务");
            writeBytes(outputStream, 0x0C);

            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("生成打印指令失败", exception);
        }
    }

    private static void writeLine(ByteArrayOutputStream outputStream, String text) throws IOException {
        outputStream.write(text.getBytes(PRINT_CHARSET));
        outputStream.write("\r\n".getBytes(StandardCharsets.US_ASCII));
    }

    private static void writeBytes(ByteArrayOutputStream outputStream, int... bytes) {
        for (int value : bytes) {
            outputStream.write(value);
        }
    }

    private static String formatDate(String text) {
        if (text == null || text.isBlank()) {
            return "--";
        }
        return text.replace('T', ' ').substring(0, Math.min(text.length(), 16));
    }

    private static String safe(String text) {
        return text == null || text.isBlank() ? "--" : text;
    }

    private static String stripTrailingZero(Float value) {
        if (value == null) {
            return "0";
        }
        BigDecimal decimal = BigDecimal.valueOf(value.doubleValue()).stripTrailingZeros();
        return decimal.toPlainString();
    }

    private static String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String pad(String text, int size) {
        String value = text == null ? "" : text;
        if (value.length() >= size) {
            return value.substring(0, size);
        }
        return value + " ".repeat(size - value.length());
    }
}
