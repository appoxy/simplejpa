#SimpleJPA's new home.#

Discussion Group: http://groups.google.com/group/simplejpa

##Introduction##
Here's how to get started using SimpleJPA.

##Dependencies##
Starting with version 1.5 SimpleJPA switched to use Amazon's Java SDK. Use the latest releases of (I'll try to package these up into the release if I can figure out the licensing compatability):

commons-lang - You can grab all commons libs at http://commons.apache.org/downloads/index.html
commons-collections
commons-logging
commons-codec
cglib-nodep
kitty-cache
ehcache
scannotation
javassist (required for scannotation)
ejb3-persistence-api
AWS SDK for Java
Apache HttpClient
When building from source and using the Maven pom.xml file Kitty-cache will need to be added explicitly as a reference.

Dependencies for versions pre 1.5
commons-lang - You can grab all commons libs at http://commons.apache.org/downloads/index.html
commons-beanutils
commons-collections
commons-logging (required for jets3t)
commons-codec (required for jets3t)
Apache HttpClient - (required for jets3t)
typica
jets3t
cglib-nodep - http://sourceforge.net/project/showfiles.php?group_id=56933
ejb3-persistence-api - http://mirrors.ibiblio.org/pub/mirrors/maven2/org/hibernate/ejb3-persistence/1.0.2.GA/
scannotation - http://scannotation.sourceforge.net/
javassist (scannotation) - http://www.csg.is.titech.ac.jp/~chiba/javassist/
kitty-cache - http://code.google.com/p/kitty-cache/
ehcache - http://ehcache.org/


##Setup##
Create a file called simplejpa.properties and put on the classpath. Add your Amazon access key and secret key like:

accessKey = AAAAAAAAAAAAAAAAAAAAAAA
secretKey = SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS
For more configuration options, see Configuration.

Now the Code
Create an EntityManagerFactory
// Create EntityManagerFactory. This should be a global object that you reuse.
private static EntityManagerFactoryImpl factory = new EntityManagerFactoryImpl("persistenceUnitName", null);
Get EntityManager's from the Factory
// Get an EntityManager from the factory. This is a short term object that you'll use for some processing then throw away
EntityManager em = factory.createEntityManager();
##Persisting an object##
Lets create a very simple object to store.

@Entity
public class MyTestObject {
    private String id;
    private String name;

    @Id
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
   
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
Now to persist it:

MyObject ob = new MyObject();
ob.setName("Scooby doo");
em.persist(ob);
That's it!

##Querying##
See JPAQuery

##Deleting##
MyObject ob...
em.remove(ob);
Close the EntityManager when you're done
This is done after you've completed a set of tasks, such as displaying a web page. It ensures that caches get cleaned up and no memory gets wasted.

em.close();
Close the EntityManagerFactory before you shutdown your app
factory.close();
##What Next?##
See all the JPA features currently supported.
Cast your EntityManager to SimpleEntityManager to get more advanced features like asynchronous operations.
Use our ready to go BaseClasses so you can write less code
