package milbot.lodestone.scraper;

import com.google.gson.reflect.TypeToken;
import eu.haruka.jpsfw.configuration.Json;
import milbot.objects.Instance;
import milbot.objects.InstanceType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    // hahahaha

    public static String[] urls = new String[]{
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=2",
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=2&page=2",
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=2&page=3",
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=2&page=4",
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=2&page=5",
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=2&page=6",
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=4",
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=4&page=2",
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=4&page=3",
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=4&page=4",
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=4&page=5",
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=4&page=6",
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=5",
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=5&page=2",
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=5&page=3",
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=5&page=4",
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=5&page=5",
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=5&page=6",
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=28",
            "https://na.finalfantasyxiv.com/lodestone/playguide/db/duty/?category2=28&page=2",
    };

    public static void main(String[] args) throws Exception {

        List<Instance> old = new ArrayList<>();
        File oldfile = new File("instances.json");
        if (oldfile.exists()) {
            old = (List<Instance>) new Json(oldfile, new TypeToken<ArrayList<Instance>>() {
            }).getObject();
        }

        ArrayList<Instance> instances = new ArrayList<Instance>();

        for (String url : urls) {
            System.out.println(url);
            Document list = Jsoup.connect(url).get();
            for (Element e : list.getElementsByTag("a")) {
                String href = e.attr("href");
                if (href != null && href.startsWith("/lodestone/playguide/db/duty/") && !href.contains("?") && href.split("/").length > 5 && href.split("/")[5].length() > 0
                ) {
                    System.out.println(href);
                    Thread.sleep(1000);
                    Document instance = Jsoup.connect("https://na.finalfantasyxiv.com" + href).get();
                    String name = instance.getElementsByClass("latest_patch__major__detail__text").get(0).text();
                    String level = instance.getElementsByClass("db-view__detail__level").get(0).text();
                    String type = instance.getElementsByClass("db-view__detail__content_type").get(0).text();
                    String players = instance.getElementsByClass("db-view__data__content_info").get(1).child(0).text();
                    String ilvl = instance.getElementsByClass("db-view__data__content_info").get(1).html();
                    System.out.println(ilvl);
                    String img = instance.getElementsByClass("db-view__detail__visual").get(0).child(0).attr("src");

                    Instance i = new Instance();
                    i.lsid = href.split("/")[href.split("/").length - 1];
                    i.name = name;
                    i.level = Integer.parseInt(level.split(" ")[1]);
                    i.type = InstanceType.valueOf(type.toUpperCase().replace(' ', '_'));
                    i.players = players.contains("3 parties") ? 24 : (players.contains("1-8") || players.contains("8 player(s) only") ? 8 : 4);

                    String pattern = "Item Level\\: (.*)\\<";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(ilvl);
                    if (m.find()) {
                        i.ilevel = Integer.parseInt(m.group(1).split("<")[0]);
                    }
                    i.imageUrl = img;

                    Instance old_version = getInstanceFrom(old, i.lsid);
                    if (old_version != null) {
                        i.altnames = old_version.altnames;
                    }

                    System.out.println(i);
                    instances.add(i);

                }
            }
        }

        new Json(instances, true).save(new File("instances.json"));

    }

    private static Instance getInstanceFrom(List<Instance> old, String lsid) {
        for (Instance i : old) {
            if (lsid.equals(i.lsid)) {
                return i;
            }
        }
        return null;
    }
}
