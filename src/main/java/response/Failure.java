package response;

public class Failure extends Response {
    public Failure(String message) {
        super(Response.STATUS_FAIL, message);
    }
}
