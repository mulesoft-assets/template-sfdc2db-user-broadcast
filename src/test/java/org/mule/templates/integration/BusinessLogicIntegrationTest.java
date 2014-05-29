package org.mule.templates.integration;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
//import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.modules.salesforce.bulk.EnrichedUpsertResult;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.templates.builders.SfdcObjectBuilder;

import com.mulesoft.module.batch.BatchTestHelper;

/**
 * The objective of this class is to validate the correct behavior of the flows for this Mule Template that make calls to external systems.
 */
public class BusinessLogicIntegrationTest extends AbstractTemplateTestCase {

	protected static final int TIMEOUT = 60;
	private static final String DATABASE_NAME = "SFDC2DBUserBroadcast" + new Long(new Date().getTime()).toString();
	private BatchTestHelper helper;
	private Map<String, Object> user = null;


	
	@BeforeClass
	public static void init() {
		System.setProperty("page.size", "1000");
		System.setProperty("poll.frequencyMillis", "10000");
		System.setProperty("poll.startDelayMillis", "20000");
		System.setProperty("watermark.default.expression", "#[groovy: new Date(System.currentTimeMillis() - 10000).format(\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\", TimeZone.getTimeZone('UTC'))]");
		System.setProperty("database.url", "jdbc:mysql://iappsandbox.cbbmvnwhlhi8.us-east-1.rds.amazonaws.com:3306/"+DATABASE_NAME+"?rewriteBatchedStatements=true&password=PMmulebells&user=iappsandbox");
		
	}

	@Before
	public void setUp() throws Exception {
		
		setUpDatabase();
		
		stopFlowSchedulers(POLL_FLOW_NAME);
		
		registerListeners();
		
		helper = new BatchTestHelper(muleContext);

		// prepare test data
		user = createSalesforceUser();
		insertUserSalesforce(user);
	}

	private void setUpDatabase() {
		
		System.out.println("******************************** Populate MySQL DB **************************");
		String dbURL = "jdbc:mysql://iappsandbox.cbbmvnwhlhi8.us-east-1.rds.amazonaws.com:3306/?user=iappsandbox&password=PMmulebells";
		Connection conn = null;
		
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			// Get a connection
			conn = DriverManager.getConnection(dbURL);
			Statement stmt = conn.createStatement();
			FileInputStream fstream = new FileInputStream("src/main/resources/user.sql");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			stmt.addBatch("CREATE DATABASE "+DATABASE_NAME);
			stmt.addBatch("USE "+DATABASE_NAME);

			String strLine;
			StringBuffer createStatement = new StringBuffer();
			// Specify delimiter according to sql file
			while ((strLine = br.readLine()) != null) {
				if (strLine.length() > 0) {
					strLine.replace("\n", "");
					createStatement.append(strLine);
				}
			}
			stmt.addBatch(createStatement.toString());
			in.close();
		
			stmt.executeBatch();
			
		} catch (SQLException ex) {
		    // handle any errors
		    System.out.println("SQLException: " + ex.getMessage());
		    System.out.println("SQLState: " + ex.getSQLState());
		    System.out.println("VendorError: " + ex.getErrorCode());
		} catch (Exception except) {
			except.printStackTrace();
		}
	
	}
	
	@After
	public void tearDown() throws Exception {
		
		
		stopFlowSchedulers(POLL_FLOW_NAME);
		// delete user from Salesforce
		// user could at least be flagged as inactive

		// delete previously created user from db with matching email
		Map<String, Object> usr = new HashMap<String, Object>();
		usr.put("email", user.get("Email"));
		deleteUserFromDB(usr);

		tearDownDataBase();
		
	}

	private void tearDownDataBase() {
		
		System.out
		.println("******************************** Delete Tables from MySQL DB **************************");
		String dbURL = "jdbc:mysql://iappsandbox.cbbmvnwhlhi8.us-east-1.rds.amazonaws.com:3306/?user=iappsandbox&password=PMmulebells";
		Connection conn = null;
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		
			// Get a connection
			conn = DriverManager.getConnection(dbURL);
		
			Statement stmt = conn.createStatement();
			stmt.executeUpdate("DROP SCHEMA "+DATABASE_NAME);
		} catch (Exception except) {
			except.printStackTrace();
		}

		
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
		final String email = (String) user.get("Email");
		final Map<String, Object> userToRetrieveMail = new HashMap<String, Object>();
		userToRetrieveMail.put("Email", email);
//		log.info("userToRetrieveMail: " + userToRetrieveMail);

		// Execute selectUserFromDB sublow
		SubflowInterceptingChainLifecycleWrapper selectUserFromDBFlow = getSubFlow("selectUserFromDB");
		final MuleEvent event = selectUserFromDBFlow.process(getTestEvent(userToRetrieveMail, MessageExchangePattern.REQUEST_RESPONSE));
		final List<Map<String, Object>> payload = (List<Map<String, Object>>) event.getMessage().getPayload();

		// print result
//		for (Map<String, Object> usr : payload)
//			log.info("selectUserFromDB response: " + usr);

		// User previously created in Salesforce should be present in database
		Assert.assertEquals("The user should have been sync", 1, payload.size());
		Assert.assertEquals("The user email should match", email, payload.get(0).get("email"));
	}

	@SuppressWarnings("unchecked")
	private void insertUserSalesforce(Map<String, Object> user) throws Exception {
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("insertUserSalesforceSubFlow");
		flow.initialise();

		final MuleEvent event = flow.process(getTestEvent(user, MessageExchangePattern.REQUEST_RESPONSE));
		final List<EnrichedUpsertResult> result = (List<EnrichedUpsertResult>) event.getMessage().getPayload();

		// store Id into our user
		for (EnrichedUpsertResult item : result) {
//			log.info("response from insertUserSalesforceSubFlow: " + item);
			user.put("Id", item.getId());
		}
	}

	private void deleteUserFromDB(Map<String, Object> user) throws Exception {
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("deleteUserDB");
		flow.initialise();

		MuleEvent event = flow.process(getTestEvent(user, MessageExchangePattern.REQUEST_RESPONSE));
		Object result = event.getMessage().getPayload();
//		log.info("deleteUserDB result: " + result);
	}

	private Map<String, Object> createSalesforceUser() {
		final String name = "tst" + buildUniqueName(5);
		final String uniqueEmail = buildUniqueEmail(name);
		System.err.println(name);
		SfdcObjectBuilder builder = new SfdcObjectBuilder();
		final Map<String, Object> user = builder
				.with("Email", uniqueEmail)
				.with("UserName", uniqueEmail)
				.with("LastName", name)
				.with("FirstName", name)
				.with("Alias", name)
				.with("CommunityNickname", name)

				// hardcoded defaults
				.with("LocaleSidKey", "en_US")
				.with("LanguageLocaleKey", "en_US")
				.with("TimeZoneSidKey", "America/New_York")

				// id of the chatter external user profile
				.with("ProfileId", "00e80000001C9I0")
				.with("EmailEncodingKey", "ISO-8859-1").build();
		return user;
	}

	private String buildUniqueName(int len) {
		return RandomStringUtils.randomAlphabetic(len).toLowerCase();
	}
}
