package nl.mpi.oai.harvester.control;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import nl.mpi.Utilities;
import nl.mpi.oai.harvester.Provider;
import org.junit.*;

import javax.xml.bind.JAXB;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.junit.Assert.*;

public class MainIT {

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(8009);

    @Rule
    public WireMockClassRule wireMockInstanceRule = wireMockRule;

    @Before
    public void setUp() throws Exception {
        //Also see the other mappings in resources/mappings
        final ResponseDefinitionBuilder identityXslResponse = aResponse().withBody(
                Files.readString(
                        Paths.get(
                                getClass().getClassLoader().getResource("identity.xsl").toURI()))
        );
        //set up mock www server;
        // to make the config search for identity.xsl easier
        stubFor(get(urlEqualTo("/xslts/identity.xsl"))
                .atPriority(1)
                .willReturn(identityXslResponse));
        // to provide a different transform action
        stubFor(get(urlEqualTo("/xslts/identity2.xsl"))
                .atPriority(1)
                .willReturn(identityXslResponse));
        // provide static repository
        stubFor(get(urlPathMatching("/oai-pmh/static-repo.xml"))
                .atPriority(1)
                .willReturn(aResponse().withBody(
                        Files.readString(
                                Paths.get(
                                        getClass().getClassLoader().getResource("static-repo.xml").toURI()), StandardCharsets.ISO_8859_1)
                )));
        stubFor(get("/dspace5l/oai/request?verb=ListRecords&resumptionToken=oai_dc%2F%2F%2F%2F100")
                .atPriority(1)
                .willReturn(serviceUnavailable())
        );

//        startRecording(
//            recordSpec()
//                    .forTarget("http://ufal-point-dev.ufal.hide.ms.mff.cuni.cz/")
//                    .onlyRequestsMatching(getRequestedFor(urlPathMatching("/dspace5l/.*")))
//                    .allowNonProxied(true)
//                    .ignoreRepeatRequests()
//        );

    }

//    @After
//    public void tearDown(){
//        final SnapshotRecordResult snapshotRecordResult = stopRecording();
//        final List<StubMapping> stubMappings = snapshotRecordResult.getStubMappings();
//        System.out.println(stubMappings);
//    }

    @Test
    public void testIt() throws URISyntaxException {
        //There are no asserts here; but the whole thing should not throw an exception
        // Also running a test enables assertions in code (-ea) so in fact this does some testing
        final String configOnDisk = Utilities.getConfig("config/test-config-it.xml");
        Main.main(new String[] {configOnDisk});
    }

    @Test
    public void errorTokenSaved() throws URISyntaxException, IOException {
        final Path tokenPath = Paths.get("target/it/workspace2/tokens/fake_resume");
        Files.deleteIfExists(tokenPath);
        final String configOnDisk = Utilities.getConfig("config/test-resume-with-token.xml");
        Main.main(new String[]{configOnDisk});
        //there's 503 simulating an issue
        final StringReader reader = new StringReader(Files.readString(tokenPath));
        final Provider.ResumeDetails details = JAXB.unmarshal(reader, Provider.ResumeDetails.class);
        assertEquals( "oai_dc////100", details.resumptionToken);
    }

    @Test
    public void resumeWhenThereIsATokenSaved() throws IOException, URISyntaxException {
        final String workspace = "target/it/workspace2";
        //Pretend there is a resumption token stored
        Path tokenPath = prepareResumeTest(workspace, "fake_resume");
        Path tokenPath2 = prepareResumeTest(workspace, "fake_resume2");

        final String configOnDisk = Utilities.getConfig("config/test-resume-with-token.xml");
        // the harvest should start with the resumption token
        Main.main(new String[]{configOnDisk});
        //test this exists and is the only file
        Path testRecordPath = Paths.get(workspace, "oai-rec/fake_resume" +
                "/oai_ufal_point_dev_ufal_hide_ms_mff_cuni_cz_11234_5_XXX_TEST.xml");
        assertTrue(String.format("File '%s' should exist", testRecordPath), Files.exists(testRecordPath));
        assertEquals( 1L, Files.list(testRecordPath.getParent()).count());
        //test cleanup on success
        assertFalse("Token file should be removed on success", Files.exists(tokenPath));

        // verify identifier harvesting
        testRecordPath = Paths.get(workspace, "oai-rec/fake_resume2" +
                "/oai_ufal_point_dev_ufal_hide_ms_mff_cuni_cz_11234_5_XXX_TEST.xml");
        assertTrue(String.format("File '%s' should exist", testRecordPath), Files.exists(testRecordPath));
        assertEquals( 1L, Files.list(testRecordPath.getParent()).count());
        assertFalse("Token file should be removed on success", Files.exists(tokenPath2));

    }

    final String resumptionToken = "ABC001";
    private Path prepareResumeTest(String workspace, String provider) throws IOException {
        final Path tokenPath = Paths.get(workspace, "tokens", provider);
        Files.createDirectories(tokenPath.getParent());

        //store the token
        final Provider.ResumeDetails details = new Provider.ResumeDetails();
        details.resumptionToken = resumptionToken;
        details.prefixes = List.of("oai_dc");
        details.pIndex = 0;
        details.sIndex = 0;
        JAXB.marshal(details, tokenPath.toFile());
        return tokenPath;
    }

    @After
    public void cleanup() throws IOException {
       Utilities.deleteRecursive(Path.of("target/it"));
    }
}
