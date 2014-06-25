/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.integration;

import java.io.FileInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.JCEIESCipher.ECIES;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.templates.builders.SfdcObjectBuilder;
import org.mule.templates.db.MySQLDbCreator;

import com.mulesoft.module.batch.BatchTestHelper;
import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the flows for this Mule Template that make calls to external systems.
 */
public class BusinessLogicIT extends AbstractTemplateTestCase {

	protected static final int TIMEOUT = 60;
	private static final Logger log = Logger.getLogger(BusinessLogicIT.class);
	private BatchTestHelper helper;
	private Map<String, Object> user = null;
	private String existingUser = null;

	private static final String PATH_TO_TEST_PROPERTIES = "./src/test/resources/mule.test.properties";
	private static final String PATH_TO_SQL_SCRIPT = "src/main/resources/user.sql";
	private static final String DATABASE_NAME = "SFDC2DBUserBroadcast" + new Long(new Date().getTime()).toString();
	private static final MySQLDbCreator DBCREATOR = new MySQLDbCreator(DATABASE_NAME, PATH_TO_SQL_SCRIPT, PATH_TO_TEST_PROPERTIES);
	
	@BeforeClass
	public static void init() {
		System.setProperty("page.size", "1000");
		System.setProperty("poll.frequencyMillis", "10000");
		System.setProperty("poll.startDelayMillis", "20000");
		System.setProperty("watermark.default.expression", "#[groovy: new Date(System.currentTimeMillis() - 10000).format(\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\", TimeZone.getTimeZone('UTC'))]");
		
		System.setProperty("database.url", DBCREATOR.getDatabaseUrlWithName());
		DBCREATOR.setUpDatabase();
	}

	@Before
	public void setUp() throws Exception {
		final Properties props = new Properties();
		try {
			props.load(new FileInputStream(PATH_TO_TEST_PROPERTIES));
		} catch (Exception e) {
			e.printStackTrace();
		}
		existingUser = props.getProperty("sfdc.user.id");
		System.err.println(existingUser);

		stopFlowSchedulers(POLL_FLOW_NAME);
		registerListeners();
		helper = new BatchTestHelper(muleContext);

		// prepare test data
		user = createSalesforceUser();
		insertUserSalesforce(user);
		
	}

	
	@After
	public void tearDown() throws Exception {
		stopFlowSchedulers(POLL_FLOW_NAME);

		// delete previously created user from db with matching email
		Map<String, Object> usr = new HashMap<String, Object>();
		usr.put("lastname", user.get("LastName"));
		deleteUserFromDB(usr);

		DBCREATOR.tearDownDataBase();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMainFlow() throws Exception {
		// Run poll and wait for it to run
		runSchedulersOnce(POLL_FLOW_NAME);
		waitForPollToRun();

		// Wait for the batch job executed by the poll flow to finish
		helper.awaitJobTermination(TIMEOUT_SEC * 1000, 500);
		helper.assertJobWasSuccessful();

		// Prepare payload
		final String lastName = (String) user.get("LastName");
		final Map<String, Object> userToRetrieveLastName = new HashMap<String, Object>();
		userToRetrieveLastName.put("LastName", lastName);
		log.error("userToRetrieveLastName: " + userToRetrieveLastName);

		// Execute selectUserFromDB sublow
		SubflowInterceptingChainLifecycleWrapper selectUserFromDBFlow = getSubFlow("selectUserFromDB");
		final MuleEvent event = selectUserFromDBFlow.process(getTestEvent(userToRetrieveLastName, MessageExchangePattern.REQUEST_RESPONSE));
		final List<Map<String, Object>> payload = (List<Map<String, Object>>) event.getMessage().getPayload();

		// print result
		for (Map<String, Object> usr : payload)
			log.error("selectUserFromDB response: " + usr);

		// User previously created in Salesforce should be present in database
		log.error("before assert" + payload);
		Assert.assertEquals("The user should have been sync", 1, payload.size());
		Assert.assertEquals("The user email should match", lastName, payload.get(0).get("lastname"));
	}

	private void insertUserSalesforce(Map<String, Object> user) throws Exception {
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("updateUserSalesforceSubFlow");
		flow.initialise();

		final MuleEvent event = flow.process(getTestEvent(user, MessageExchangePattern.REQUEST_RESPONSE));
		final SaveResult result = (SaveResult) event.getMessage().getPayload();

		// store Id into our user
		log.info("response from updateUserSalesforceSubFlow: " + result);
		user.put("Id", result.getId());
	}

	private void deleteUserFromDB(Map<String, Object> user) throws Exception {
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("deleteUserDB");
		flow.initialise();

		MuleEvent event = flow.process(getTestEvent(user, MessageExchangePattern.REQUEST_RESPONSE));
		Object result = event.getMessage().getPayload();
		log.info("deleteUserDB result: " + result);
	}

	private Map<String, Object> createSalesforceUser() {
		final String name = "test";
		SfdcObjectBuilder builder = new SfdcObjectBuilder();
		
		// updating existing one rather than creating new one because it cannot be deleted
		final Map<String, Object> user = builder
				.with("Id", existingUser)
				.with("LastName", name)
				.with("FirstName", name)
				.build();
		return user;
	}

}
