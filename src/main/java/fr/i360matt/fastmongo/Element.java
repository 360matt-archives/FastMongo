package fr.i360matt.fastmongo;

import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class représentant et gérant un object de la collection
 * @author 360matt
 */
public final class Element {

    public final String id;
    public final CollectionManager manager;

    public Element (final String id, final CollectionManager manager) {
        this.id = id;
        this.manager = manager;

        if (!MongoIntegration.cache.containsKey(id)) {
            MongoIntegration.cache.put(manager.name + "#" + id, false);
            defineDefaultSchema();
        }
    }


    /**
     * Permet de créer le document s'il n'existe pas
     */
    protected final void defineDefaultSchema () {
        if (manager.autoInsert && manager.defaultDocument != null) {
            // if (collection.count(new Document("_id", id)) == 0) {
            // si le document n'existe pas dans la collection

            try {
                manager.collection.updateOne(
                        new Document("_id", this.id),
                        new Document("$setOnInsert", manager.defaultDocument),
                        new UpdateOptions().upsert(true)
                );
                // update du document dans la bdd
            } catch (final Exception e) {
                e.printStackTrace();
            }
            // }
        }
    }

    // _________________________________________________________________________________________________________________


    /**
     * Permet de définir des fields via un document
     * @param document le document en question
     */
    public final void setDocument (final Document document) {
        manager.collection.updateOne(
                new Document("_id", this.id),
                new Document("$set", document),
                new UpdateOptions().upsert(true)
        );
        // update du document dans la bdd
    }

    /**
     * Permet de récupérer le document sous sa forme originale
     * @return le document en question
     */
    public final Document getDocument () {
        return manager.collection.find(new Document("_id", id)).first();
    }


    // _________________________________________________________________________________________________________________


    /**
     * Permet de mettre à jour les données du document
     * @param raw les données à modifier en Raw
     */
    public final <D> void setRaw (final D raw) {
        try {
            final Document values = new Document("_id", this.id) {{
                for (final Field field : raw.getClass().getFields())
                    append(field.getName(), field.get(raw));
                // définition du Document à partir du Raw
            }};

            manager.collection.updateOne(
                    new Document("_id", this.id),
                    new Document("$set", values),
                    new UpdateOptions().upsert(true)
            );
            // update du document dans la bdd

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Permet de récupérer toute la structure d'un document
     *
     * @return structure de l'élément 'id' de l'instance
     */
    public final <D> D getRaw (final Class<D> structure) {
        final D res = manager.getEmptyRaw(structure);
        final Document doc = manager.collection.find(new Document("_id", id)).first();

        if (doc != null) { // check de sécurité si le document existe.
            try {
                for (final Field field : structure.getFields()) {
                    if (doc.containsKey(field.getName()))
                        field.set(res, doc.get(field.getName()));
                    // importation des valeurs des fields du Raw vide
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        return res;
    }


    // _________________________________________________________________________________________________________________


    /**
     * Permet d'update plusieurs fields
     * d'un document grâce au formatage variadique
     *
     * @param values Les couples key/value à update
     */
    public final void update (final Object... values) {
        if (values.length % 2 == 0) {
            // si 'values' est paire ( donc key->value )

            final Document toModify = new Document() {{
                for (int ind = 0; ind < values.length; ind += 2)
                    append(String.valueOf(values[ind]), values[ind + 1]);
            }};

            manager.collection.updateOne(
                    new Document("_id", this.id),
                    new Document("$set", toModify)
            );
        }
    }

    /**
     * Permet d'update plusieurs fields
     * d'un document grâce à une Map
     *
     * @param values Les couples key/value à update
     */
    public final void update (final Map<String, Object> values) {
        final Document toModify = new Document();

        values.forEach(toModify::append);

        manager.collection.updateOne(
                new Document("_id", this.id),
                new Document("$set", toModify)
        );
    }

    /**
     * Permet d'update un field seulement d'un document
     *
     * @param key nom du field
     * @param value valeur du field
     */
    public final void update (final String key, final Object value) {
        manager.collection.updateOne(
                new Document("_id", this.id),
                new Document("$set", new Document(key, value))
        );
    }

    // _________________________________________________________________________________________________________________


    /**
     * Permet d'incrémenter [ou décrémenter] un field.
     *
     * La valeur négative aboutiera à une décrémentation.
     *
     * @param key nom du field
     * @param value valeur d'incrémentation,
     *              peut être négatif pour
     *              inverser pour une décrémentation
     */
    public final void increment (final String key, final Object value) {
        manager.collection.updateOne(
                new Document("_id", this.id),
                new Document("$inc", new Document(key, value))
        );
    }

    /**
     * Permet d'incrémenter [ou décrémenter] plusieurs fields en une requête
     * Grâce a une Map<Str , Nb>
     *
     * Les valeurs négatifs aboutieront à des décrémentations
     *
     * @param values couple field / valeur [ incrémentation / décrémentation ]
     */
    public final void increment (final Map<String, Number> values) {
        if (values.size() > 0) {
            final Document doc = new Document() {{
                values.forEach(this::append);
            }};

            manager.collection.updateOne(
                    new Document("_id", this.id),
                    new Document("$inc", doc)
            );
        }
    }

    /**
     * Permet d'incrémenter [ou décrémenter] plusieurs fields en une requête
     * Grâce à un arg variadique.
     *
     * Deux valeurs à la suite correspondent au couple
     * key -> value
     *
     * Les valeurs négatifs aboutieront à des décrémentations
     *
     * @param values couple field / valeur [ incrémentation / décrémentation ]
     */
    public final void increment (final Object... values) {
        if (values.length >= 2 && values.length%2 == 0) {
            // si 'values' est paire ( donc key->value )

            final Document toModify = new Document() {{
                for (int ind=0; ind<values.length; ind+=2)
                    append(String.valueOf(values[ind]), values[ind+1]);
            }};

            manager.collection.updateOne(
                    new Document("_id", this.id),
                    new Document("$inc", toModify)
            );
        }
    }

    // _________________________________________________________________________________________________________________

    /**
     * Permet d'ajouter des éléments à un array
     * @param key le field étant un array
     * @param values les valeurs à ajouter
     */
    public final void push (final String key, final Object values) {
        manager.collection.updateOne(
                new Document("_id", this.id),
                new Document("$push", new Document(key, values)),
                new UpdateOptions().upsert(true)
        );
    }

    /**
     * Permet d'ajouter des éléments à un array
     * @param key le field étant un array
     * @param values les documents à ajouter
     */
    public final void push (final String key, final Document values) {
        manager.collection.updateOne(
                new Document("_id", this.id),
                new Document("$push", new Document(key, new Document(values))),
                new UpdateOptions().upsert(true)
        );
    }

    /**
     * Permet de retirer des éléments choisis d'un array
     * @param key le field étant un array
     * @param values les valeurs à retirer
     */
    public final void pull (final String key, final Object values) {
        manager.collection.updateOne(
                new Document("_id", this.id),
                new Document("$pull", new Document(key, values))
        );
    }

    /**
     * Permet de retirer des documents choisis d'un array
     * @param key le field étant un array
     * @param values les documents à retirer
     */
    public final void pull (final String key, final Document values) {
        manager.collection.updateOne(
                new Document("_id", this.id),
                new Document("$pull", new Document(key, values))
        );
    }

    /**
     * Permet de retirer tous les éléments d'un array
     * @param key le field étant un array
     */
    public final void pullAll (final String key) {
        manager.collection.updateOne(
                new Document("_id", this.id),
                new Document("$set", new Document(key, "[]"))
        );
    }


    /**
     * Permet de récupérer une liste à partir d'un field
     * @param key field/key de la liste
     * @return la liste demandée
     */
       public final List<?> getList (final String key) {
          final Document resq = manager.collection.find(new Document("_id", this.id)).first();
          return (resq != null) ? (ArrayList<?>) resq.getList(key, Object.class) : new ArrayList<>();
      }

    /**
     * Permet de récupérer une liste à partir d'un field
     * @param key field/key de la liste
     * @return la liste demandée
     */
     public final List<String> getStringList (final String key) {
          final Document resq = manager.collection.find(new Document("_id", this.id)).first();
          return (resq != null) ? resq.getList(key, String.class) : new ArrayList<>();
      }

    /**
     * Permet de récupérer les éléments d'une liste
     * Sous forme de document
     * @param key field/key de la liste
     * @return la liste demandée
     */
      public final List<Document> getListAsDocument (final String key) {
          final Document resq = manager.collection.find(new Document("_id", this.id)).first();
          return (resq != null) ? resq.getList(key, Document.class) : new ArrayList<>();
      }


    public final void pullIndex (final String key, final int index) {
        manager.collection.updateOne(
                new Document("_id", this.id),
                new Document("$unset", new Document(key + "." + index, 1))
        );

        manager.collection.updateOne(
                new Document("_id", this.id),
                new Document("$pull", new Document(key, null))
        );
    }
}