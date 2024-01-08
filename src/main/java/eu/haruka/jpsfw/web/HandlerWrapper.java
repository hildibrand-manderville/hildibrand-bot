package eu.haruka.jpsfw.web;

import eu.haruka.jpsfw.logging.GlobalLogger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Class that defines a path in the webserver.
 */
public abstract class HandlerWrapper extends HttpServlet {

    protected Webserver server;

    /**
     * Creates a new HandlerWrapper.
     *
     * @param server The webserver that owns this handler.
     */
    public HandlerWrapper(Webserver server) {
        this.server = server;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepare(req, resp);
        try {
            get(req, resp);
        } catch (Throwable tr) {
            GlobalLogger.exception("Error in handling web request from " + req.getRemoteAddr() + " to " + req.getRequestURI() + " (post)", tr);
            resp.setStatus(500);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepare(req, resp);
        try {
            post(req, resp);
        } catch (Throwable tr) {
            GlobalLogger.exception("Error in handling web request from " + req.getRemoteAddr() + " to " + req.getRequestURI() + " (post)", tr);
            resp.setStatus(500);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepare(req, resp);
        try {
            put(req, resp);
        } catch (Throwable tr) {
            GlobalLogger.exception("Error in handling web request from " + req.getRemoteAddr() + " to " + req.getRequestURI() + " (put)", tr);
            resp.setStatus(500);
        }
    }

    private void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepare(req, resp);
        try {
            patch(req, resp);
        } catch (Throwable tr) {
            GlobalLogger.exception("Error in handling web request from " + req.getRemoteAddr() + " to " + req.getRequestURI() + " (patch)", tr);
            resp.setStatus(500);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        prepare(req, resp);
        try {
            delete(req, resp);
        } catch (Throwable tr) {
            GlobalLogger.exception("Error in handling web request from " + req.getRemoteAddr() + " to " + req.getRequestURI() + " (delete)", tr);
            resp.setStatus(500);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getMethod().equals("PATCH")) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    private void prepare(HttpServletRequest req, HttpServletResponse resp) {
        GlobalLogger.fine("Web request from " + req.getRemoteAddr() + " to " + req.getRequestURI() + " (post)");
        resp.setCharacterEncoding("UTF-8");
    }

    /**
     * Called when a GET request is made to this handler.
     *
     * @param req  The HttpServletRequest.
     * @param resp The HttpServletResponse.
     * @throws Exception If there is an error in the web request.
     */
    public abstract void get(HttpServletRequest req, HttpServletResponse resp) throws Exception;

    /**
     * Called when a POST request is made to this handler.
     *
     * @param req  The HttpServletRequest.
     * @param resp The HttpServletResponse.
     * @throws Exception If there is an error in the web request.
     */
    public abstract void post(HttpServletRequest req, HttpServletResponse resp) throws Exception;

    /**
     * Called when a PUT request is made to this handler.
     *
     * @param req  The HttpServletRequest.
     * @param resp The HttpServletResponse.
     * @throws Exception If there is an error in the web request.
     */
    public void put(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        post(req, resp);
    }

    /**
     * Called when a PATCH request is made to this handler.
     *
     * @param req  The HttpServletRequest.
     * @param resp The HttpServletResponse.
     * @throws Exception If there is an error in the web request.
     */
    public void patch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        post(req, resp);
    }

    /**
     * Called when a DELETE request is made to this handler.
     *
     * @param req  The HttpServletRequest.
     * @param resp The HttpServletResponse.
     * @throws Exception If there is an error in the web request.
     */
    public void delete(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        post(req, resp);
    }

}
