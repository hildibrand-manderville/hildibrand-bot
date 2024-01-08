package milbot.lodestone;

import eu.haruka.jpsfw.configuration.CSV;
import milbot.Main;
import milbot.chatapi.Server;
import milbot.chatapi.User;
import milbot.objects.Instance;
import milbot.objects.db.FF14Class;
import milbot.objects.db.GuildSettings;
import milbot.objects.db.UserSettings;
import milbot.util.HibernateUtil;
import milbot.util.Util;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.query.Query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FF14GameDatabase {

    private static Session db;

    static {
        db = HibernateUtil.getSessionFactory().openSession();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Running db shutdown");
            shutdown();
            HibernateUtil.shutdown();
            System.out.println("...done");
        }));
    }

    public synchronized static UserSettings getOrCreateUser(User u) {
        return getOrCreateUser(u.getId());
    }

    public static synchronized UserSettings getOrCreateUser(long id) {
        UserSettings u = db.get(UserSettings.class, id);
        if (u == null) {
            u = new UserSettings(id);
            db.save(u);
        }
        return u;
    }

    public static synchronized GuildSettings getOrCreateGuild(Server g) {
        return getOrCreateGuild(g.getId());
    }

    public static synchronized GuildSettings getOrCreateGuild(long id) {
        GuildSettings g = db.get(GuildSettings.class, id);
        if (g == null) {
            g = new GuildSettings(id);
            db.save(g);
        }
        return g;
    }

    public static synchronized GuildSettings getGuild(long id) {
        return db.get(GuildSettings.class, id);
    }

    public static synchronized void save(Object o) {
        Transaction t = db.beginTransaction();
        try {
            db.saveOrUpdate(o);
            t.commit();
        } catch (Throwable tr) {
            t.rollback();
            throw tr;
        }
    }

    public static synchronized void initializeClasses() throws IOException {
        Transaction t = db.beginTransaction();
        //noinspection SqlWithoutWhere
        db.createSQLQuery("DELETE FROM classes").executeUpdate();
        CSV classes = new CSV(new File(Main.DATA_DIR, "classes.csv"), ',', true);
        for (int i = 0; i < classes.size(); i++) {
            db.save(new FF14Class(classes, i));
        }
        t.commit();
    }

    public static synchronized <T> List<T> loadAllData(Class<T> type) {
        CriteriaBuilder builder = db.getCriteriaBuilder();
        CriteriaQuery<T> criteria = builder.createQuery(type);
        criteria.from(type);
        return db.createQuery(criteria).getResultList();
    }

    public static synchronized FF14Class getClass(String clazz) {
        Query<FF14Class> q = db.createQuery("from FF14Class where name = :a OR shortname = :b OR altname1 = :c OR altname2 = :d", FF14Class.class);
        q.setParameter("a", clazz);
        q.setParameter("b", clazz);
        q.setParameter("c", clazz);
        q.setParameter("d", clazz);
        List<FF14Class> classes = q.getResultList();
        if (classes.size() > 0) {
            return classes.get(0);
        } else {
            return null;
        }
    }

    public static synchronized List<FF14Class> getClassByCategory(String cat) {
        Query<FF14Class> q = db.createQuery("from FF14Class where category = :a", FF14Class.class);
        q.setParameter("a", cat);
        return q.getResultList();
    }

    public static synchronized String getVersionInfo() {
        return ((SessionFactoryImpl) HibernateUtil.getSessionFactory()).getDialect().toString();
    }

    public static synchronized Session getDB() {
        return db;
    }

    public static Instance getInstanceByLodestoneId(String id) {
        if (id == null) {
            return null;
        }
        for (Instance i : Main.instances) {
            if (id.equals(i.lsid)) {
                return i;
            }
        }
        return null;
    }

    public static Instance getInstanceByName(String name) {
        String lname = name.toLowerCase();
        for (Instance i : Main.instances) {
            if (i.name.toLowerCase().contains(lname)) {
                return i;
            } else {
                for (String altname : i.altnames) {
                    if (altname.toLowerCase().contains(lname)) {
                        return i;
                    }
                }
            }
        }
        return null;
    }

    public static synchronized List<FF14Class> parseClassesFromString(String classesStr) {
        ArrayList<FF14Class> classes = new ArrayList<>();

        for (String classStr : Util.discordArgParse(classesStr)) {
            FF14Class clazz = FF14GameDatabase.getClass(classStr);
            if (clazz == null) {
                throw new IllegalArgumentException(classStr);
            }
            classes.add(clazz);
        }
        return classes;
    }

    public static synchronized List<GuildSettings> getAllServers() {
        return loadAllData(GuildSettings.class);
    }

    public static synchronized void executeRaw(String sql, Object... objs) {
        Transaction t = db.beginTransaction();
        try {
            Query q = db.createQuery(sql);
            for (int i = 0; i < objs.length; i++) {
                q.setParameter(i + 1, objs[i]);
            }
            q.executeUpdate();
            t.commit();
        } catch (Throwable tr) {
            t.rollback();
            throw tr;
        }
    }

    public static synchronized void shutdown() {
        db.close();
    }
}
