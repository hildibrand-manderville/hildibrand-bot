package milbot.api;

import eu.haruka.jpsfw.web.Webserver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class API404 extends HildiAPI<Nothing, APIError> {

    public API404(Webserver server) {
        super(server);
    }

    @Override
    protected Class<Nothing> getInputType() {
        return null;
    }

    @Override
    protected APIError handle(Nothing in, HttpServletRequest req, HttpServletResponse resp) {
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return new APIError("404");
    }
}
