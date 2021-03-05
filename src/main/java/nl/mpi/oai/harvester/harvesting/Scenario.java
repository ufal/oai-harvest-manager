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

package nl.mpi.oai.harvester.harvesting;

import nl.mpi.oai.harvester.Provider;
import nl.mpi.oai.harvester.action.ActionSequence;
import nl.mpi.oai.harvester.control.FileSynchronization;
import nl.mpi.oai.harvester.control.Main;
import nl.mpi.oai.harvester.metadata.Metadata;
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
public class Scenario {

    private static final Logger logger = LogManager.getLogger(Scenario.class);

    //
    final Provider provider;

    //
    final ActionSequence actionSequence;

    //
    private static final ReadWriteLock exclusiveLock = new ReentrantReadWriteLock(true);

    public Scenario (Provider provider, ActionSequence actionSequence) {
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
    public List<String> getPrefixes(Harvesting harvesting){

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

    /**
     * Get metadata records indirectly, that is by first obtaining a list of
     * identifiers pointing to them <br><br>
     *
     * @param harvesting harvester
     * @return false on parser or input output error
     */
    public boolean listIdentifiers(AbstractHarvesting harvesting) {

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
    public boolean listRecords(AbstractHarvesting harvesting) {

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

    public List<String> getMetadataFormats(OAIFactory oaiFactory){
        // set type of format harvesting to apply
        AbstractHarvesting harvesting = new FormatHarvesting(oaiFactory,
                provider, actionSequence);

        // get the prefixes
        List<String> prefixes = this.getPrefixes(harvesting);
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
        // determine the type of record harvesting to apply
        if (method.equals("ListIdentifiers")) {
            // get the records indirectly, first obtaining identifiers
            done = listIdentifiers(new IdentifierListHarvesting(oaiFactory,
                    provider, prefixes, metadataFactory, provider.getEndpoint()));
            logger.debug("list identifiers -> done["+done+"]");
        } else {
            // get the records with ListRecords
            done = listRecords(new RecordListHarvesting(oaiFactory,
                    provider, prefixes, metadataFactory, provider.getEndpoint()));
            logger.debug("list records -> done[" + done + "]");
        }
        if(Main.config.isIncremental()) {
            FileSynchronization.execute(provider);
        }
        return done;

    }
}

