package eu.haruka.jpsfw.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Class used to work with JSON files mapped to a java object.
 */
public class JsonT<T> {

    private Gson gson;
    private String source;
    private T object;
    private Class<T> type;

    private JsonT() {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.serializeSpecialFloatingPointValues();
        gson = builder.create();
    }

    /**
     * Creates a new JSON object.
     *
     * @param file The JSON file to load.
     * @param type The class of the target object that will be created.
     * @throws IOException if there is an error reading the file, or with the JSON syntax.
     */
    public JsonT(File file, Class<T> type) throws IOException {
        this(new String(Files.readAllBytes(Paths.get(file.toURI())), StandardCharsets.UTF_8), type);
    }

    /**
     * Creates a new JSON object.
     *
     * @param string The JSON string to load.
     * @param type   The class of the target object that will be created.
     * @throws IOException if there is an error reading the file, or with the JSON syntax.
     */
    public JsonT(String string, Class type) throws IOException {
        this();
        source = string;
        this.type = type;
        object = load();
    }

    /**
     * Creates a new JSON object.
     *
     * @param object The object to use as a JSON object.
     */
    public JsonT(T object) {
        this();
        this.object = object;
        type = (Class<T>) object.getClass();
        source = save();
    }

    /**
     * (Re-)loads the file from the string. A file's content is cached in memory and will not be reloaded.
     *
     * @return The parsed JSON object.
     * @throws IOException if there is an error reading the file, or with the JSON syntax.
     */
    public T load() throws IOException {
        try {
            return gson.fromJson(source, type);
        } catch (JsonSyntaxException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Saves this JSON object to a string.
     *
     * @return The object bound to this JSON in JSON format.
     */
    public String save() {
        return gson.toJson(object, type);
    }

    /**
     * Saves this JSON object to a file.
     *
     * @param file The file to save to.
     * @throws IOException If there is an error writing the file.
     */
    public void save(File file) throws IOException {
        try (FileWriter fw = new FileWriter(file)) {
            gson.toJson(object, type, fw);
        }
    }

    /**
     * Returns the object instance.
     *
     * @return the object instance.
     */
    public T getObject() {
        return object;
    }

    /**
     * Sets the object instance.
     *
     * @param object The new object.
     */
    public void setObject(T object) {
        this.object = object;
    }


    /**
     * Static method to quickly convert an object to JSON.
     *
     * @param object The object to convert.
     * @param <T>    The type of the object.
     * @return A string containing the object in JSON.
     */
    public static <T> String toJson(T object) {
        return new JsonT<T>(object).save();
    }

    /**
     * Static method to quickly convert an object from JSON.
     *
     * @param str   The JSON string to convert.
     * @param clazz The class of the target object.
     * @param <T>   The type of the target object.
     * @return A new instance of the object described by str.
     * @throws IOException If there is an error parsing the JSON string.
     */
    public static <T> T fromJson(String str, Class<T> clazz) throws IOException {
        return new JsonT<T>(str, clazz).getObject();
    }

    /**
     * Convenience method for writing an object in JSON format to a stream.
     *
     * @param w   The output.
     * @param obj The object to convert.
     * @throws IOException If there is an error writing to the stream.
     */
    public static void write(Writer w, Object obj) throws IOException {
        w.write(toJson(obj));
    }

    /**
     * Convenience method for writing an object in JSON format to a stream.
     *
     * @param resp The output.
     * @param obj  The object to convert.
     * @throws IOException If there is an error writing to the stream.
     */
    public static void write(HttpServletResponse resp, Object obj) throws IOException {
        write(resp.getWriter(), obj);
    }
}
