package nl.mpi.oai.harvester.control;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import nl.mpi.Utilities;
import nl.mpi.oai.harvester.Provider;
import org.junit.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

public class WorkerIT {
    private static Configuration config;

    private String workspace = "target/it/workspace3";

    @BeforeClass
    public static void setup() throws URISyntaxException, ParserConfigurationException, SAXException,
            XPathExpressionException, IOException {
        Main.setSystemProperties();
        final String configOnDisk = Utilities.getConfig("config/worker-test.xml");
        config = new Configuration();
        config.readConfig(configOnDisk);
        config.applyTimeoutSetting();
    }

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(8009);

    @Rule
    public WireMockClassRule wireMockInstanceRule = wireMockRule;

    @Before
    public void setUp() throws Exception {
        stubFor(get("/dspace5l/oai/request?verb=ListRecords&set=hdl_11858_00-097C-0000-0023-8C33-2&metadataPrefix=cmdi")
                .atPriority(1)
                .willReturn(serviceUnavailable())
        );
    }

    @Before
    @After
    public void cleanup() throws IOException {
        Utilities.deleteRecursive(Path.of(workspace));
    }

    @Test
    public void runShouldNotContinueOnNetworkErrors() throws InterruptedException, IOException {
        final Provider provider = config.getProviders().get(0);

        Worker w = new Worker(provider, config);
        final Thread t = new Thread(w);
        t.start();
        t.join();
        final Path oaiDcResults = Paths.get(workspace, "oai-pmh/worker_test");
        assertEquals("oai_dc wasn't supposed to be harvested; error on cmdi should have stopped that",
                0, countFilesInDir(oaiDcResults));


    }

    private long countFilesInDir(Path path) throws IOException {
        if(Files.exists(path)){
            return Files.list(path).count();
        }else{
            return 0L;
        }
    }
}
