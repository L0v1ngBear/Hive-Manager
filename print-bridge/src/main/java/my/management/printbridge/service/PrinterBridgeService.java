package my.management.printbridge.service;

import my.management.printbridge.model.PrinterInfo;
import my.management.printbridge.model.RawPrintRequest;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

public class PrinterBridgeService {

    public List<PrinterInfo> listPrinters() {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();

        List<PrinterInfo> printers = new ArrayList<>();
        for (PrintService printService : printServices) {
            printers.add(new PrinterInfo(
                    printService.getName(),
                    defaultService != null && defaultService.getName().equals(printService.getName())
            ));
        }
        printers.sort(Comparator.comparing(PrinterInfo::getName, String.CASE_INSENSITIVE_ORDER));
        return printers;
    }

    public String getDefaultPrinterName() {
        PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
        return defaultService == null ? "" : defaultService.getName();
    }

    public PrinterInfo printRaw(RawPrintRequest request) {
        validateRequest(request);

        PrintService printService = choosePrintService(request.getPrinterName());
        byte[] bytes = Base64.getDecoder().decode(request.getBase64Content());

        try {
            DocPrintJob printJob = printService.createPrintJob();
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(bytes, flavor, null);
            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            printJob.print(doc, attributes);
            return new PrinterInfo(
                    printService.getName(),
                    printService.getName().equalsIgnoreCase(getDefaultPrinterName())
            );
        } catch (PrintException exception) {
            throw new IllegalStateException("发送打印任务失败: " + exception.getMessage(), exception);
        }
    }

    private void validateRequest(RawPrintRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (request.getBase64Content() == null || request.getBase64Content().isBlank()) {
            throw new IllegalArgumentException("打印内容不能为空");
        }
    }

    private PrintService choosePrintService(String printerName) {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        if (printServices.length == 0) {
            throw new IllegalStateException("未检测到本机打印机");
        }

        if (printerName != null && !printerName.isBlank()) {
            for (PrintService printService : printServices) {
                if (printService.getName().equalsIgnoreCase(printerName.trim())) {
                    return printService;
                }
            }
            throw new IllegalStateException("未找到指定打印机: " + printerName);
        }

        PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
        if (defaultService != null) {
            return defaultService;
        }
        return printServices[0];
    }
}
