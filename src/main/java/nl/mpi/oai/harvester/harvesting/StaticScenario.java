package nl.mpi.oai.harvester.harvesting;

import nl.mpi.oai.harvester.StaticProvider;
import nl.mpi.oai.harvester.action.ActionSequence;
import nl.mpi.oai.harvester.metadata.MetadataFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class StaticScenario extends Scenario {
    private static final Logger logger = LogManager.getLogger(StaticScenario.class);

    public StaticScenario(StaticProvider provider, ActionSequence actionSequence) {
        super(provider, actionSequence);
    }

    public List<String> getMetadataFormats(OAIFactory oaiFactory){
        // set type of format harvesting to apply
        AbstractHarvesting harvesting = new StaticPrefixHarvesting(
                oaiFactory,
                (StaticProvider) provider,
                actionSequence);
        logger.debug("harvesting["+harvesting+"]");

        // get the prefixes
        List<String> prefixes = getPrefixes(harvesting);
        logger.debug("prefixes["+prefixes+"]");
        return prefixes;
    }

    public boolean getRecords(String method, OAIFactory oaiFactory, MetadataFactory metadataFactory){
        // list of prefixes provided by the endpoint
        List<String> prefixes = getMetadataFormats(oaiFactory);
        if (prefixes.isEmpty()) {
            // no match
            logger.debug("no prefixes[" + prefixes + "] -> done");
            return false;
        }
        boolean done;
        // set type of record harvesting to apply
        StaticRecordListHarvesting harvesting = new StaticRecordListHarvesting(oaiFactory,
                (StaticProvider) provider, prefixes, metadataFactory);

        // get the records
        if (method.equals("ListIdentifiers")) {
            done = listIdentifiers(harvesting);
            logger.debug("list identifiers -> done["+done+"]");
        } else {
            done = listRecords(harvesting);
            logger.debug("list records -> done["+done+"]");
        }
        return done;
    }
}
