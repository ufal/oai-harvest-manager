package nl.mpi.oai.harvester.action;

import nl.mpi.oai.harvester.Provider;
import nl.mpi.oai.harvester.metadata.Metadata;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class StripActionTest {
    String xml = "<record><header status=\"deleted\"><identifier>oai:ufal-point-dev.ufal.hide.ms.mff.cuni" +
            ".cz:11858/00-097C-0000-0001-48FA-2</identifier><datestamp>2017     -04-10T13:34:17Z</datestamp><setSpec>hdl_11858_00-097C-0000-0001-486F-D</setSpec><setSpec>hdl_123456789_3198</setSpec><setSpec>hdl_11858_00-097C-0000-0001-4877-A</setSpec></header></record>";

    @Test
    public void performOnStreamRespectsDeleted() throws ParserConfigurationException {
        List<Metadata> records = new ArrayList<>();
        String id = "test_id";
        String prefix = "oai_dc";
        Provider provider = new Provider("https://example.com", 1, new int[] {1});

        ByteArrayInputStream docStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        Metadata md = new Metadata(id, prefix, docStream, provider, false, false);
        records.add(md);

        Action strip = new StripAction();
        strip.perform(records);
        assertEquals("Performing on deleted only should pass on empty list", 0, records.size());
        assertEquals("There should be a deleted record", 1, provider.deletedCount());
    }

    @Test
    public void performOnDocRespectsDeleted() throws ParserConfigurationException, IOException, SAXException {
        List<Metadata> records = new ArrayList<>();
        String id = "test_id";
        String prefix = "oai_dc";
        Provider provider = new Provider("https://example.com", 1, new int[] {1});

        final Document doc = provider.db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        Metadata md = new Metadata(id, prefix, doc, provider, false, false);
        records.add(md);

        Action strip = new StripAction();
        strip.perform(records);
        assertEquals("Performing on deleted only should pass on empty list", 0, records.size());
        assertEquals("There should be a deleted record", 1, provider.deletedCount());
    }
}