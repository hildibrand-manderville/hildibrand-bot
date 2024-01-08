/*
 * Copyright (C) 2015-2016 eu.haruka
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.haruka.jpsfw.web;

import eu.haruka.jpsfw.logging.GlobalLogWriter;
import eu.haruka.jpsfw.logging.GlobalLogger;
import eu.haruka.jpsfw.logging.JettyLogDisabler;
import eu.haruka.jpsfw.util.AccessEverything;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.h2.server.web.WebServer;
import org.h2.server.web.WebServlet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.*;
import java.util.logging.Level;

/**
 * Class used for a webserver that belongs to a private server instance, such as for an item shop or other web-based contents based on the jetty engine.
 */
public class Webserver {

    private Server server;
    private boolean suppressJettyErrors;
    private int[] ports;
    private String rootDirName;
    private File rootFolder;
    private File keystore;
    private String keystorepass;
    private String keyalias;
    private ServletContextHandler context;
    private boolean started;
    private boolean ssl;
    private HashMap<String, HandlerWrapper> handlers;
    private boolean dirAllowed;

    /**
     * Creates a new Webserver.
     *
     * @param suppressJettyErrors true if errors from the jetty engine should not show up in the log.
     * @param port                The port on which the server runs on.
     * @param rootDirName         The name of the basepath if the server should also serve static files such as images or HTML
     * @param rootFolder          The folder containing static files. May be null to disable.
     */
    public Webserver(boolean suppressJettyErrors, int port, String rootDirName, File rootFolder) {
        this(suppressJettyErrors, port, false, null, null, null, rootDirName, rootFolder);
    }

    /**
     * Creates a new Webserver.
     *
     * @param suppressJettyErrors true if errors from the jetty engine should not show up in the log.
     * @param port                The port on which the server runs on.
     * @param isSSL               Whether or not this server uses HTTPS.
     * @param rootDirName         The name of the basepath if the server should also serve static files such as images or HTML
     * @param rootFolder          The folder containing static files. May be null to disable.
     */
    public Webserver(boolean suppressJettyErrors, int port, boolean isSSL, File keystorePath, String keyPass, String keyAlias, String rootDirName, File rootFolder) {
        this(suppressJettyErrors, new int[]{port}, isSSL, keystorePath, keyPass, keyAlias, rootDirName, rootFolder, false);
    }

    /**
     * Creates a new Webserver.
     *
     * @param suppressJettyErrors true if errors from the jetty engine should not show up in the log.
     * @param ports               The ports on which the server runs on.
     * @param isSSL               Whether or not this server uses HTTPS.
     * @param rootDirName         The name of the basepath if the server should also serve static files such as images or HTML
     * @param rootFolder          The folder containing static files. May be null to disable.
     */
    public Webserver(boolean suppressJettyErrors, int[] ports, boolean isSSL, File keystorePath, String keyPass, String keyAlias, String rootDirName, File rootFolder, boolean directoryAllowed) {
        this.suppressJettyErrors = suppressJettyErrors;
        this.ports = ports;
        this.ssl = isSSL;
        this.keystore = keystorePath;
        this.keystorepass = keyPass;
        this.keyalias = keyAlias;
        this.rootDirName = rootDirName;
        this.rootFolder = rootFolder;
        this.handlers = new HashMap<>();
        this.dirAllowed = directoryAllowed;
        initalize();
    }

    private void initalize() {

        GlobalLogger.info("Webserver initializing...");
        GlobalLogger.fine("Suppress Jetty Errors: " + suppressJettyErrors);

        if (suppressJettyErrors) {
            System.setProperty("org.eclipse.jetty.http.LEVEL", "OFF");
            System.setProperty("org.eclipse.jetty.util.log.StdErrLog", "OFF");
            org.eclipse.jetty.util.log.Log.setLog(new JettyLogDisabler());
        }

        QueuedThreadPool threadPool = new QueuedThreadPool(30, 2);
        server = new Server(threadPool);

        ArrayList<ServerConnector> conns = new ArrayList<>();
        for (int port : ports) {
            ServerConnector c1;
            GlobalLogger.fine("Creating connector on port: " + port);

            if (ssl) {
                GlobalLogger.fine("SSL enabled");
                GlobalLogger.fine("Keystore: " + keystore.getAbsolutePath());
                GlobalLogger.fine("Keystore Password: " + keystorepass);
                GlobalLogger.fine("Keystore Key Name: " + keyalias);
                HttpConfiguration https = new HttpConfiguration();
                https.addCustomizer(new SecureRequestCustomizer());
                https.addCustomizer(new ForwardedRequestCustomizer());
                https.setSendServerVersion(false);
                https.setSendXPoweredBy(false);
                https.setIdleTimeout(30000);

                SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
                sslContextFactory.setKeyStorePath(keystore.getAbsolutePath());
                sslContextFactory.setKeyStorePassword(keystorepass);
                sslContextFactory.setKeyManagerPassword(keystorepass);
                sslContextFactory.setCertAlias(keyalias);
                sslContextFactory.setTrustAll(true);
                try {
                    KeyStore ks = KeyStore.getInstance("JKS");
                    ks.load(new FileInputStream(keystore), keystorepass.toCharArray());
                    sslContextFactory.setTrustStore(ks);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                c1 = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(https));
            } else {
                HttpConfiguration http = new HttpConfiguration();
                http.addCustomizer(new ForwardedRequestCustomizer());
                http.setSendServerVersion(false);
                http.setSendXPoweredBy(false);
                http.setIdleTimeout(30000);
                c1 = new ServerConnector(server, new HttpConnectionFactory(http));
            }
            c1.setPort(port);
            conns.add(c1);
        }
        server.setConnectors(conns.toArray(new ServerConnector[conns.size()]));
        GlobalLogger.fine(conns.size() + " connectors initialized");

        WebAppContext webappcontext = new WebAppContext();
        if (rootFolder != null) {
            GlobalLogger.fine("Flat file folder set as: " + rootFolder.getAbsolutePath());
            if (!rootFolder.exists()) {
                throw new IllegalArgumentException(rootFolder + " does not exist");
            }
            //System.setProperty("org.apache.jasper.compiler.disablejsr199", "true");
            if (rootDirName != null) {
                GlobalLogger.fine("Flat file accessible under: /" + rootDirName);
                webappcontext.setContextPath("/" + rootDirName);
            } else {
                GlobalLogger.fine("Flat file accessible under: /");
                webappcontext.setContextPath("/");
            }
            webappcontext.setThrowUnavailableOnStartupException(true);
            webappcontext.setWar(rootFolder.getAbsolutePath());
            webappcontext.setWelcomeFiles(new String[]{"index.jsp", "index.php", "index.htm", "index.html"});
            webappcontext.setInitParameter("useFileMappedBuffer", "false");
            webappcontext.setInitParameter("maxCachedFiles", "0");
            webappcontext.setInitParameter("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");
            webappcontext.setInitParameter("compilerTargetVM", "1.8");
            webappcontext.setInitParameter("compilerSourceVM", "1.8");
            webappcontext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", String.valueOf(dirAllowed));
            webappcontext.setInitParameter("dirAllowed", String.valueOf(dirAllowed));
        }

        context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        GlobalLogger.fine("Context initialized");

        ServletHolder holderHome = new ServletHolder("JPSFW-Static-Files", DefaultServlet.class);
        if (rootFolder != null) {
            holderHome.setInitParameter("resourceBase", rootFolder.getAbsolutePath());
        }
        holderHome.setInitParameter("dirAllowed", "true");
        holderHome.setInitParameter("pathInfoOnly", "true");
        holderHome.setInitParameter("useFileMappedBuffer", "false");
        holderHome.setInitParameter("maxCachedFiles", "0");
        holderHome.setInitParameter("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");
        holderHome.setInitParameter("compilerTargetVM", "1.8");
        holderHome.setInitParameter("compilerSourceVM", "1.8");
        holderHome.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", String.valueOf(dirAllowed));
        holderHome.setInitParameter("dirAllowed", String.valueOf(dirAllowed));
        context.addServlet(holderHome, "/*");

        ServletHolder holderPwd = new ServletHolder("default", DefaultServlet.class);
        holderPwd.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", String.valueOf(dirAllowed));
        holderPwd.setInitParameter("dirAllowed", String.valueOf(dirAllowed));
        context.addServlet(holderPwd, "/");

        server.setHandler(context);

        GlobalLogger.fine("Webserver path initialization complete.");
    }


    /**
     * Adds a handler to the webserver.
     *
     * @param w    The handler to add.
     * @param path The path of the handler.
     * @throws IllegalStateException If the server is already running.
     */
    public void addHandler(HandlerWrapper w, String path) throws IllegalStateException {
        if (started) {
            throw new IllegalStateException("server already running");
        }
        ServletHolder sh = new ServletHolder(w);
        sh.getRegistration().setMultipartConfig(new MultipartConfigElement("tmp"));
        context.addServlet(sh, path);
        handlers.put(path, w);
    }

    public void addFilter(Class<? extends Filter> clazz, String path) {
        context.addFilter(clazz, path, EnumSet.of(DispatcherType.INCLUDE, DispatcherType.REQUEST));
    }

    /**
     * Starts the server. No more handlers can be added after the server start.
     *
     * @throws Exception If there was an error starting the server.
     */
    public void start() throws Exception {

        WebServlet h2server = new WebServlet();
        context.addServlet(new ServletHolder(h2server), "/h2database/*");

        server.getServer().setRequestLog(new CustomRequestLog(new GlobalLogWriter(Level.FINE, "NetRequest: "), CustomRequestLog.EXTENDED_NCSA_FORMAT));

        server.start();

        started = true;

        if (h2server != null) {
            WebServer s = AccessEverything.get(h2server, "server", WebServer.class);
            AccessEverything.set(s, "allowOthers", true);
        }

        GlobalLogger.info("Webserver started on port " + Arrays.toString(ports));
    }

    /**
     * Returns a set of entries of all registered handlers, with the path being the key.
     *
     * @return a set of entries of all registered handlers
     */
    public Set<Map.Entry<String, HandlerWrapper>> getHandlers() {
        return handlers.entrySet();
    }

}
