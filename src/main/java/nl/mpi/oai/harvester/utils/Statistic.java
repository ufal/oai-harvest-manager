package nl.mpi.oai.harvester.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/**
 * Basic harvest statistic class
**/
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Statistic {
    private static final Logger logger = LogManager.getLogger(Statistic.class);

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    private String dateGathered;

    private long records = 0;
    private long requests = 0;
    private long deleted = 0;

    private long harvestStartTime;

    public Statistic(){
        this(new Date());
    }

    public Statistic(Date date){
        harvestStartTime = System.currentTimeMillis();
        dateGathered = formatter.format(date);
    }

    public void incRecordCount(int increment){
        records += increment;
    }

    public void incRequestCount(){
        requests++;
    }

    @XmlElement
    public long getHarvestedRecords() {
        return  records;
    }

    public void setHarvestedRecords(long records){
        this.records = records;
    }

    @XmlElement
    public long getRequests() {
        return requests;
    }

    public void setRequests(long requests){
        this.requests = requests;
    }

    @XmlElement(name="harvestTimeSec")
    public long getHarvestTime() {
        long harvestFinishTime = System.currentTimeMillis();
        return (harvestFinishTime - harvestStartTime)/1000;
    }

    public void setDateGathered(String date) throws ParseException {
        final Date d = formatter.parse(date);
        dateGathered = formatter.format(d);
    }

    @XmlElement
    public String getDateGathered(){
        return dateGathered;
    }

    @XmlElement
    public long getDeleted(){
        return deleted;
    }

    public void setDeleted(long deleted){
        this.deleted = deleted;
    }

    public void persist(Path file){
        try {
            Files.createDirectories(file.getParent());
            JAXB.marshal(this, file.toFile());
        } catch (IOException e) {
            logger.error(e);
        }

    }

    public static Optional<Statistic> load(Path path) {
        if(Files.exists(path)){
            return Optional.of(JAXB.unmarshal(path.toFile(), Statistic.class));
        }
        return Optional.empty();
    }

    public void incDeletedCount() {
        deleted++;
    }
}
