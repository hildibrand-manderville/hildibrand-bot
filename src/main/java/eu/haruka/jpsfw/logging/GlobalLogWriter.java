package eu.haruka.jpsfw.logging;

import org.eclipse.jetty.server.RequestLog;

import java.io.IOException;
import java.util.logging.Level;

public class GlobalLogWriter implements RequestLog.Writer {

    private Level level;
    private String prefix = "";

    public GlobalLogWriter(Level level) {
        this.level = level;
    }

    public GlobalLogWriter(Level level, String prefix) {
        this.level = level;
        this.prefix = prefix;
    }

    @Override
    public void write(String s) throws IOException {
        GlobalLogger.log(level, prefix + s);
    }

}
