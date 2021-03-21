package fr.i360matt.fastmongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.i360matt.fastmongo.utils.ExpirableCache;
import fr.i360matt.fastmongo.utils.ExpirableCacheList;
import org.bson.Document;
import java.util.Collections;

/**
 * This class is used to instantiate the connection to the database
 * @author 360matt
 */
public final class MongoIntegration {

    protected static final ExpirableCacheList<String> cache = new ExpirableCacheList<>(1_000);
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

    /**
     * Allow to connect to the Mongo server
     * @param auth All information required for connection
     */
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
        isAvailable = true;
    }

    /**
     * Allow to end a connection
     */
    public static void disconnect () {
        client.close();
        isAvailable = false;
    }


    public static boolean isAvailable () { return isAvailable; }
    public static void setAvailable (boolean value) { isAvailable = value; }

    /**
     * Allows to find out if a collection exists
     * @param name The name of the collection
     * @return The answer about existence
     */
    public static boolean existCollect (final String name) {
        for (final String candidate : database.listCollectionNames())
            if (candidate.equalsIgnoreCase(name))
                return true;
        return false;
    }

    /**
     * Allows to create a collection, without error checking, you must use existCollect() before evoking this method.
     * @param name The name of the collection to create
     */
    public static void createCollect (final String name) {
        MongoIntegration.database.createCollection(name);
    }

    /**
     * Allows to retrieve a collection
     * @param name The name of the collection
     * @return the collection, can be null.
     */
    public static MongoCollection<Document> getCollect (final String name) {
        return MongoIntegration.database.getCollection(name);
    }

}
