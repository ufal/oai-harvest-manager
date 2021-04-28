package nl.mpi.oai.harvester.harvesting.scenarios;

import nl.mpi.oai.harvester.Provider;
import nl.mpi.oai.harvester.action.ActionSequence;
import nl.mpi.oai.harvester.harvesting.AbstractListHarvesting;
import nl.mpi.oai.harvester.harvesting.OAIFactory;
import nl.mpi.oai.harvester.harvesting.OAIHelper;
import nl.mpi.oai.harvester.harvesting.RecordListHarvesting;
import nl.mpi.oai.harvester.metadata.Metadata;
import nl.mpi.oai.harvester.metadata.MetadataFactory;
import nl.mpi.oai.harvester.utils.DocumentSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DirectScenario extends Scenario {
    private static final Logger logger = LogManager.getLogger(DirectScenario.class);
    private IdGenerator idGenerator = new TimestampIdGenerator();

    DirectScenario(Provider provider, ActionSequence actionSequence) {
        super(provider, actionSequence);
    }

    /**
     * <br>Get metadata records directly, that is without first obtaining a list
     * of identifiers pointing to them <br><br>
     *
     * @param harvesting harvester
     * @return false on parser or input output error
     */
    private boolean listRecords(AbstractListHarvesting harvesting) {

        DocumentSource records;

        do {
            try {

                if (provider.isExclusive()) {
                    exclusiveLock.writeLock().lock();
                } else {
                    exclusiveLock.readLock().lock();
                }

                //TODO cleanup - this should be true or it throws; at least with ListHarvesting
                if (!harvesting.request()) {
                    return false;
                } else {
                    records = harvesting.getResponse();
                    //TODO cleanup - records can't be null the above throws if it is; at least with ListHarvesting
                    if (records == null) {
                        return false;
                    } else {
                        String idSuffix = idGenerator.nextId();

                        Metadata metadata = harvesting.getMetadataFactory().create(
                                provider.getName() + "-" + idSuffix,
                                OAIHelper.getPrefix(records),
                                records, this.provider, true, true);

                        // apply the action sequence to the records
                        actionSequence.runActions(metadata);

                        // cleanup
                        metadata.close();
                    }
                }
                /* Check if in principle another response would be
                   available.
                 */
            } finally {
                if (provider.isExclusive()) {
                    exclusiveLock.writeLock().unlock();
                } else {
                    exclusiveLock.readLock().unlock();
                }
            }
        } while (harvesting.requestMore());

        return true;
    }



    @Override
    AbstractListHarvesting createHarvesting(List<String> prefixes, OAIFactory oaiFactory, MetadataFactory metadataFactory) {
        logger.debug("DirectScenario.createHarvesting3");
        return new RecordListHarvesting(oaiFactory,
                provider, prefixes, metadataFactory);
    }

    @Override
    boolean doGetRecords(AbstractListHarvesting harvesting){
        logger.debug("DirectScenario.doGetRecords1");
        // get the records with ListRecords
        boolean done = listRecords(harvesting);
        logger.debug("list records -> done[" + done + "]");
        return done;
    }

    // TODO fix the tests properly; this is a convenience method to bring back the old id's the ScenarioTest needs
    public void switchToNumberIdGenerator(){
        idGenerator = new NumberIdGenerator();
    }

    interface IdGenerator {
        String nextId();
    }
    private static class NumberIdGenerator implements IdGenerator {
        private int n = 0;
        @Override
        public String nextId() {
            return String.format("%07d", n++);
        }
    }
    private static class TimestampIdGenerator implements IdGenerator{
        private static final SimpleDateFormat TIMESTAMP = new SimpleDateFormat("yyyyMMddHHmmssSSS");

        @Override
        public String nextId() {
            return TIMESTAMP.format(new Date());
        }
    }
}
