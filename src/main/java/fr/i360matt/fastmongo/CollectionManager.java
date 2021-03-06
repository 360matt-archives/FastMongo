package fr.i360matt.fastmongo;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import java.lang.reflect.Field;

/**
 * Cette classe permet de gérer une collection
 * @author 360matt
 */
public class CollectionManager {

    /**
     * Permet de récupérer le manager de la collection dans le cache
     * @param name nom de la collection
     * @param providedParams paramètres du manager
     * @return le manager concerné
     */
    public static CollectionManager getCollection (final String name, final CollectParams providedParams) {
        if (MongoIntegration.collectionCache.containsKey(name)) {
            return MongoIntegration.collectionCache.get(name);
        } else {
            return new CollectionManager(name, providedParams);
            // s'ajoutera au cache à l'instanciation [*]
        }
    }

    /**
     * Permet de récupérer le manager de la collection dans le cache
     * @param name nom de la collection
     * @return le manager concerné
     */
    public static CollectionManager getCollection (final String name) {
        return getCollection(name, new CollectParams());
    }


    public final String name;
    public final MongoCollection<Document> collection;
    public CollectParams settings;

    public Class<?> defaultTemplate;
    public Document defaultDocument;
    public boolean autoInsert;

    public CollectionManager (final String name) {
        this(name, new CollectParams());
    }

    public CollectionManager (final String name, final CollectParams params) {
        this.name = name;

        final CollectionManager candidate = MongoIntegration.collectionCache.get(name);
        if (candidate != null) {
            // si la collection est déjà dans le cache

            this.collection = candidate.collection;
            this.settings = candidate.settings;
        } else {
            if (!MongoIntegration.existCollect(name))
                MongoIntegration.createCollect(name);

            this.collection = MongoIntegration.getCollect(name);
            this.settings = params;

            // ajout au cache statique: [*]
            MongoIntegration.collectionCache.put(name, this);
        }
    }

    /**
     * Permet d'appliquer la structure choisie sur tous les documents existants
     * @param structure structure de donnée
     */
    public final void updateStructure (final Class<?> structure) {
        this.defaultTemplate = structure;
        final Document doc = new Document();

        for (final Field field : structure.getFields()) {
            doc.append(field.getName(), MongoIntegration.typeCache.get(structure));

            collection.updateMany(
                    new Document(field.getName(), new Document("$exists", false)), // patern: [ 'field' don't exist ]
                    new Document("$set", new Document(field.getName(), doc.get(field.getName())))
            );
        }
    }

    /**
     * Permet de définir si les documents doivent être créé lorsque les éléments sont instanciés
     * Avec le choix de la structure à appliquer
     * @param structure la structure par défaut
     */
    public final <D> void autoInsert (final Class<D> structure) {
        this.autoInsert = true;
        this.defaultDocument = new Document();

        for (final Field field : structure.getFields()) {
            this.defaultDocument.append(field.getName(), MongoIntegration.typeCache.get(structure));
        }
    }

    /**
     * Permet de définir des options
     * @param params options
     * @return instance de ce manager
     */
    public final CollectionManager setOptions (final CollectParams params) {
        this.settings = params;
        return this;
    }

    /**
     * Permet de récupérer les données par défauts de la structure choisie
     * @param structure structure choisie
     * @param <D> le type de la structure
     * @return les données de la structure (instance)
     */
    public final <D> D getEmptyRaw (final Class<D> structure) {
        if (!MongoIntegration.typeCache.containsKey(structure)) {
            try {
                final D res = structure.newInstance();
                MongoIntegration.typeCache.put(structure, res);
                return res;
            } catch (InstantiationException | IllegalAccessException e) {
                return null;
            }
        }
        return (D) MongoIntegration.typeCache.get(structure);
    }


    /**
     * Permet de convertir un Document en un Raw
     * @param doc document d'origine
     * @return Raw structure générée
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
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Permet de récupérer un élément
     * @param id identifiant de l'élément
     * @return l'élément en question
     */
    public final Element getObject (final String id) {
        return new Element(id, this);
    }

    /**
     * Permet de verifier si un élément existe
     * @param id identifiant de l'élément
     * @return l'état de l'existence
     */
    public final boolean exist (final String id) {
        return collection.count(new Document("_id", id)) > 0;
    }

    /**
     * Permet de supprimer l'élément
     * @param id identifiant de l'élément
     */
    public final void remove (final String id) {
        collection.deleteOne(new Document("_id", id));
    }




    // _________________________________________________________________________________________________________________

    /**
     * Permet de créer un classement par un ou plusieurs fields
     * @param fields fields choisies
     * @return une instance du module de classement
     */
    public final Sort buildSort (final String... fields) {
        return new Sort(this).setRule(fields);
    }

    /**
     * Permet de créer un classement par un ou plusieurs fields
     * @param direction l'ordre du classement
     * @param fields fields choisies
     * @return une instance du module de classement
     */
    public final Sort buildSort (final Sort.Direction direction, final String... fields) {
        return new Sort(this).setRule(direction, fields);
    }




}
