package milbot.api.v1;

import eu.haruka.jpsfw.web.Webserver;
import milbot.api.APIBase;
import milbot.api.HildiAPI;
import milbot.api.Nothing;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class APIStatus extends HildiAPI<Nothing, APIStatus.StatusResponse> {

    public APIStatus(Webserver server) {
        super(server);
    }

    @Override
    protected Class<Nothing> getInputType() {
        return null;
    }

    @Override
    protected StatusResponse handle(Nothing in, HttpServletRequest req, HttpServletResponse resp) {
        return new StatusResponse();
    }

    public static class StatusResponse extends APIBase {

        public StatusResponse() {
            status = true;
        }

        public int version = 1;
    }
}
