package nl.mpi.oai.harvester;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import nl.mpi.oai.harvester.control.Main;
import org.junit.*;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

public class MainIT {

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(8009);

    @Rule
    public WireMockClassRule wireMockInstanceRule = wireMockRule;

    @Before
    public void setUp() throws Exception {
        //set up mock www server;
        // to make the config search for identity.xsl easier
        stubFor(get(urlEqualTo("/xslts/identity.xsl"))
                .willReturn(aResponse().withBody(
                                Files.readString(
                                        Paths.get(
                                                getClass().getClassLoader().getResource("identity.xsl").toURI()))
                        )));
        // provide static repository
        stubFor(get(urlPathMatching("/oai-pmh/static-repo.xml"))
                .willReturn(aResponse().withBody(
                        Files.readString(
                                Paths.get(
                                        getClass().getClassLoader().getResource("static-repo.xml").toURI()), StandardCharsets.ISO_8859_1)
                )));

    }

    @Test
    public void testIt() throws URISyntaxException {
        final String configOnDisk = getConfig();
        Main.main(new String[] {configOnDisk});
    }

    private String getConfig() throws URISyntaxException {
        String configResource = "config/test-config-it.xml";
        final URL resourceURL = getClass().getClassLoader().getResource(configResource);
        final String configOnDisk = Paths.get(resourceURL.toURI()).toAbsolutePath().toString();
        return configOnDisk;
    }
}
