package my.management.printbridge.model;

public class BridgeResponse {

    private Integer code;

    private String msg;

    private Object data;

    public static BridgeResponse success(Object data) {
        BridgeResponse response = new BridgeResponse();
        response.setCode(200);
        response.setMsg("success");
        response.setData(data);
        return response;
    }

    public static BridgeResponse fail(String message) {
        BridgeResponse response = new BridgeResponse();
        response.setCode(500);
        response.setMsg(message);
        response.setData(null);
        return response;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
