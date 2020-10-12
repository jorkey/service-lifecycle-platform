package distribution;

import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestMongo {
    public static void main(String[] args) throws Exception {
        //MongoClient client = MongoClients.create(MongoClientSettings.builder()
        //        .applyToClusterSettings(builder -> builder.serverSelectionTimeout(1, TimeUnit.SECONDS))
        //        .build());
        MongoClient client = MongoClients.create();
        MongoDatabase db = client.getDatabase("test");

        CountDownLatch latch = new CountDownLatch(1);
        db.listCollectionNames().subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(final Subscription s) {
                System.out.println("onSubscribe is called");
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(final String s) {
                System.out.println(s);
            }

            @Override
            public void onError(final Throwable throwable) {
                System.out.println("onError is called");
                throwable.printStackTrace();
                latch.countDown();
            }

            @Override
            public void onComplete() {
                System.out.println("Complete is called");
                latch.countDown();
            }
        });

        System.out.println("Wait finished with " + latch.await(60, TimeUnit.SECONDS));
    }
}
