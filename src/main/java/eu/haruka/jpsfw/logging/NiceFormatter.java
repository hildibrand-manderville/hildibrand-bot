package eu.haruka.jpsfw.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

class NiceFormatter extends Formatter {

    private static final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    private static final DateFormat stf = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);


    private Date prevDate;
    private boolean thread;

    public NiceFormatter() {
        this(false);
    }

    public NiceFormatter(boolean thread) {
        this.thread = thread;
    }

    @Override
    public String format(LogRecord record) {
        Date d = new Date();

        StringBuilder out = new StringBuilder();

        if (prevDate == null || prevDate.getDate() != d.getDate()) {
            out.append("--- ").append(sdf.format(d)).append(" ---\n");
        }
        prevDate = d;

        out.append('[');
        out.append(stf.format(d));
        out.append(']');

        if (thread) {
            out.append('[');
            out.append(Thread.currentThread().getName());
            out.append(']');
        }

        out.append('[');
        out.append(record.getLevel());
        out.append("] ");

        out.append(record.getMessage());

        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            record.getThrown().printStackTrace(pw);
            out.append('\n').append(sw);
        }

        out.append('\n');

        return out.toString();

    }

}
