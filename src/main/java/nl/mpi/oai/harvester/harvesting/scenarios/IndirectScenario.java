package nl.mpi.oai.harvester.harvesting.scenarios;

import nl.mpi.oai.harvester.Provider;
import nl.mpi.oai.harvester.action.ActionSequence;
import nl.mpi.oai.harvester.harvesting.AbstractListHarvesting;
import nl.mpi.oai.harvester.harvesting.IdentifierListHarvesting;
import nl.mpi.oai.harvester.harvesting.OAIFactory;
import nl.mpi.oai.harvester.metadata.Metadata;
import nl.mpi.oai.harvester.metadata.MetadataFactory;
import nl.mpi.oai.harvester.utils.DocumentSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class IndirectScenario extends Scenario {
    private static final Logger logger = LogManager.getLogger(IndirectScenario.class);

    IndirectScenario(Provider provider, ActionSequence actionSequence) {
        super(provider, actionSequence);
    }

    /**
     * Get metadata records indirectly, that is by first obtaining a list of
     * identifiers pointing to them <br><br>
     *
     * @param harvesting harvester
     * @return false on parser or input output error
     */
    private boolean listIdentifiers(AbstractListHarvesting harvesting) {

        DocumentSource identifiers;

        for (;;) {
            try {

                if (provider.isExclusive()) {
                    exclusiveLock.writeLock().lock();
                } else {
                    exclusiveLock.readLock().lock();
                }

                if (!harvesting.request()) {
                    return false;
                } else {
                    identifiers = harvesting.getResponse();

                    if (identifiers == null) {
                        return false;
                    } else {
                        if (!harvesting.processResponse(identifiers)) {
                            // something went wrong, no identifiers for this endpoint
                            return false;
                        } else {
                            // received response

                            if (!harvesting.requestMore()) {
                                // finished requesting
                                break;
                            }
                        }
                    }
                }
            } finally {
                if (provider.isExclusive()) {
                    exclusiveLock.writeLock().unlock();
                } else {
                    exclusiveLock.readLock().unlock();
                }
            }
        }

        /* Iterate over the list of pairs, for each pair, get the record it
           identifies.
         */
        while(!harvesting.fullyParsed()) {
            try {

                if (provider.isExclusive()) {
                    exclusiveLock.writeLock().lock();
                } else {
                    exclusiveLock.readLock().lock();
                }

                Metadata record = (Metadata) harvesting.parseResponse();

                if (record == null) {
                    // something went wrong, skip the record
                } else {
                    // apply the action sequence to the record
                    actionSequence.runActions(record);
                    record.close();
                }

            } finally {
                if (provider.isExclusive()) {
                    exclusiveLock.writeLock().unlock();
                } else {
                    exclusiveLock.readLock().unlock();
                }
            }
        }

        return true;
    }

    @Override
    AbstractListHarvesting createHarvesting(List<String> prefixes, OAIFactory oaiFactory, MetadataFactory metadataFactory) {
        logger.debug("IndirectScenario.createHarvesting3");
        return new IdentifierListHarvesting(oaiFactory,
                provider, prefixes, metadataFactory);
    }

    @Override
    boolean doGetRecords(AbstractListHarvesting harvesting){
        logger.debug("IndirectScenario.doGetRecords1");
        // get the records indirectly, first obtaining identifiers
        boolean done = listIdentifiers(harvesting);
        logger.debug("list identifiers -> done["+done+"]");
        return done;
    }
}
