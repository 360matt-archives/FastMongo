package fr.i360matt.fastmongo;

import com.mongodb.client.MongoCollection;
import fr.i360matt.fastmongo.utils.ExpirableCache;
import org.bson.Document;
import java.lang.reflect.Field;

/**
 * This class is used to manage a collection
 * @author 360matt
 */
public class CollectionManager {

    protected static final ExpirableCache<Class<?>, Object> typeCache = new ExpirableCache<>(10_000);
    protected static final ExpirableCache<String, CollectionManager> collectionCache = new ExpirableCache<>(3600_000);

    /**
     * Allows to retrieve the manager of the collection in the cache
     * @param name name of the collection
     * @return the manager concerned
     */
    public static CollectionManager getCollection (final String name) {
        if (collectionCache.containsKey(name)) {
            return collectionCache.get(name);
        } else {
            return new CollectionManager(name);
            // will be added to the cache at instantiation  [*]
        }
    }


    public final String name;
    public String fieldID = "_id";
    public final MongoCollection<Document> collection;

    public Class<?> defaultTemplate;
    public Document defaultDocument;
    public boolean autoInsert;

    public CollectionManager (final String name) {
        this.name = name;

        final CollectionManager candidate = collectionCache.get(name);
        if (candidate != null) {
            // if the collection is already in the cache

            this.collection = candidate.collection;
        } else {
            if (!MongoIntegration.existCollect(name))
                MongoIntegration.createCollect(name);

            this.collection = MongoIntegration.getCollect(name);

            // add to static cache: [*]
            collectionCache.put(name, this);
        }
    }

    /**
     * Allows to apply the chosen structure to all existing documents
     * @param structure data structure class
     */
    public final void updateStructure (final Class<?> structure) {
        this.defaultTemplate = structure;
        final Document doc = new Document();

        for (final Field field : structure.getFields()) {
            doc.append(field.getName(), typeCache.get(structure));

            collection.updateMany(
                    new Document(field.getName(), new Document("$exists", false)), // patern: [ 'field' don't exist ]
                    new Document("$set", new Document(field.getName(), doc.get(field.getName())))
            );
        }
    }

    /**
     * Allows to define whether documents should be created when elements are instantiated
     * With the choice of the structure to be applied
     * @param structure the default structure
     */
    public final <D> void autoInsert (final Class<D> structure) {
        this.autoInsert = true;
        this.defaultDocument = new Document();

        for (final Field field : structure.getFields()) {
            this.defaultDocument.append(field.getName(), typeCache.get(structure));
        }
    }

    /**
     * Allows to change the name of the field which serves as an identifier
     * Example: UUID, username, etc ...
     * @param id the name of the field
     */
    public final void setFieldID (final String id) {
        this.fieldID = id;
    }


    /**
     * Allows to recover the default data of the chosen structure
     * @param structure chosen structure
     * @param <D> the type of structure
     * @return the default data of the structure (instance)
     */
    public final <D> D getEmptyRaw (final Class<D> structure) {
        if (!typeCache.containsKey(structure)) {
            try {
                final D res = structure.newInstance();
                typeCache.put(structure, res);
                return res;
            } catch (InstantiationException | IllegalAccessException e) {
                return null;
            }
        }
        return (D) typeCache.get(structure);
    }


    /**
     * Allows to convert a Document into a Raw
     * @param doc original document
     * @return generated structure
     */
    public final <D> D getRawFromDocument (final Document doc, final Class<D> structure) {
        try {
            final D res = getEmptyRaw(structure);
            if (doc != null) { // check de sécurité si le document existe.
                for (final Field field : structure.getFields()) {
                    if (doc.containsKey(field.getName()))
                        field.set(res, doc.get(field.getName()));
                    // importation des valeurs des fields du Raw vide
                }
            }
            return res;
        } catch (final IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Allows to retrieve an item
     * @param id element id
     * @return the item in question
     */
    public final Element getObject (final String id) {
        return new Element(id, this);
    }

    /**
     * Allows to check if an element exists
     * @param id identifiant de l'élément
     * @return l'état de l'existence
     */
    public final boolean exist (final String id) {
        return collection.count(new Document(this.fieldID, id)) > 0;
    }

    /**
     * Allows to delete the item
     * @param id element id
     */
    public final void remove (final String id) {
        collection.deleteOne(new Document(this.fieldID, id));
    }




    // _________________________________________________________________________________________________________________

    /**
     * Allows to create a classification by one or more fields
     * @param fields chosen fields
     * @return an instance of the ranking module
     */
    public final Sort buildSort (final String... fields) {
        return new Sort(this).ascending(fields);
    }

    /**
     * Allows you to create a classification by one or more fields
     * @return an instance of the ranking module
     */
    public final Sort buildSort () {
        return new Sort(this);
    }




}
