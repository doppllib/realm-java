package io.realm.examples.intro;
import android.os.AsyncTask;
import android.util.Log;

import java.util.Iterator;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.examples.intro.model.Cat;
import io.realm.examples.intro.model.Dog;
import io.realm.examples.intro.model.Person;

/**
 * Created by kgalligan on 1/28/17.
 */

public class IntroExamplePresenter
{
    private Realm realm;
    private final IntroExamplePresenterHoster host;

    public IntroExamplePresenter(IntroExamplePresenterHoster host)
    {
        this.host = host;
    }

    public void init()
    {

        // Create the Realm instance
        realm = Realm.getDefaultInstance();

        basicCRUD(realm);
        basicQuery(realm);
        basicLinkQuery(realm);

        // More complex operations can be executed on another thread.
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                String info;
                info = complexReadWrite();
                info += complexQuery();
                return info;
            }

            @Override
            protected void onPostExecute(String result) {
                showStatus(result);
            }
        }.execute();
    }

    public void shutdown()
    {
        realm.close();
    }

    private void showStatus(String txt) {
//        Log.i("IntroExamplePresenter", txt);
        host.showStatus(txt);
//        TextView tv = new TextView(this);
//        tv.setText(txt);
//        rootLayout.addView(tv);
    }


    private void basicCRUD(Realm realm) {
        showStatus("Perform basic Create/Read/Update/Delete (CRUD) operations...");

        // All writes must be wrapped in a transaction to facilitate safe multi threading
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                // Add a person
                Person person = realm.createObject(Person.class);
                person.setId(1);
                person.setName("Young Person");
                person.setAge(14);

            }
        });

        // Find the first person (no query conditions) and read a field
        final Person person = realm.where(Person.class).findFirst();
        showStatus(person.getName() + ":" + person.getAge());

        // Update person in a transaction
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                person.setName("Senior Person");
                person.setAge(99);
                showStatus(person.getName() + " got older: " + person.getAge());
            }
        });

        // Delete all persons
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.delete(Person.class);
            }
        });
    }

    private void basicQuery(Realm realm) {
        showStatus("\nPerforming basic Query operation...");
        showStatus("Number of persons: " + realm.where(Person.class).count());

        RealmResults<Person> results = realm.where(Person.class).equalTo("age", 99).findAll();

        showStatus("Size of result set: " + results.size());
        showStatus("All vals: "+ realm.where(Person.class).count());
    }

    private void basicLinkQuery(Realm realm) {
        showStatus("\nPerforming basic Link Query operation...");
        showStatus("Number of persons: " + realm.where(Person.class).count());

        RealmResults<Person> results = realm.where(Person.class).equalTo("cats.name", "Tiger").findAll();

        showStatus("Size of result set: " + results.size());
    }

    private String complexReadWrite() {
        String status = "\nPerforming complex Read/Write operation...";

        // Open the default realm. All threads must use its own reference to the realm.
        // Those can not be transferred across threads.
        Realm realm = Realm.getDefaultInstance();

        // Add ten persons in one transaction
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Dog fido = realm.createObject(Dog.class);
                fido.name = ("fido");
                for (int i = 0; i < 10; i++) {
                    Person person = realm.createObject(Person.class);
                    person.setId(i);
                    person.setName("Person no. " + i);
                    person.setAge(i);
                    person.setDog(fido);

                    // The field tempReference is annotated with @Ignore.
                    // This means setTempReference sets the Person tempReference
                    // field directly. The tempReference is NOT saved as part of
                    // the RealmObject:
                    person.setTempReference(42);

                    for (int j = 0; j < i; j++) {
                        Cat cat = realm.createObject(Cat.class);
                        cat.name = ("Cat_" + j);
                        person.getCats().add(cat);
                    }
                }

                Cat magicCat = realm.createObject(Cat.class);
                magicCat.name = ("magicCatName");
            }
        });

        // Implicit read transactions allow you to access your objects
        status += "\nNumber of persons: " + realm.where(Person.class).count();

        // Iterate over all objects
        for (Person pers : realm.where(Person.class).findAll()) {
            String dogName;
            if (pers.getDog() == null) {
                dogName = "None";
            } else {
                dogName = pers.getDog().name;
            }
            status += "\n" + pers.getName() + ":" + pers.getAge() + " : " + dogName + " : " + pers.getCats().size();
            Iterator<Cat> iterator = pers.getCats().iterator();
            int catCount = 0;
            while(iterator.hasNext())
            {
                Cat cat = iterator.next();
                status += "\n\tcat"+ (++catCount) +": "+ cat.name;
            }
        }

        status += "\nCats!";

        for(Cat cat : realm.where(Cat.class).findAll())
        {
            status += "\n"+ cat.name;
        }

        // Sorting
        RealmResults<Person> sortedPersons = realm.where(Person.class).findAllSorted("age", Sort.DESCENDING);
        status += "\nSorting " + sortedPersons.last().getName() + " == " + realm.where(Person.class).findFirst()
                .getName();

        realm.close();
        return status;
    }

    private String complexQuery() {
        String status = "\n\nPerforming complex Query operation...";

        Realm realm = Realm.getDefaultInstance();
        status += "\nNumber of persons: " + realm.where(Person.class).count();

        // Find all persons where age between 7 and 9 and name begins with "Person".
        RealmResults<Person> results = realm.where(Person.class)
                .between("age", 7, 9)       // Notice implicit "and" operation
                .beginsWith("name", "Person").findAll();
        status += "\nSize of result set: " + results.size();

        realm.close();
        return status;
    }
}