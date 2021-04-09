package nl.mpi.oai.harvester.harvesting.scenarios;

import nl.mpi.oai.harvester.StaticProvider;
import nl.mpi.oai.harvester.action.ActionSequence;
import nl.mpi.oai.harvester.harvesting.*;
import nl.mpi.oai.harvester.metadata.MetadataFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.util.List;

public class StaticScenario extends Scenario {
    private static final Logger logger = LogManager.getLogger(StaticScenario.class);
    private final Scenario scenario;

    StaticScenario(Scenario scenario){
        super(scenario.provider, scenario.actionSequence);
        this.scenario = scenario;
    }

    @Override
    public List<String> getMetadataFormats(OAIFactory oaiFactory){
        // set type of format harvesting to apply
        FormatHarvesting harvesting = new StaticPrefixHarvesting(
                oaiFactory,
                (StaticProvider) provider,
                actionSequence);
        logger.debug("harvesting["+harvesting+"]");

        // get the prefixes
        List<String> prefixes = getPrefixes(harvesting);
        logger.debug("prefixes["+prefixes+"]");
        return prefixes;
    }

    @Override
    AbstractListHarvesting createHarvesting(List<String> prefixes, OAIFactory oaiFactory, MetadataFactory metadataFactory) {
        logger.debug("StaticScenario.createHarvesting3");
        return new StaticRecordListHarvesting(oaiFactory,
                (StaticProvider) provider, prefixes, metadataFactory);
    }

    @Override
    boolean doGetRecords(AbstractListHarvesting harvesting){
        logger.debug("StaticScenario.doGetRecords1");
        return scenario.doGetRecords(harvesting);
    }
}
