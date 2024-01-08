package milbot.api;

public class APIError extends APIBase {

    public String error;

    public APIError(String error) {
        this.error = error;
    }
}
