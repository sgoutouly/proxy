package fr.laposte.disfe.cicsproxy;

import org.junit.BeforeClass;
import org.junit.Test;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;

import rx.Observable;

public class TestEmpty {

	private static Bucket bucket;
	
	@BeforeClass
	public static void setup() {
		bucket = CouchbaseCluster.create().openBucket("CTG");
	}
	
	@Test
	public void test() {
		Observable.just("ddd")
		.flatMap(k -> bucket.async().get(k))
		.doOnNext(doc -> System.err.println(doc.toString()))
		.switchIfEmpty(Observable.<JsonDocument>error(new RuntimeException("coucou")))
		.doOnError(e -> System.err.println("coucou"))
		.onErrorResumeNext(Observable.just(JsonDocument.create("ID")))
		.toBlocking()
		.single();
	}
	
}
