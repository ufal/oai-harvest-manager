package nl.mpi.oai.harvester;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@XmlRootElement
public class ResumeDetails {
    private static final Logger logger = LogManager.getLogger(ResumeDetails.class);
    public String resumptionToken;
    public int pIndex;
    public int sIndex;
    public List<String> prefixes;

    public static Optional<ResumeDetails> load(Path path) {
        if(Files.exists(path)){
            return Optional.of(JAXB.unmarshal(path.toFile(), ResumeDetails.class));
        }
        return Optional.empty();
    }

    public void persist(Path file) {
        try {
            Files.createDirectories(file.getParent());
            JAXB.marshal(this, file.toFile());
        } catch (IOException e) {
            logger.error(e);
        }
    }

}
