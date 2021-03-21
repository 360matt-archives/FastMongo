package fr.i360matt.fastmongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to classify a Document in relation to one or more fields
 * @author 360matt
 */
public final class Sort implements Closeable {
    public enum Direction {
        ASCENDING, DESCENDING
    }

    private FindIterable<Document> iter;
    private final CollectionManager manager;
    private int limit;

    /**
     * Allows to create a classification that can be completed later
     * @param manager The manager of the collection where the classification is supposed to take place
     */
    public Sort (final CollectionManager manager) {
        this.manager = manager;
    }

    /**
     * Allows to define the maximum number of results allowed at the output
     * @param limit The number of documents to classify
     * @return The current instance
     */
    public final Sort setLimit (final int limit) {
        this.limit = limit;
        return this;
    }

    private Sort addRule (final boolean ascending, final String... fields) {
        final Bson choice = (ascending) ? Sorts.ascending(fields) : Sorts.descending(fields);
        this.iter = (this.iter == null) ? manager.collection.find().sort(choice) : this.iter.sort(choice);
        return this;
    }

    /**
     * Allows to add a ascending ranking rule
     * @param fields The fields you want to add as criteria
     * @return The current instance
     */
    public final Sort ascending (final String... fields) {
        return addRule(true, fields);
    }

    /**
     * Allows to add a descending ranking rule
     * @param fields The fields you want to add as criteria
     * @return The current instance
     */
    public final Sort descending (final String... fields) {
        return addRule(false, fields);
    }

    // _________________________________________________________________________________________________________________

    public final FindIterable<Document> getIterable () {
        return (limit == 0) ? iter : iter.limit(limit);
    }

    public final List<Document> getDocuments () {
        final List<Document> res = new ArrayList<>();
        getIterable().iterator().forEachRemaining(res::add);
        close();
        return res;
    }

    public final <D> List<D> getRaws (final Class<D> structure) {
        final List<D> res = new ArrayList<>();
        getIterable().iterator().forEachRemaining(x -> res.add(manager.getRawFromDocument(x, structure)));
        close();
        return res;
    }

    public void close () {
        iter = null;
    }

}