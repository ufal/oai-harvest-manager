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
import nl.mpi.oai.harvester.cycle.Cycle;
import nl.mpi.oai.harvester.cycle.CycleFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;


/**
 * Executable class, main entry point of OAI Harvester.
 *
 * @author Lari Lampen (MPI-PL)
 */
public class Main {
    private static final String sep = System.getProperty("file.separator");
    private static final Logger logger = LogManager.getLogger(Main.class);

    /** Object containing entries from configuration file. */
    public static Configuration config;

    private static void runHarvesting(Configuration config) {
	config.log();
        
        ExecutorService executor = new ScheduledThreadPoolExecutor(config.getMaxJobs());

	// create a CycleFactory
	CycleFactory factory = new CycleFactory();
	// get a cycle based on the overview file
	File OverviewFile = new File (config.getOverviewFile());
	Cycle cycle = factory.createCycle(OverviewFile);

		final List<Callable<Object>> workers = config.getProviders().stream()
				.map(provider -> new Worker(provider, cycle))
				.map(Executors::callable)
				.collect(Collectors.toList());
		try {
			executor.invokeAll(workers);
		} catch (InterruptedException e) {
			logger.error(e);
		}
		executor.shutdown();
    }

    public static void main(String[] args) {
        
        logger.info("Welcome to the main OAI Harvest Manager!");
        
	String configFile = null;

	setSystemProperties();

	// If the "config" parameter is specified, take it as the
	// configuration file name.
	for (String arg : args) {
	    if (arg.startsWith("config=")) {
		configFile = arg.substring(7);
	    } else if (!arg.contains("=")) {
		configFile = arg;
	    }
	}

	// If a configuration file name hasn't been specified in the
	// arguments, use "resources/config.xml" by default.
	if (configFile == null) {
	    configFile = "resources" + sep + "config.xml";
	}

	// Process options given on the command line (if any), then read the
	// configuration file. 
	config = new Configuration();
	for (String arg : args) {
	    if (arg.indexOf('=') > -1) {
		String[] tmp=arg.split("=");
		if (tmp.length == 1) {
		    config.setOption(tmp[0], null);
		} else if (tmp.length >= 2) {
		    config.setOption(tmp[0], tmp[1]);
		}
	    }
	}
	try {
	    config.readConfig(configFile);
	} catch (ParserConfigurationException | SAXException 
		| XPathExpressionException | IOException ex) {
	    logger.error("Unable to read configuration file", ex);
	    return;
	}

	// Ensure the timeout setting is honored.
	config.applyTimeoutSetting();

	runHarvesting(config);
        
        logger.info("Goodbye from the main OAI Harvest Manager!");

    }

    public static void setSystemProperties(){
		// Select Saxon XSLT/XPath implementation (necessary in case there
		// are other XSLT/XPath libraries in classpath).
        // This must also be set for the tests.
		// Saxon 10.3 doesn't register xpathfactory automatically https://www.saxonica.com/documentation/#!xpath-api/jaxp-xpath/factory
		// and the test will fail while using org.apache.xpath.jaxp.XPathImpl
		// can also be set from command line with -Djavax.xml.xpath.XPathFactory:http://java.sun.com/jaxp/xpath/dom=net.sf.saxon.xpath.XPathFactoryImpl
        // use jaxp.debug (-Djaxp.debug=true) to see what gets used
		System.setProperty("javax.xml.transform.TransformerFactory",
				"net.sf.saxon.TransformerFactoryImpl");
		System.setProperty("javax.xml.xpath.XPathFactory:http://java.sun.com/jaxp/xpath/dom",
				"net.sf.saxon.xpath.XPathFactoryImpl");

		// Some endpoints behave differently when you're not a browser, so fake it
		System.setProperty("http.agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");


	}
}
