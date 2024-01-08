package eu.haruka.jpsfw.logging;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Date;
import java.util.Random;

public class RefCodeMaker {

    public static File LOG_DIR = new File(GlobalLogger.LOG_DIRECTORY, "ref");
    private static Random rng = new Random();

    public static String dump(String message) {
        return dump(message, null);
    }

    public static String dump(String message, Throwable exception, Object... data) {
        String code = generateString(rng, "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", 16);
        PrintWriter fw = null;
        try {
            if (!LOG_DIR.exists()) {
                Files.createDirectory(LOG_DIR.toPath());
            }
            fw = new PrintWriter(new FileWriter(new File(LOG_DIR, code + ".txt")));
            fw.println(new Date().toString());
            for (Object o : data) {
                fw.println(o);
            }
            fw.println("---------------------------");
            fw.println(message);
            if (exception != null) {
                exception.printStackTrace(fw);
            }
            fw.println("---------------------------");
            new Exception().printStackTrace(fw);
        } catch (Exception ex) {
            GlobalLogger.exception(ex, "Failed to dump RefCode for " + code);
        } finally {
            if (fw != null) {
                fw.close();
            }
        }
        return code;
    }

    public static String generateString(Random rng, String characters, int length) {
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);
    }

}
