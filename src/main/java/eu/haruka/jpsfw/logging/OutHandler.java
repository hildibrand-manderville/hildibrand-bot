package eu.haruka.jpsfw.logging;

import java.io.*;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

class OutHandler extends StreamHandler {

    public DataOutputStream fi;
    private Formatter f;

    public OutHandler(Formatter f, File fi) throws FileNotFoundException {
        this(f, fi, true);
    }

    public OutHandler(Formatter f, File fi, boolean usesystemout) throws FileNotFoundException {
        this.f = f;
        if (fi != null) {
            this.fi = new DataOutputStream(new FileOutputStream(fi, true));
        }
        if (usesystemout) {
            setOutputStream(System.out);
        }
        setLevel(Level.ALL);
    }

    @Override
    public Formatter getFormatter() {
        return f;
    }

    @Override
    public synchronized void publish(LogRecord record) {
        if (fi != null) {
            try {
                fi.writeBytes(f.format(record));
            } catch (IOException ignored) {
            }
        }
        super.publish(record);
        flush();
    }

}
