# :boom: FastMongo [![BCH compliance](https://bettercodehub.com/edge/badge/360matt/FastMongo?branch=master)](https://bettercodehub.com/)

Being able to manipulate documents in the form of a personalized structure thanks to reflection.  
Create any class, add as many fields to it as you want, set defaults to whatever you want, and you can finally use it to fetch and define documents. 

### Why use this API ?
* :bulb: As simple as possible, it is easy to learn
* :hourglass: Its use is very fast, even the migration
* :art: It is customizable, you can define many behavior in this API
* :floppy_disk: Your data is better structured
* :vertical_traffic_light: You can develop your project and your structure


## :star: Beginning:
1. You must define the connection to the database:
```java
MongoIntegration.connect(new MongoIntegration.Auth() {{
        this.host = ;
        this.port = ;
        this.user = ;
        this.password = ;
        this.database = ;
}});
```
2. If at any time you want to close the connection:
```java
MongoIntegration.disconnect();
```
3. You can cycle connect/disconnect as many times. But you can only connect to one database at a time for this API version 

## :zzz: Legacy references:
```java
MongoIntegration.client // static field
MongoIntegration.database // static field
CollectionManager#collection // instance field related of collection
Element#getDocument() // get legacy document of Element
```

## :zap: Collection Manager:
* The collection is created in the DB each time CollectionManager is instantiated
```java
 // getting Collection manager
CollectionManager man = new CollectionManager("collection name") {{

        updateStructure(Kangourou.class);
        // Optional: allows you to define new fields without deleting old ones from all existing documents 
        
        autoInsert(Kangourou.class);
        // Optional: allows to auto-insert the document with default values upon instantiation of Element
        
        setFieldID("UUID");
        // Optional: By default, IDs are mapped by the field _id, but you can use any other field name 
        
}};
```

### :hammer: Features:
* It is possible to classify documents by order according to one or more fields:  
:warning: you can use any class you want, the return type adjusts itself.  
Only existing fields will be modified, without errors, but be reasonable on the utility.

You can return structures:
```java
List<Kangourou> topPrice = man.buildSort("price")
        .setLimit(20) // limit to 20 elements
        .getRaws(Kangourou.class); // recover as structure
```

You can return documents:
```java
List<Document> topPrice = man.buildSort("price")
        .setLimit(20) // limit to 20 elements
        .getDocuments(); // recover as legacy document
```

You can sort multiple fields:
```java
Sort currentSort = man.buildSort("price", "age");
// croissant sort

Sort currentSort = man.buildSort(Sort.Direction.DECROISSANT, "price", "age");
// decroissant sort



currentSort.setRule(Sort.Direction.CROISSANT, "one", "two", "three"); // croissant sort
currentSort.setRule(Sort.Direction.DECROISSANT, "one", "two", "three"); // decroissant sort
// It is possible to add classification rules after instantiation


List<Kangourou> topPriceWithBetterAge = currentSort.getRaws(Kangourou.class);
// recover as structure
List<Document> topPriceWithBetterAge = currentSort.getDocument();
// recover as legacy document
```

* You can check if document exist by id:  
:warning: Set by default, "_id", you can change the name of the field id with setFieldID( NAME ) in manager initialiser.
```java
booelan state = man.exist( "name" );
```

* Get empty default structure (Utils):
```java
Kangourou emptyWithDefault = man.getEmptyRaw( Kangourou.class )
```

## :unlock: Element (represents a document):
The elements each represent a document whether it is fictitious or not.  
it is thanks to an element instance that we can handle a document in the DB (create, modify, delete) 

There are two ways to get an Element instance:
* Using the element's constructor directly (less popular but still possible):
```java
Element element = new Element("name of document by ID", managerOfAnyCollection);
// Element represents a document (fictive or not) with chosen id and collection
```
* Using the collection manager:
```java
Element element = managerOfAnyCollection.getObject("name of document by ID");
// Element represents a document (fictive or not) in collection with chosen id
```

### Features:
* Get structure with current value from DB:
```java
Kangourou struct = element.getRaw(Kangourou.class);
```
* Set structure to create/edit the document:  
:warning: The other unedited fields appearing in the reset structure are those of the document  
Those which are not present in the structure but present in the document will be kept intact

:information_source: You can also change name/id of document
```java
element.setRaw(new Kangourou() {{
  this.anyField = "newer value";
}});
```
* Update document without structures:
```java
element.update("key", "value");
// update single key

element.update(
  "key", "value",
  "key_2", "value_2"
);
// update multiples keys with variadic

element.update( new HashMap() {{
  put("key", "value");
}});
// update with hashmap
```
* Increment / Decrement one or multiple fields:
```java
element.increment("key", 3);
// will do +3

element.increment("key", -5);
// will do -5

element.increment(
  "key_1", 3,
  "key_2", -5
)
// [in/de]crement multiple fields

element.increment(new HashMap() {{
  put("key", 3);
}});
// will do +3
```
* Get legacy document:
```java
element.getDocument();
```
* Get list List<?> from field:
```java
List<?> list = element.getList("predators");
// You can try to cast after that
```
* Get string list List<String> from field:  
:information_source: is safety
```java
List<String> list = element.getStringList(" field name ");
```
* Get document list List<Document> from field:
```java
List<Document> document = element.getListAsDocument(" field name" );
```
* Push (add) entrie to a list:
```java
element.push(" field ", new AnyObject());
// the value can be any type

element.push(" field ", new Document());
// the document will be parsed
```
* Pull (remove) entrie from a list:
```java
element.pull(" field ", new AnyObject());
// remove this object if exist

element.pull(" field ", new Document());
// can remove document from array/list

element.pullIndex(" field ", 0);
// can remove by index (here, the first element)

element.pullAll(" field ");
// can remove all entries from list
```

### Extra:
* You can retrieve the collection manager from a Element instance:
```java
element.manager // is the collection manager
```
* You can retrieve the ID of Element/Document:
```java
element.id
```

## :innocent: For more help:
* Here the Javadoc you can download: https://github.com/360matt/FastMongo/blob/master/javadoc  
* You can also add me on Discord: Matteow#6953

### :ghost: About Me:
* I am a 16 year old French developer.
* I started around 12 years old with basic PHP, then around 14 years old I was doing Discord bots in JS with NodeJS.
* I finally started Java around 15 (so it's been over a year), and my experience has improved over time (see the rest of my Github) 
