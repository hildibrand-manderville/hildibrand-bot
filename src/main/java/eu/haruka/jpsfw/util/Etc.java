package eu.haruka.jpsfw.util;


import java.io.*;
import java.util.ArrayList;

/**
 * Misc. things.
 */
public class Etc {

    private Etc() {
    }

    /**
     * Searches for an element in an array.
     *
     * @param needle   The element to find.
     * @param haystack The array.
     * @param <T>      The type of the element.
     * @return The index of the element in the array or -1.
     */
    public static <T> int indexOf(T needle, T[] haystack) {
        for (int i = 0; i < haystack.length; i++) {
            if (haystack[i] != null && haystack[i].equals(needle)
                    || needle == null && haystack[i] == null) return i;
        }

        return -1;
    }

    /**
     * Reads all lines from a file.
     *
     * @param file The file to read.
     * @throws IOException If there is an error reading from the file.
     */
    public static String[] readFile(File file) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            lines.add(line);
        }
        br.close();
        return lines.toArray(new String[lines.size()]);
    }

    /**
     * Writes a file in binary mode.
     *
     * @param file The file to write.
     * @param b    The content of the file.
     * @throws IOException If there is an error writing the file.
     */
    public static void writeFileBinary(File file, byte[] b) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(b);
        fos.flush();
        fos.close();
    }
}
