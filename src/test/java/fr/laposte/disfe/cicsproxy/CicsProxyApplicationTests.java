package fr.laposte.disfe.cicsproxy;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.couchbase.client.java.CouchbaseCluster;

import fr.laposte.disfe.cicsproxy.proxy.CommareaProxy;
import fr.laposte.disfe.cicsproxy.proxy.CacheHandler;
import fr.laposte.disfe.cicsproxy.proxy.FrontendHandler;
import fr.laposte.disfe.cicsproxy.proxy.Initializer;

//@RunWith(SpringRunner.class)
//@SpringBootTest
public class CicsProxyApplicationTests {

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
	
	
	@BeforeClass
	public static void setup() {
		new Server();
	}
	
	

}
