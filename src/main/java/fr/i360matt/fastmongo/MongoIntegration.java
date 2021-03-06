package fr.i360matt.fastmongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Collections;

public final class MongoIntegration {

    protected static final ExpirableCache<String, Boolean> cache = new ExpirableCache<>(1_000);
    protected static final ExpirableCache<Class<?>, Object> typeCache = new ExpirableCache<>(10_000);
    protected static final ExpirableCache<String, CollectionManager> collectionCache = new ExpirableCache<>(3600_000);

    private static boolean isAvailable;
    public static MongoDatabase database;
    public static MongoClient client;

    public static class Auth {
        public String host;
        public int port;
        public String user;
        public String password;
        public String database;
    }

    public static void connect (final Auth auth) {
        final MongoCredential credential = MongoCredential.createCredential(
                auth.user,
                auth.database,
                auth.password.toCharArray()
        );

        if (client != null) {
            disconnect();
        }

        client = new MongoClient(
                new ServerAddress(auth.host, auth.port),
                Collections.singletonList(credential)
        );

        database = client.getDatabase(auth.database);
        setAvailable(true);
    }

    public static void disconnect () {
        client.close();
        setAvailable(false);
    }


    public static boolean isAvailable () { return isAvailable; }
    public static void setAvailable (boolean value) { isAvailable = value; }


    public static boolean existCollect (final String name) {
        for (final String candidate : database.listCollectionNames())
            if (candidate.equalsIgnoreCase(name))
                return true;
        return false;
    }
    public static void createCollect (final String name) {
        MongoIntegration.database.createCollection(name);
    }
    public static MongoCollection<Document> getCollect (final String name) {
        return MongoIntegration.database.getCollection(name);
    }

}
