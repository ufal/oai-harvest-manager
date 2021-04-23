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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
    private final boolean isDryRun;
    private final Configuration config;

    /**
     * Associate a provider and action actionSequences with a scenario
     *
     * @param provider OAI-PMH provider that this thread will harvest
     */
    public Worker(Provider provider) {
        this(provider, Main.config);
    }

    public Worker(Provider provider, Configuration config){
        this.provider = provider;
        this.actionSequences = config.getActionSequences();
        this.isDryRun = config.isDryRun();
        this.config = config;
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

            logger.info("Processing provider[" + provider + "]");
            FileSynchronization.addProviderStatistic(provider);

            for (final ActionSequence actionSequence : actionSequences) {
                
                if(isDryRun) {
                    logger.info("Dry run mode. Skipping action sequence: {{}}", actionSequence.toString());
                } else {
                    done = provider.harvest(actionSequence);
                }
                // break after any (the first) action sequence has completed successfully
                if (done){
                    provider.cleanupResumptionDetails();
                    break;
                }
            }

            provider.persistCurrentStatistic();
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

    private void writeProviderToMapFile(Provider provider) {
        synchronized (config) {
            String map = config.getMapFile();
            try (PrintWriter m = new PrintWriter(new FileWriter(map, true))) {
                if (config.hasRegistryReader()) {
                    m.println(config.getRegistryReader().endpointMapping(provider.getOaiUrl(), provider.getName()));
                } else {
                    m.printf("%s,%s,,", provider.getOaiUrl(), Util.toFileFormat(provider.getName()).replaceAll("/", ""));
                    m.println();
                }
            } catch (IOException e) {
                logger.error("failed to write to the map file!", e);
            }
        }
    }

}
