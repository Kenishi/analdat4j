package ai.eve.stores;

import static org.junit.Assert.*;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.DockerClient.ListImagesParam;
import com.spotify.docker.client.DockerClient.RemoveContainerParam;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.ImageSearchResult;
import com.spotify.docker.client.messages.PortBinding;
import com.spotify.docker.client.messages.ProgressMessage;

public class ParseStoreIntegrationTest {
	final private static String DOCKER_URI = "https://192.168.99.100:2376";
	final private static String DOCKER_CERTS = "/Users/Kei/.docker/machine/machines/local/";
	
	private static String mongoId;
	private static String parseId;
	private static String appId = "mytestid";
	private static String masterKey = "mymasterekey";
	private static String restKey = "myrestkey";
	
	@BeforeClass
	public static void beforeAll() throws DockerCertificateException, DockerException, InterruptedException {
		setupTempParseServer();
	}
	
	@AfterClass
	public static void afterAll() throws DockerCertificateException, DockerException, InterruptedException {
		tearDownTempParseServer();
	}
	
	@Before
	public void setUp() throws Exception {
		Properties props = new Properties();
		props.setProperty("store.class", "ai.eve.stores.ParseStore");
		props.setProperty("store.parse.appId", appId);
		props.setProperty("store.parse.master", masterKey);
		props.setProperty("store.parse.key", restKey);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDispatch() throws InterruptedException {
		
	}
	
	private static void setupTempParseServer() throws DockerCertificateException, DockerException, InterruptedException {
		DockerClient client = DefaultDockerClient.builder()
				.uri(DOCKER_URI)
				.dockerCertificates(new DockerCertificates(Paths.get(DOCKER_CERTS)))
				.build();
				
		if(needsMongoImage(client)) {
			System.out.println("Grabbing mongo");
			client.pull("mongo", new ProgressHandler() {
				
				@Override
				public void progress(ProgressMessage msg) throws DockerException {
					System.out.println("mongo :" + msg.status() + ": " + msg.progress());
				}
			});
		}
		if(needsParseImage(client)) {
			System.out.println("Grabbing parse server");
			client.pull("yongjhih/parse-server", new ProgressHandler() {
				@Override
				public void progress(ProgressMessage msg) throws DockerException {
					System.out.println(msg.status() + "(" + msg.id() + ")" + ": " + msg.progress());
					if(msg.error() != null) {
						System.err.println(msg.error());
					}
				}
			});
			
		}
		System.out.println("Creating containers");
		
		Map<String, List<PortBinding>> portMap = new HashMap<>();
		portMap.put("27017/tcp", Arrays.asList(new PortBinding[]{PortBinding.create("0.0.0.0", "27017")}));
		ContainerConfig config = ContainerConfig.builder()
				.image("mongo")
				.hostConfig(HostConfig.builder()
						.portBindings(portMap)
						.build())
				.exposedPorts("27017/tcp")
				.build();
		ContainerCreation creation = client.createContainer(config, "integtest-mongo");
		mongoId = creation.id();
		
		portMap = new HashMap<>();
		portMap.put("1337/tcp", Arrays.asList(new PortBinding[]{PortBinding.create("0.0.0.0", "1337")}));
		config = ContainerConfig.builder()
				.image("yongjhih/parse-server")
				.hostConfig(HostConfig.builder()
						.portBindings(portMap)
						.links("integtest-mongo:integtest-mongo")
						.build())
				.exposedPorts("1337/tcp")
				.env("APP_ID=" + appId, "REST_API_KEY=" + restKey, "MASTER_KEY=" + masterKey)
				.build();
		creation = client.createContainer(config, "integtest-parse-server");
		parseId = creation.id();
		
		client.startContainer(mongoId);
		client.startContainer(parseId);
	}
	
	private static void tearDownTempParseServer() throws DockerCertificateException, DockerException, InterruptedException {
		DockerClient client = DefaultDockerClient.fromEnv().build();
		RemoveContainerParam removeVol = RemoveContainerParam.removeVolumes(true);
		client.stopContainer(parseId, 0);
		client.stopContainer(mongoId, 0);
		client.removeContainer(parseId, removeVol);
		client.removeContainer(mongoId, removeVol);
	}
	
	private static boolean needsMongoImage(DockerClient client) throws DockerException, InterruptedException {
		List<Image> results = client.listImages();
		System.out.println(results.toString());
		return results.size() <= 0;
	}
	
	private static boolean needsParseImage(DockerClient client) throws DockerException, InterruptedException {
		List<Image> results = client.listImages(ListImagesParam.byName("yongjhih/parse-server"));
		System.out.println(results.toString());
		return results.size() <= 0;
	}

}
