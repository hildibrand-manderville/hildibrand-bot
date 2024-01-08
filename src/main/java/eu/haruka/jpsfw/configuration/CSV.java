package eu.haruka.jpsfw.configuration;

import eu.haruka.jpsfw.util.Etc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Class used to work with .csv files
 */
public class CSV implements Iterable<String[]> {

    private String[] headers;
    private ArrayList<String[]> data;

    public CSV(File f) throws IOException {
        this(f, ',', true);
    }

    public CSV(File f, char seperator, boolean has_header) throws IOException {
        this(Etc.readFile(f), seperator, has_header);
    }

    public CSV(String[] str, char seperator, boolean has_header) throws IOException {
        data = new ArrayList<>();
        if (str == null || str.length == 0) {
            headers = new String[0];
            return;
        }
        if (has_header) {
            headers = str[0].split(String.valueOf(seperator));
        } else {
            headers = new String[str[0].split(String.valueOf(seperator)).length];
        }
        for (int i = has_header ? 1 : 0; i < str.length; i++) {
            String line = str[i];
            String[] row = line.split(String.valueOf(seperator));
            if (row.length != headers.length) {
                throw new IOException("Failed to read CSV file: Malformed line: " + line + "(expected " + headers.length + ", got " + row.length);
            }
            if (has_header && i == 0) { // skip header row
                continue;
            }
            data.add(row);
        }
    }

    public int size() {
        return data.size();
    }

    public String[][] getData() {
        return data.toArray(new String[0][]);
    }

    public String[] getRow(int i) {
        return data.get(i);
    }

    public String getValue(String header, int row) {
        int index = Etc.indexOf(header, headers);
        if (index == -1) {
            throw new IllegalArgumentException("Unknown CSV header: " + header + ", Available: " + Arrays.toString(headers));
        }
        return data.get(row)[index];
    }

    public List<String[]> find(String header, String value) {
        ArrayList<String[]> results = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            if (value.equals(getValue(header, i))) {
                results.add(getRow(i));
            }
        }
        return results;
    }

    public String[] findOne(String header, String value) {
        for (int i = 0; i < data.size(); i++) {
            if (value.equals(getValue(header, i))) {
                return getRow(i);
            }
        }
        return null;
    }

    public int indexOf(String header, String value) {
        for (int i = 0; i < data.size(); i++) {
            if (value.equals(getValue(header, i))) {
                return i;
            }
        }
        return -1;
    }


    public String[] getHeaders() {
        return headers;
    }

    @Override
    public Iterator<String[]> iterator() {
        return new Iterator<String[]>() {

            private int i;

            @Override
            public boolean hasNext() {
                return i + 1 < data.size();
            }

            @Override
            public String[] next() {
                return getRow(i++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
