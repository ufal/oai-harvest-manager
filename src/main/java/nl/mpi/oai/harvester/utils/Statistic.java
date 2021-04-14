package nl.mpi.oai.harvester.utils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Basic harvest statistic class
**/
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Statistic {

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    private String dateGathered;

    private long records = 0;
    private long requests = 0;

    private long harvestStartTime;

    public Statistic(){
        this(new Date());
    }

    public Statistic(Date date){
        harvestStartTime = System.currentTimeMillis();
        dateGathered = formatter.format(date);
    }

    public void incRecordCount(){
        records++;
    }

    public void incRequestCount(){
        requests++;
    }

    @XmlElement
    public long getHarvestedRecords() {
        return  records;
    }
    @XmlElement
    public long getRequests() {
        return requests;
    }
    @XmlElement
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
}
