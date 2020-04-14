package response;

public class Response {
    final static public String STATUS_FAIL = "FAIL";
    final static public String STATUS_SUCCESS = "SUCCESS";
    protected String status;
    protected String message;

    public Response(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public Response(String status) {
        this.status = status;
        this.message = "";
    }

}
