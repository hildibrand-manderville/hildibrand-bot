package eu.haruka.jpsfw.logging;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class for a unified logger between private servers.
 */
public class GlobalLogger {

    /**
     * The Logger object.
     */
    private static final Logger log = Logger.getLogger("HarukaPServerFramework");
    private static final Logger exceptionLog = Logger.getLogger("HarukaPServerFrameworkException");

    /**
     * The logging directory.
     */
    public static final File LOG_DIRECTORY = new File("log");

    private static boolean initialized;
    private static boolean remoteAccessAllowed;
    private static ArrayList<String> createdLoggers = new ArrayList<>();
    private static boolean printThreadName = true;

    /**
     * Returns true if the logger is initalized.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    public static void setPrintThreadName(boolean printThreadName) {
        if (isInitialized()) {
            throw new IllegalStateException("Logger already initialized");
        }
        GlobalLogger.printThreadName = printThreadName;
    }

    public static void initialize() throws IOException {
        initialize(false);
    }

    /**
     * Initializes the global logger.
     *
     * @param allowAccess Whether or not to allow getLogger.
     * @throws IOException If there is an error creating the log file.
     */
    public static void initialize(boolean allowAccess) throws IOException {
        if (initialized) {
            return;
        }
        if (!LOG_DIRECTORY.exists()) {
            LOG_DIRECTORY.mkdir();
        }
        createLogger(log.getName(), new File(LOG_DIRECTORY, "server.log"), true, printThreadName);
        createLogger("HarukaPServerFrameworkException", new File(LOG_DIRECTORY, "exception.log"), false, printThreadName);
        initialized = true;
        remoteAccessAllowed = allowAccess;
    }

    public static Logger getLogger() {
        if (remoteAccessAllowed) {
            return log;
        } else {
            throw new RuntimeException("not allowed");
        }
    }

    /**
     * Creates a new logger
     *
     * @param name       The internal name of the logger.
     * @param file       The file where the log is saved to, or null for no log file.
     * @param system_out Whether or not to write log content to System.out
     * @return The new Logger.
     * @throws IOException If there is an error creating the log file.
     */
    public static Logger createLogger(String name, File file, boolean system_out) throws IOException {
        return createLogger(name, file, system_out, true);
    }

    /**
     * Creates a new logger
     *
     * @param name       The internal name of the logger.
     * @param file       The file where the log is saved to, or null for no log file.
     * @param system_out Whether or not to write log content to System.out
     * @return The new Logger.
     * @throws IOException If there is an error creating the log file.
     */
    public static Logger createLogger(String name, File file, boolean system_out, boolean thread) throws IOException {
        Logger log = Logger.getLogger(name);
        if (createdLoggers.contains(name)) {
            return log;
        }
        createdLoggers.add(name);
        log.setUseParentHandlers(false);
        log.setLevel(Level.ALL);
        if (file != null) {
            if (!file.exists()) {
                Files.createFile(file.toPath());
            }
        }
        NiceFormatter nf = new NiceFormatter();
        OutHandler oh = new OutHandler(nf, file, false);
        log.addHandler(oh);
        if (system_out) {
            log.addHandler(new OutHandler(new NiceFormatter(thread), null, true));
        }
        return log;
    }

    public static void info(String str) {
        log.info(str);
    }

    public static void warning(String str) {
        log.warning(str);
    }

    public static void severe(String str) {
        log.severe(str);
    }

    public static void fine(String str) {
        log.fine(str);
    }

    public static void finer(String str) {
        log.finer(str);
    }

    public static void finest(String str) {
        log.finest(str);
    }

    public static void log(Level l, String str) {
        log.log(l, str);
    }

    @Deprecated
    public static void exception(String str, Throwable ex) {
        exception(ex, str);
    }

    public static void exception(Throwable ex, String str) {
        log.log(Level.SEVERE, str, ex);
        exceptionLog.log(Level.SEVERE, str, ex);
    }

    public static void setLevel(Level l) {
        log.setLevel(l);
    }

}
