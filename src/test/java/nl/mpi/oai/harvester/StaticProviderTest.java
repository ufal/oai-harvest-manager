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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ORG.oclc.oai.harvester2.verb.HarvesterVerb;
import ORG.oclc.oai.harvester2.verb.Identify;
import nl.mpi.oai.harvester.action.Action;
import nl.mpi.oai.harvester.action.ActionSequence;
import nl.mpi.oai.harvester.harvesting.*;
import nl.mpi.oai.harvester.metadata.Metadata;
import nl.mpi.oai.harvester.metadata.MetadataFactory;
import nl.mpi.oai.harvester.metadata.MetadataFormat;
import nl.mpi.oai.harvester.utils.DocumentSource;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.mockito.internal.util.reflection.FieldSetter;
import org.w3c.dom.Document;

/**
 * Tests for StaticProvider class. These test parsing of an actual static
 * provider response (found in the file static-repo.xml in the test resources
 * directory). No network connections are made.
 * 
 * @author Lari Lampen (MPI-PL)
 */
public class StaticProviderTest {
    /**
     * Test of getProviderName method, of class StaticProvider.
     */
    @Test
    public void testGetProviderName() throws Exception {
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	DocumentBuilder db = dbf.newDocumentBuilder();
	Document doc = db.parse(getClass().getResourceAsStream("/static-repo.xml"));

	String expResult = "Magoria Books' Carib and Romani Archive";

	StaticProvider instance = new StaticProvider(doc);
	String result = instance.getProviderName();

	assertEquals(expResult, result);
    }

    /**
     * Test of init + getName methods, of class StaticProvider.
     */
    @Test
    public void testGetName() throws Exception {
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	DocumentBuilder db = dbf.newDocumentBuilder();
	Document doc = db.parse(getClass().getResourceAsStream("/static-repo.xml"));

	String expResult = "Magoria Books' Carib and Romani Archive";

	StaticProvider instance = new StaticProvider(doc);
	instance.init();
	String result = instance.getName();

	assertEquals(expResult, result);
    }

    /**
     * Test of getPrefixes method, of class StaticProvider.
     */
    @Test
    public void testGetPrefixes() throws Exception {
	List<String> expResult = new ArrayList<>();
	expResult.add("olac");

	StaticProvider instance = new StaticProvider(null, 1, new int[]{0});
	MetadataFormat format = new MetadataFormat("namespace",
		"http://www.language-archives.org/OLAC/1.0/");
	Action[] actions = new Action[]{};
	ActionSequence actionSequence = new ActionSequence(format, actions, 1);
	Scenario scenario = new Scenario(instance, actionSequence);
	Harvesting harvesting = spy(new StaticPrefixHarvesting(new OAIFactory(), instance, actionSequence));
	when(harvesting.request()).thenReturn(true);
	when(harvesting.getResponse()).thenReturn(new DocumentSource(getClass().getResourceAsStream("/static-repo.xml")));
	List<String> result = scenario.getPrefixes(harvesting);

	assertEquals(expResult, result);
    }

    /**
     * Test of getIdentifiers method, of class StaticProvider.
     */
    @Test
    public void testGetIdentifiers() throws Exception {
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	DocumentBuilder db = dbf.newDocumentBuilder();
	Document doc = db.parse(getClass().getResourceAsStream("/static-repo.xml"));

	List<String> expResult = new ArrayList<>();
	expResult.add("oai:mbcarrom.linguistlist.org:370");
	expResult.add("oai:mbcarrom.linguistlist.org:371");

	StaticProvider instance = injectStaticRepoXmlResponse(new StaticProvider(null, 1, new int[]{}));
	List<String> result = getIdentifiers(instance, "olac");

	assertEquals(expResult, result);
    }

    /**
     * Test of getRecord method, of class StaticProvider.
     */
    @Test
    public void testGetRecord() throws Exception {
	String id = "oai:mbcarrom.linguistlist.org:371";
	String mdPrefix = "olac";
	StaticProvider instance = injectStaticRepoXmlResponse(new StaticProvider(null, 1, new int[]{}));
	Metadata result = getRecord(instance, id, mdPrefix);

	assertNotNull(result);
	assertEquals(id, result.getId());
	assertEquals(instance, result.getOrigin());
	assertNotNull(result.getDoc());
    }

    /**
     * Test of getRecord method, of class StaticProvider. In this case there is
     * no match.
     */
    @Test
    public void testGetRecord_noSuchId() throws Exception {
	String id = "garbage";
	String mdPrefix = "olac";
	StaticProvider instance = injectStaticRepoXmlResponse(new StaticProvider(null, 1, new int[]{}));
	Metadata result = getRecord(instance, id, mdPrefix);

	assertNull(result);
    }

    /**
     * Test of getRecord method, of class StaticProvider.
     */
    @Test
    public void testGetRecord_noSuchPrefix() throws Exception {
	String id = "oai:mbcarrom.linguistlist.org:371";
	String mdPrefix = "garbage";
	StaticProvider instance = injectStaticRepoXmlResponse(new StaticProvider(null, 1, new int[]{}));
	Metadata result = getRecord(instance, id, mdPrefix);

	assertNull(result);
    }

    private Metadata getRecord(StaticProvider provider, String id, String mdPrefix) {
		for(Metadata metadata : getMetadataRecords(provider, mdPrefix)){
    		if(id.equals(metadata.getId()) && mdPrefix.equals(metadata.getPrefix())){
    			assertEquals(provider, metadata.getOrigin());
    			return metadata;
			}
		}
    	return null;
	}

	private List<Metadata> getMetadataRecords(StaticProvider provider, String mdPrefix){
		OAIFactory oaiFactory = new OAIFactory();
		final List<Metadata> metadataRecords = new ArrayList<>();
		Action action = new Action() {
			@Override
			public boolean perform(List<Metadata> records) {
				return metadataRecords.addAll(records);
			}

			@Override
			public Action clone() {
				return this;
			}
		};
		ActionSequence actionSequence = new ActionSequence(new MetadataFormat("prefix", mdPrefix),
				new Action[]{action}, 1);

		Scenario scenario = new Scenario(provider, actionSequence);
		AbstractHarvesting harvesting = new StaticRecordListHarvesting(oaiFactory, provider, Arrays.asList(mdPrefix),
				new MetadataFactory());
		boolean done = scenario.listIdentifiers(harvesting);
		return  metadataRecords;
	}

	private StaticProvider injectStaticRepoXmlResponse(StaticProvider provider) throws IOException, NoSuchFieldException {
		Identify identify = spy(new Identify());
		InputStream is = new ByteArrayInputStream(
				IOUtils.toByteArray(
						getClass().getResourceAsStream("/static-repo.xml")
				)
		);
		FieldSetter.setField(identify, HarvesterVerb.class.getDeclaredField("str"), is);
		provider = spy(provider);
		when(provider.getResponse()).thenReturn(identify);
		return provider;
	}

	private List<String> getIdentifiers(StaticProvider provider, String mdPrefix){
		return getMetadataRecords(provider, mdPrefix).stream().map(Metadata::getId).collect(Collectors.toList());
	}
}
