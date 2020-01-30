package nl.mpi.oai.harvester.utils;

/**
 * Basic harvest statistic class
**/
public class Statistic {

    private long records = 0;
    private long requests = 0;

    private long harvestStartTime;

    public Statistic(){
        harvestStartTime = System.currentTimeMillis();
    }

    public void incRecordCount(){
        records++;
    }

    public void incRequestCount(){
        requests++;
    }

    public long getHarvestedRecords() {
        return  records;
    }
    public long getRequests() {
        return requests;
    }
    public long getHarvestTime() {
        long harvestFinishTime = System.currentTimeMillis();
        return (harvestFinishTime - harvestStartTime)/1000;
    }
}
