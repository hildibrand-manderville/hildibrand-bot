package milbot.api;

import eu.haruka.jpsfw.configuration.JsonT;
import eu.haruka.jpsfw.logging.GlobalLogger;
import eu.haruka.jpsfw.web.HandlerWrapper;
import eu.haruka.jpsfw.web.Webserver;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.util.Enumeration;

public abstract class HildiAPI<InputType extends APIBase, OutputType extends APIBase> extends HandlerWrapper {

    public HildiAPI(Webserver server) {
        super(server);
    }

    @Override
    public void get(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        post(httpServletRequest, httpServletResponse);
    }

    @Override
    public void post(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        Class<? extends APIBase> inclass = getInputType();
        JsonT<InputType> injson = inclass != null ? new JsonT<>(IOUtils.toString(req.getReader()), inclass) : null;
        JsonT<APIBase> outjson;
        try {
            InputType inobj = null;
            if (injson != null && injson.getObject() != null) {
                inobj = injson.getObject();
            } else if (inclass != null) {
                inobj = (InputType) inclass.newInstance();
            }
            if (inobj != null) {
                Enumeration<String> names = req.getParameterNames();
                while (names.hasMoreElements()) {
                    String field = names.nextElement();
                    try {
                        Field fieldobj = inclass.getField(field);
                        if (fieldobj.getType() == Integer.TYPE) {
                            fieldobj.setInt(inobj, Integer.parseInt(req.getParameter(field)));
                        } else if (fieldobj.getType() == Long.TYPE) {
                            fieldobj.setLong(inobj, Long.parseLong(req.getParameter(field)));
                        } else if (fieldobj.getType() == Boolean.TYPE) {
                            fieldobj.setBoolean(inobj, Boolean.parseBoolean(req.getParameter(field)));
                        } else {
                            fieldobj.set(inobj, req.getParameter(field));
                        }
                    } catch (NoSuchFieldException ignored) {
                    }
                }
            }
            outjson = new JsonT<>(handle(inobj, req, resp));
        } catch (Exception ex) {
            APIError er = new APIError("Internal server error: " + ex.getMessage());
            GlobalLogger.exception(ex, "Error handling " + req.getRequestURI());
            outjson = new JsonT<>(er);
        }

        resp.setContentType("application/json");
        resp.getWriter().write(outjson.save());
    }

    protected abstract Class<InputType> getInputType();

    protected abstract OutputType handle(InputType in, HttpServletRequest req, HttpServletResponse resp);
}
