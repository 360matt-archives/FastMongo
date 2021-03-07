package fr.i360matt.fastmongo;

import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class representing and managing an object from the collection
 * @author 360matt
 */
public final class Element {

    public final String id;
    public final CollectionManager manager;

    /**
     * Allows to create an editing reference for a document (whether or not it is fictitious) so that the final document can be manipulated
     * @param id The ID of the document you want to represent
     * @param manager The manager of the collection where the document is supposed to be located (whether it exists or not)
     */
    public Element (final String id, final CollectionManager manager) {
        this.id = id;
        this.manager = manager;

        if (!MongoIntegration.cache.containsKey(id)) {
            MongoIntegration.cache.put(manager.name + "#" + id, false);
            defineDefaultSchema();
        }
    }


    /**
     * Allows to create the document if it does not exist
     */
    protected final void defineDefaultSchema () {
        if (manager.autoInsert && manager.defaultDocument != null) {
            try {
                manager.collection.updateOne(
                        new Document(manager.fieldID, this.id),
                        new Document("$setOnInsert", manager.defaultDocument),
                        new UpdateOptions().upsert(true)
                );
                // update document in DB
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    // _________________________________________________________________________________________________________________


    /**
     * Allows to define fields via a document
     * @param document the document in question
     */
    public final void setDocument (final Document document) {
        manager.collection.updateOne(
                new Document(manager.fieldID, this.id),
                new Document("$set", document),
                new UpdateOptions().upsert(true)
        );
    }

    /**
     * Allows to recover the document in its original form
     * @return the document in question
     */
    public final Document getDocument () {
        return manager.collection.find(new Document(manager.fieldID, id)).first();
    }


    // _________________________________________________________________________________________________________________


    /**
     * Allows to update the document data
     * @param raw the data to modify from a structure
     */
    public final <D> void setRaw (final D raw) {
        try {
            final Document values = new Document(manager.fieldID, this.id) {{
                for (final Field field : raw.getClass().getFields())
                    append(field.getName(), field.get(raw));
                // We creating the Document object with reflection from raw
            }};

            manager.collection.updateOne(
                    new Document(manager.fieldID, this.id),
                    new Document("$set", values),
                    new UpdateOptions().upsert(true)
            );
            // and we can now update

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Allows to retrieve the values of the document in the form of a chosen structure (non-existent fields will be ignored without causing an error)
     *
     * @return the class structure
     */
    public final <D> D getRaw (final Class<D> structure) {
        final D res = manager.getEmptyRaw(structure);
        final Document doc = manager.collection.find(new Document(manager.fieldID, id)).first();

        if (doc != null) { // if the document exist before,
            try {
                for (final Field field : structure.getFields()) {
                    if (doc.containsKey(field.getName()))
                        field.set(res, doc.get(field.getName()));
                    // set fields values from document values
                }
            } catch (final IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return res;
    }


    // _________________________________________________________________________________________________________________


    /**
     * Allows to update several fields of a document thanks to the variadic formatting
     * @param values The key/value pairs at update
     */
    public final void update (final Object... values) {
        if (values.length % 2 == 0) {
            // if the values are of even number ( so each key is linked to its value )

            final Document toModify = new Document() {{
                for (int ind = 0; ind < values.length; ind += 2)
                    append(String.valueOf(values[ind]), values[ind + 1]);
                    // we set the document from values given
            }};

            manager.collection.updateOne(
                    new Document(manager.fieldID, this.id),
                    new Document("$set", toModify)
            );
            // and update
        }
    }

    /**
     * Allows to update several fields of a document using a map
     * @param values The key/value pairs at update
     */
    public final void update (final Map<String, Object> values) {
        final Document toModify = new Document(values);
        // we set the document from values given

        manager.collection.updateOne(
                new Document(manager.fieldID, this.id),
                new Document("$set", toModify)
        );
        // and update
    }

    /**
     * Allows to update a field only of a document
     *
     * @param key name of the field
     * @param value value of the field
     */
    public final void update (final String key, final Object value) {
        manager.collection.updateOne(
                new Document(manager.fieldID, this.id),
                new Document("$set", new Document(key, value))
        );
    }

    // _________________________________________________________________________________________________________________


    /**
     * Allows to increment [or decrement] a field.
     *
     * The negative value will result in a decrement.
     *
     * @param key name of the field.
     * @param value increment value, can be negative to reverse for a decrement
     */
    public final void increment (final String key, final Object value) {
        manager.collection.updateOne(
                new Document(manager.fieldID, this.id),
                new Document("$inc", new Document(key, value))
        );
    }

    /**
     * Allows to increment [or decrement] several fields in a request thanks to a Map.
     * Negative values will result in decrementations
     *
     * @param values field / value pair [increment / decrement]
     */
    public final void increment (final Map<String, Object> values) {
        if (values.size() > 0) {
            final Document doc = new Document(values);

            manager.collection.updateOne(
                    new Document(manager.fieldID, this.id),
                    new Document("$inc", doc)
            );
        }
    }

    /**
     * Allows to increment [or decrement] several fields in a query thanks to a variadic argument
     *
     * Two consecutive values match a pair
     * key -> value
     *
     * Negative values will result in decrementations
     *
     * @param values field / value pair [increment / decrement]
     */
    public final void increment (final Object... values) {
        if (values.length >= 2 && values.length%2 == 0) {
            // if the values is of an even number ( so key->value )

            final Document toModify = new Document() {{
                for (int ind=0; ind<values.length; ind+=2)
                    append(String.valueOf(values[ind]), values[ind+1]);
            }};

            manager.collection.updateOne(
                    new Document(manager.fieldID, this.id),
                    new Document("$inc", toModify)
            );
        }
    }

    // _________________________________________________________________________________________________________________

    /**
     * Allows you to add elements to an array
     * @param key the field representing an array in the DB
     * @param value the value to add
     */
    public final void push (final String key, final Object value) {
        manager.collection.updateOne(
                new Document(manager.fieldID, this.id),
                new Document("$push", new Document(key, value)),
                new UpdateOptions().upsert(true)
        );
    }

    /**
     * Allows to add a document to an array
     * @param key the field representing an array in the DB
     * @param value the document to add
     */
    public final void push (final String key, final Document value) {
        manager.collection.updateOne(
                new Document(manager.fieldID, this.id),
                new Document("$push", new Document(key, new Document(value))),
                new UpdateOptions().upsert(true)
        );
    }

    /**
     * Allows you to remove a selected element from an array
     * @param key the field representing an array in the DB
     * @param values the value to remove
     */
    public final void pull (final String key, final Object values) {
        manager.collection.updateOne(
                new Document(manager.fieldID, this.id),
                new Document("$pull", new Document(key, values))
        );
    }

    /**
     * Allows to remove a selected document from an array
     * @param key the field representing an array in the DB
     * @param value the document to remove
     */
    public final void pull (final String key, final Document value) {
        manager.collection.updateOne(
                new Document(manager.fieldID, this.id),
                new Document("$pull", new Document(key, value))
        );
    }

    /**
     * Allows you to remove all elements from an array
     * @param key the field representing an array in the DB
     */
    public final void pullAll (final String key) {
        manager.collection.updateOne(
                new Document(manager.fieldID, this.id),
                new Document("$set", new Document(key, "[]"))
        );
    }


    /**
     * Allows you to retrieve a list from a field
     * @param key the field representing an array in the DB
     * @return the requested list
     */
       public final List<?> getList (final String key) {
          final Document resq = manager.collection.find(new Document(manager.fieldID, this.id)).first();
          return (resq != null) ? (ArrayList<?>) resq.getList(key, Object.class) : new ArrayList<>();
      }

    /**
     * Allows you to retrieve a list of String from a field
     * @param key the field representing an array in the DB
     * @return the requested list of type String
     */
     public final List<String> getStringList (final String key) {
          final Document resq = manager.collection.find(new Document(manager.fieldID, this.id)).first();
          return (resq != null) ? resq.getList(key, String.class) : new ArrayList<>();
      }

    /**
     * Allows to retrieve documents from a list
     * @param key the field representing an array in the DB
     * @return the requested list of type document
     */
      public final List<Document> getListAsDocument (final String key) {
          final Document resq = manager.collection.find(new Document(manager.fieldID, this.id)).first();
          return (resq != null) ? resq.getList(key, Document.class) : new ArrayList<>();
      }

    /**
     * Allows to remove an element from a list by its index
     * @param key the field representing an array in the DB
     * @param index the index of the element concerned
     */
    public final void pullIndex (final String key, final int index) {
        manager.collection.updateOne(
                new Document(manager.fieldID, this.id),
                new Document("$unset", new Document(key + "." + index, 1))
        );

        manager.collection.updateOne(
                new Document(manager.fieldID, this.id),
                new Document("$pull", new Document(key, null))
        );
    }
}