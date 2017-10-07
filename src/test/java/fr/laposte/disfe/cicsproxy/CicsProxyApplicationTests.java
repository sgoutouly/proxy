package fr.laposte.disfe.cicsproxy;

import org.junit.BeforeClass;
import org.junit.Test;

import rx.Observable;

//@RunWith(SpringRunner.class)
//@SpringBootTest
public class CicsProxyApplicationTests {

	@Test
	public void switchIfEmpty() {
		
		System.out.println(
			Observable.just("1")
			.map(s -> s + "1")
			
			.map(s -> s + "1")
			.map(s -> s + "1")
			.flatMap(s -> Observable.<String>empty())
			.map(s -> Integer.parseInt(s))
			.switchIfEmpty(Observable.just(1))
			.toBlocking()
			.singleOrDefault(null)
			);
		
		
	}
	
	
	
	@Test
	public void proxyShouldStart()  throws InterruptedException {
		/*new CommareaProxy(
			new Initializer(
				new CacheHandler(CouchbaseCluster
					.create()
					.openBucket("CTG")), 
				new FrontendHandler())
			).start();*/
		
		new Client();
	}
	
	
/*	@BeforeClass
	public static void setup() {
		new Server();
	}*/
	
	

}
