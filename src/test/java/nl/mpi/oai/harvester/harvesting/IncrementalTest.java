package nl.mpi.oai.harvester.harvesting;

import nl.mpi.Utilities;
import nl.mpi.oai.harvester.Provider;
import nl.mpi.oai.harvester.action.ActionSequence;
import nl.mpi.oai.harvester.control.Configuration;
import nl.mpi.oai.harvester.control.Main;
import nl.mpi.oai.harvester.control.Util;
import nl.mpi.oai.harvester.harvesting.scenarios.Scenario;
import nl.mpi.oai.harvester.harvesting.scenarios.ScenarioFactory;
import nl.mpi.oai.harvester.metadata.MetadataFactory;
import nl.mpi.oai.harvester.metadata.MetadataFormat;
import nl.mpi.oai.harvester.utils.DocumentSource;
import nl.mpi.oai.harvester.utils.Statistic;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXB;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class IncrementalTest {
    private static final String wd = "target/it/IncrementalTest";
    public static final String NAME = "XXXTest_name";

    @BeforeClass
    public static void setup(){
        Main.config = new Configuration(){
            @Override
            public String getWorkingDirectory(){
                return wd;
            }
        };
    }

    @Before
    @After
    public void cleanup() throws IOException {
        Utilities.deleteRecursive(Path.of(wd));
    }

    @Test(expected = HackToStopExecution.class)
    public void selectiveHarvestingWithDatestamps() throws ParserConfigurationException, IOException, ParseException {

        final String expectedFromDate = "2020-01-01";
        Statistic currentStatistic = new Statistic();
        currentStatistic.setDateGathered(expectedFromDate);

        final Path file = Paths.get(wd, "last_successful_harvest_stats", Util.toFileFormat(NAME));
        Files.createDirectories(file.getParent());
        StringWriter sw = new StringWriter();
        JAXB.marshal(currentStatistic, sw);
        Files.writeString(file, sw.toString());

        Provider p = new Provider("bogus", 1, new int[] {});
        p.setName(NAME);
        p.setIncremental(true);
        p.init();

        ListHarvesting l = new ListHarvesting(new OAIFactory(), p, List.of("oai_dc"), new MetadataFactory()) {
            @Override
            DocumentSource verb2(String metadataPrefix, String resumptionToken, int timeout) throws IOException, ParserConfigurationException, SAXException, TransformerException, NoSuchFieldException, XMLStreamException {
                throw new UnsupportedOperationException();
            }

            @Override
            DocumentSource verb5(String endpoint, String fromDate, String untilDate, String metadataPrefix, String set, int timeout, Path temp) throws IOException, ParserConfigurationException, SAXException, TransformerException, NoSuchFieldException, XMLStreamException {
                assertEquals("verb5 is not called with the expected fromDate", expectedFromDate, fromDate);
                throw new HackToStopExecution();
            }

            @Override
            String getToken() throws TransformerException, NoSuchFieldException {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean processResponse(DocumentSource document) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object parseResponse() {
                throw new UnsupportedOperationException();
            }
        };
        l.request();

    }

    private static class HackToStopExecution extends RuntimeException{}
}
