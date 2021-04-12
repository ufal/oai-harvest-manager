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

package nl.mpi.oai.harvester;

import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import nl.mpi.oai.harvester.action.ActionSequence;
import nl.mpi.oai.harvester.harvesting.NoMoreRetriesException;
import nl.mpi.oai.harvester.harvesting.OAIFactory;
import nl.mpi.oai.harvester.harvesting.scenarios.Scenario;
import nl.mpi.oai.harvester.harvesting.scenarios.ScenarioFactory;
import nl.mpi.oai.harvester.metadata.MetadataFormat;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.w3c.dom.Document;

/**
 * Tests for Provider class. These test parsing of actual OAI-PMH responses
 * (found in files in the test resources directory). No network connections
 * are made.
 * 
 * @author Lari Lampen (MPI-PL)
 */
public class ProviderTest {
    /**
     * Test of parseProviderName method, of class Provider.
     */
    @Test
    public void testParseProviderName() throws Exception {
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	DocumentBuilder db = dbf.newDocumentBuilder();
	Document doc = db.parse(getClass().getResourceAsStream("/response-Identify.xml"));

	String expResult = "CLARIN Centre Vienna / Language Resources Portal";

	Provider instance = new Provider("dummy", 1, new int[]{0});
	String result = instance.parseProviderName(doc);

	assertEquals(expResult, result);
    }

    @Test(expected = NoMoreRetriesException.class)
	public void exceptionThrownWhenNoMoreRetries() throws ParserConfigurationException {
        Provider p = new Provider("bogus", 1, new int[] {});
		ActionSequence as = new ActionSequence(new MetadataFormat("prefix", "oai_dc"));
		Scenario mockedScenario = spy(ScenarioFactory.getScenario(p, as));
		doReturn(List.of("oai_dc")).when(mockedScenario).getMetadataFormats(any(OAIFactory.class));
		try(MockedStatic<ScenarioFactory> mockedFactory = Mockito.mockStatic(ScenarioFactory.class)){
			mockedFactory.when(() -> ScenarioFactory.getScenario(any(Provider.class), any(ActionSequence.class)))
					.thenReturn(mockedScenario);
			p.harvest(as);
		}

	}
}
