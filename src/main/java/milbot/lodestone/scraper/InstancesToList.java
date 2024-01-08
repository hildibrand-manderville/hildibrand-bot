package milbot.lodestone.scraper;

import com.google.gson.reflect.TypeToken;
import eu.haruka.jpsfw.configuration.Json;
import milbot.objects.Instance;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static milbot.Main.DATA_DIR;

public class InstancesToList {

    public static void main(String[] args) throws IOException {
        List<Instance> instances = (List<Instance>) new Json(new File(DATA_DIR, "instances.json"), new TypeToken<ArrayList<Instance>>() {
        }).getObject();
        for (Instance i : instances) {
            System.out.println(i.name);
            for (String a : i.altnames) {
                System.out.println(" - " + a);
            }
        }
    }

}
