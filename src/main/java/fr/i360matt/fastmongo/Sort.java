package fr.i360matt.fastmongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

/**
 * Cette classe permet d'effectuer un classement de Document par rapport à un ou plusieurs fields
 * @author 360matt
 */
public final class Sort {
    public enum Direction {
        CROISSANT, DECROISSANT
    }

    private FindIterable<Document> iter;
    private final CollectionManager manager;
    private int limit;

    public Sort (final CollectionManager manager) {
        this.manager = manager;
    }

    public final Sort setLimit (final int limit) {
        this.limit = limit;
        return this;
    }

    public final Sort setRule (final String... fields) {
        final Bson choice = Sorts.ascending(fields);
        this.iter = (this.iter == null) ? manager.collection.find().sort(choice) : this.iter.sort(choice);
        return this;
    }

    public final Sort setRule (final Direction direction, final String... fields) {
        final Bson choice = (direction.equals(Direction.CROISSANT)) ? Sorts.ascending(fields) : Sorts.descending(fields);
        this.iter = (this.iter == null) ? manager.collection.find().sort(choice) : this.iter.sort(choice);
        return this;
    }

    // _________________________________________________________________________________________________________________

    public final FindIterable<Document> getIterable () {
        return (limit == 0) ? iter : iter.limit(limit);
    }

    public final List<Document> getDocuments () {
        final List<Document> res = new ArrayList<>();
        getIterable().iterator().forEachRemaining(res::add);
        return res;
    }

    public final <D> List<D> getRaws (final Class<D> structure) {
        final List<D> res = new ArrayList<>();
        getIterable().iterator().forEachRemaining(x -> res.add(manager.getRawFromDocument(x, structure)));
        return res;
    }


}