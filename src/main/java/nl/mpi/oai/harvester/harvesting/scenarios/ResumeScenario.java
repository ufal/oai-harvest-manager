package nl.mpi.oai.harvester.harvesting.scenarios;

import nl.mpi.oai.harvester.Provider;
import nl.mpi.oai.harvester.harvesting.AbstractListHarvesting;
import nl.mpi.oai.harvester.harvesting.OAIFactory;
import nl.mpi.oai.harvester.metadata.MetadataFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ResumeScenario extends Scenario {
    private static final Logger logger = LogManager.getLogger(ResumeScenario.class);
    private final Scenario scenario;

    ResumeScenario(Scenario scenario) {
        super(scenario.provider, scenario.actionSequence);
        this.scenario = scenario;
    }

    @Override
    AbstractListHarvesting createHarvesting(List<String> prefixes, OAIFactory oaiFactory, MetadataFactory metadataFactory) {
        logger.debug("ResumeScenario.createHarvesting3");
        final AbstractListHarvesting harvesting = scenario.createHarvesting(prefixes, oaiFactory, metadataFactory);
        final Provider.ResumeDetails details = provider.getResumeDetails();
        harvesting.setpIndex(details.pIndex);
        harvesting.setsIndex(details.sIndex);
        harvesting.setPrefixes(details.prefixes);
        harvesting.setResumptionToken(details.resumptionToken);
        return harvesting;
    }

    @Override
    boolean doGetRecords(AbstractListHarvesting harvesting) {
        logger.debug("ResumeScenario.doGetRecords1");
        return scenario.doGetRecords(harvesting);
    }
}
