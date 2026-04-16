package my.management.printbridge.model;

public class PrinterInfo {

    private String name;

    private boolean defaultPrinter;

    public PrinterInfo() {
    }

    public PrinterInfo(String name, boolean defaultPrinter) {
        this.name = name;
        this.defaultPrinter = defaultPrinter;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDefaultPrinter() {
        return defaultPrinter;
    }

    public void setDefaultPrinter(boolean defaultPrinter) {
        this.defaultPrinter = defaultPrinter;
    }
}
