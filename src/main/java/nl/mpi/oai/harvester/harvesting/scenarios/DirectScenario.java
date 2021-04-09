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

import java.util.List;

public class DirectScenario extends Scenario {
    private static final Logger logger = LogManager.getLogger(DirectScenario.class);

    DirectScenario(Provider provider, ActionSequence actionSequence) {
        super(provider, actionSequence);
    }

    /**
     * <br>Get metadata records directly, that is without first obtaining a list
     * of identifiers pointing to them <br><br>
     *
     * In this scenario, a save action specified before a strip action is
     * interpreted to apply the the response of the GetRecords verb. Also, the
     * presence of a strip action in the sequence, is interpreted to apply to
     * the response also. Since the sequence of actions will be applied to an
     * individual record, in the sequence both will be disabled.
     *
     * @param harvesting harvester
     * @return false on parser or input output error
     */
    private boolean listRecords(AbstractListHarvesting harvesting) {

        DocumentSource records;

        Integer n = 0;

        do {
            try {

                if (provider.isExclusive()) {
                    exclusiveLock.writeLock().lock();
                } else {
                    exclusiveLock.readLock().lock();
                }

                if (!harvesting.request()) {
                    return false;
                } else {
                    records = harvesting.getResponse();
                    if (records == null) {
                        return false;
                    } else {
                        String id;
                        id = String.format("%07d", n);

                        Metadata metadata = harvesting.getMetadataFactory().create(
                                provider.getName() + "-" + id,
                                OAIHelper.getPrefix(records),
                                records, this.provider, true, true);

                        n++;

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
    boolean doGetRecords(List<String> prefixes, OAIFactory oaiFactory, MetadataFactory metadataFactory) {
        return doGetRecords(new RecordListHarvesting(oaiFactory,
                provider, prefixes, metadataFactory));
    }

    @Override
    boolean doGetRecords(AbstractListHarvesting harvesting){
        // get the records with ListRecords
        boolean done = listRecords(harvesting);
        logger.debug("list records -> done[" + done + "]");
        return done;
    }
}
