package nl.mpi.oai.harvester.harvesting.scenarios;

import nl.mpi.oai.harvester.ResumeDetails;
import nl.mpi.oai.harvester.harvesting.AbstractListHarvesting;
import nl.mpi.oai.harvester.harvesting.OAIFactory;
import nl.mpi.oai.harvester.metadata.MetadataFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * This scenario attempts to resume interrupted harvest.
 *
 * It assumes the matching prefixes or sets didn't change, between the runs.
 * It assumes only one action sequence actually gets to fetching records. We don't know the action sequence the
 * stored prefixes belong to. But if it's not the first one, the preceding were not compatible (ie. the harvest
 * didn't start).
 * The stats probably won't be correct after resume
 */
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
        if(scenario instanceof IndirectScenario) {
            //TODO a) we don't store tindex b) see the comments in MainIT#resumeWhenThereIsATokenSaved
            logger.error("Currently it's not possible to resume ListIdentifiers scenarios. Restarting harvest.");
            return harvesting;
        }
        final ResumeDetails details = provider.getResumeDetails();
        // TODO resume and stats
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
