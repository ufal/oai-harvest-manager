/*
 * Copyright (C) 2014, The Max Planck Institute for
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

package nl.mpi.oai.harvester.control;

import nl.mpi.oai.harvester.Provider;
import nl.mpi.oai.harvester.action.ActionSequence;
import nl.mpi.oai.harvester.cycle.Cycle;
import nl.mpi.oai.harvester.cycle.Endpoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * This class represents a single processing thread in the harvesting actions
 * workflow. In practice one worker takes care of one provider. The worker
 * applies a scenario for harvesting: first get record identifiers, after that
 * get the records individually. Alternatively, in a second scenario, it gets
 * multiple records per OAI request directly.
 *
 * @author Lari Lampen (MPI-PL), extensions by Kees Jan van de Looij (MPI-PL).
 */
class Worker implements Runnable {
    
    private static final Logger logger = LogManager.getLogger(Worker.class);
    
    /** The provider this worker deals with. */
    private final Provider provider;

    /** List of actionSequences to be applied to the harvested metadata. */
    private final List<ActionSequence> actionSequences;


    // kj: annotate
    final Endpoint endpoint;

    /**
     * Associate a provider and action actionSequences with a scenario
     *
     * @param provider OAI-PMH provider that this thread will harvest
     * @param cycle the harvesting cycle
     */
    public Worker(Provider provider,
                  Cycle cycle) {

	this.provider = provider;

	this.actionSequences = Main.config.getActionSequences();

        // register the endpoint with the cycle, kj: get the group
        endpoint = cycle.next(provider.getOaiUrl(), "group");

        // FIXME The Endpoint should be refactored further
        provider.setEndpoint(endpoint);
    }

    @Override
    public void run() {
        Throwable t = null;
        try {
            logger.debug("Welcome to OAI Harvest Manager worker!");
            provider.init();
            
            Thread.currentThread().setName(provider.getName().replaceAll("[^a-zA-Z0-9\\-\\(\\)]"," "));

            // setting specific log filename
            ThreadContext.put("logFileName", Util.toFileFormat(provider.getName()).replaceAll("/", ""));
            writeProviderToMapFile(provider);

            boolean done = false;

            logger.info("Processing provider[" + provider + "] using scenario[" + provider.getScenario() + "], incremental[" + provider.getIncremental() + "], timeout[" + provider.getTimeout() + "] and retry[count="+provider.getMaxRetryCount()+",delays="+Arrays.toString(provider.getRetryDelays())+"]");

            FileSynchronization.addProviderStatistic(provider);

            for (final ActionSequence actionSequence : actionSequences) {
                
                if(Main.config.isDryRun()) {
                    logger.info("Dry run mode. Skipping action sequence: {{}}", actionSequence.toString());
                } else {
                    done = provider.harvest(actionSequence);
                }
                // break after any (the first) action sequence has completed successfully
                if (done) break;
            }

            // report back success or failure to the cycle
            endpoint.doneHarvesting(done);
            if (provider.getIncremental()) {
                FileSynchronization.saveStatistics(provider);
                endpoint.setIncrement(FileSynchronization.getProviderStatistic(provider).getHarvestedRecords());
            }
            logger.info("Processing finished for " + provider);
        } catch (Throwable e) {
            logger.error("Processing failed for " + provider+": "+e.getMessage(),e);
            t = e;
            throw e;
        } finally {
            provider.close();
                
            ThreadContext.clearAll();
            
            // tell the main log how it went
            if (t != null)
                logger.error("Processing failed for " + provider+": "+t.getMessage(),t);
            else
                logger.info("Processing finished for " + provider);

            logger.debug("Goodbye from OAI Harvest Manager worker!");
        }
    }

    private static synchronized void writeProviderToMapFile(Provider provider) {
        String map = Main.config.getMapFile();
        try(PrintWriter m = new PrintWriter(new FileWriter(map,true))) {
            if (Main.config.hasRegistryReader()) {
                m.println(Main.config.getRegistryReader().endpointMapping(provider.getOaiUrl(),provider.getName()));
            } else {
                m.printf("%s,%s,,", provider.getOaiUrl(),Util.toFileFormat(provider.getName()).replaceAll("/", ""));
                m.println();
            }
        } catch (IOException e) {
            logger.error("failed to write to the map file!",e);
        }
    }

}
