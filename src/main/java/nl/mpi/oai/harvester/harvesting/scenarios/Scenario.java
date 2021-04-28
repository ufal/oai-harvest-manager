/*
 * Copyright (C) 2015, The Max Planck Institute for
 * Psycholinguistics.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * A copy of the GNU General Public License is included in the file
 * LICENSE-gpl-3.0.txt. If that file is missing, see
 * <http://www.gnu.org/licenses/>.
 */

package nl.mpi.oai.harvester.harvesting.scenarios;

import nl.mpi.oai.harvester.Provider;
import nl.mpi.oai.harvester.action.ActionSequence;
import nl.mpi.oai.harvester.harvesting.AbstractListHarvesting;
import nl.mpi.oai.harvester.harvesting.FormatHarvesting;
import nl.mpi.oai.harvester.harvesting.OAIFactory;
import nl.mpi.oai.harvester.metadata.MetadataFactory;
import nl.mpi.oai.harvester.utils.DocumentSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Scenarios for harvesting
 *
 * @author Kees Jan van de Looij (Max Planck Institute for Psycholinguistics)
 */
public abstract class Scenario {

    private static final Logger logger = LogManager.getLogger(Scenario.class);

    //
    final Provider provider;

    //
    final ActionSequence actionSequence;

    //
    static final ReadWriteLock exclusiveLock = new ReentrantReadWriteLock(true);

    Scenario (Provider provider, ActionSequence actionSequence) {
        this.provider = provider;
        this.actionSequence = actionSequence;
    }

    /**
     * <br>Get the list of metadata prefixes supported by the endpoint<br><br>
     *
     * The list created is based on the format specified in the configuration.
     *
     * @param harvesting harvester
     * @return false on parser or input output error
     */
    public List<String> getPrefixes(FormatHarvesting harvesting){

        //
        DocumentSource document;

        // list of prefixes provided by the endpoint
        final List<String> prefixes = new ArrayList<>();

        if (harvesting.request()) {
            // everything went fine
            document = harvesting.getResponse();

            if (document != null){
                if (harvesting.processResponse(document)) {
                    // received response
                    if (harvesting.fullyParsed()) {
                        // no matches
                        logger.info("No matching prefixes for format "
                                + actionSequence.getInputFormat());
                        return prefixes;
                    }
                    // get the prefixes
                    while (!harvesting.fullyParsed()) {
                        String prefix = (String) harvesting.parseResponse();
                        if (prefix != null) {
                            prefixes.add(prefix);
                        }
                    }
                }
            }
        }

        /* If there are no matches, return an empty list. In this case the
           action sequence needs to be terminated. A succeeding action
           sequence could then provide a match.
         */
        return prefixes;
    }

    public List<String> getMetadataFormats(OAIFactory oaiFactory){
        // set type of format harvesting to apply
        FormatHarvesting harvesting = new FormatHarvesting(oaiFactory,
                provider, actionSequence);

        // get the prefixes
        List<String> prefixes = this.getPrefixes(harvesting);
        logger.debug("prefixes["+prefixes+"]");
        return prefixes;
    }

    public final boolean getRecords(OAIFactory oaiFactory, MetadataFactory metadataFactory){
        // list of prefixes provided by the endpoint
        List<String> prefixes = getMetadataFormats(oaiFactory);
        if (prefixes.isEmpty()) {
            // no match
            logger.debug("no prefixes[" + prefixes + "] -> done");
            return false;
        }

        AbstractListHarvesting harvesting = createHarvesting(prefixes, oaiFactory, metadataFactory);
        boolean done = doGetRecords(harvesting);

        if(provider.getIncremental()) {
            logger.warn("Synchronization of deleted records will currently only work with providers having PERSISTENT" +
                    " deletion mode.");
            // TODO synchronization should be run only if shouldHarvestIncrementally
            // we should keep the prefixes & sets, as ListIdentifiers is prefix dependent
            //FileSynchronization.execute(provider);
        }
        return done;

    }

    abstract AbstractListHarvesting createHarvesting(List<String> prefixes, OAIFactory oaiFactory, MetadataFactory metadataFactory);
    abstract boolean doGetRecords(AbstractListHarvesting harvesting);
}

