import com.github.fakemongo.Fongo;
import fr.i360matt.fastmongo.CollectionManager;
import fr.i360matt.fastmongo.Element;
import fr.i360matt.fastmongo.MongoIntegration;

import java.util.List;

/**
 * Class test permettant à la fois de tester
 * et servant de démonstration
 * @author 360matt
 */
public class TestFastMongo {

    public static class Kangourou {
        String test = "booong";
    }

    public static class Cat {
        String test = "miaou";
        String a_newer_field = "<3";
    }

    public static class Dog {
        String truc = "wooof";
    }

    public static void main (final String[] args) throws InterruptedException {


        final Fongo fongo = new Fongo("one");
        MongoIntegration.client = fongo.getMongo();
        MongoIntegration.database = fongo.getDatabase("test");
        MongoIntegration.setAvailable(true);
        // END of creating fake Mongo server



        // You should connect to mongo like that
        /*
        MongoIntegration.connect(new MongoIntegration.Auth() {{
            this.host = ;
            this.port = ;
            this.user = ;
            this.password = ;
            this.database = ;
        }});
         */



        // getting Collection manager
        CollectionManager man = new CollectionManager("collection name") {{
            updateStructure(Kangourou.class);
            autoInsert(Kangourou.class); // if document should be created on instantiate Element()
            // default schema
        }};

        // getting a document where _id equals "Hello"
        Element a = man.getObject("Hello");
        Element b = man.getObject("Hey");
        b.setRaw(new Dog() {{
            truc = "Wouesh";
        }});


        Cat cat = a.getRaw(Cat.class); // Store data to a Cat instance
        Dog dog = a.getRaw(Dog.class); // Or a Dog instance

        // update document by Kangouru instance, so, "test" field have value "super value"
        a.setRaw(new Kangourou() {{
            this.test = "super value";
        }});


        // Ranking dogs by price
        List<Dog> dogs = man.buildSort("_id")
                .setLimit(10) // top 10
                .getRaws(Dog.class); // store as Dog object (so only Dog fields)


        dogs.forEach(x -> {
            System.out.println(x.truc);
        });






    }

}
