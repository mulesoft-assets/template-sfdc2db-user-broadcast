package org.mule.templates.integration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.modules.salesforce.bulk.EnrichedUpsertResult;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Prober;
import org.mule.templates.test.utils.PipelineSynchronizeListener;

import com.mulesoft.module.batch.BatchTestHelper;

/**
 * The objective of this class is to validate the correct behavior of the flows
 * for this Mule Template that make calls to external systems.
 */
public class BusinessLogicIT extends AbstractTemplateTestCase {

	protected static final int TIMEOUT = 60;
	private static final String POLL_FLOW_NAME = "triggerFlow";
	private SubflowInterceptingChainLifecycleWrapper selectUserFromDBFlow;

	private BatchTestHelper helper;
	private final Prober pollProber = new PollingProber(10000, 1000);
	private final PipelineSynchronizeListener pipelineListener = new PipelineSynchronizeListener(
			POLL_FLOW_NAME);

	private Map<String, Object> user = null;

	@BeforeClass
	public static void init() {
		System.setProperty("page.size", "1000");
		System.setProperty("poll.frequencyMillis", "10000");
		System.setProperty("poll.startDelayMillis", "20000");
		System.setProperty("watermark.default.expression",
				"#[groovy: new Date(System.currentTimeMillis() - 10000).format(\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\", TimeZone.getTimeZone('UTC'))]");
	}

	@Before
	public void setUp() throws Exception {
		stopFlowSchedulers(POLL_FLOW_NAME);
		registerListeners();
		helper = new BatchTestHelper(muleContext);

		// prepare test data
		user = createUser();
		insertUserSalesforce(user);
	}

	@After
	public void tearDown() throws Exception {
		stopFlowSchedulers(POLL_FLOW_NAME);
	}

	@Test
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
		System.err.println(userToRetrieveMail);

		// Execute selectUserFromDB sublow
		selectUserFromDBFlow = getSubFlow("selectUserFromDB");
		final MuleEvent event = selectUserFromDBFlow.process(getTestEvent(userToRetrieveMail, MessageExchangePattern.REQUEST_RESPONSE));
		final List<Map<String, Object>> payload = (List<Map<String, Object>>) event.getMessage().getPayload();

		// print result
		for (Map<String, Object> usr : payload)
			System.err.println("response " + usr);

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
			System.err.println("response from salesforce " + item);
			user.put("Id", item.getId());
		}
	}

	private Map<String, Object> createUser() {
		final Map<String, Object> user = new HashMap<String, Object>();
		final String name = "tst" + RandomStringUtils.randomAlphabetic(5).toLowerCase();
		final String uniqueEmail = buildUniqueEmail(name);
		user.put("LocaleSidKey", "en_US");
		user.put("LastName", name);
		user.put("LanguageLocaleKey", "en_US");
		user.put("Email", uniqueEmail);
		user.put("UserName", uniqueEmail);
		user.put("FirstName", name);
		user.put("TimeZoneSidKey", "America/New_York");
		user.put("CommunityNickname", name);
		user.put("Alias", name);
		user.put("ProfileId", "00ed0000000GO9T");
		user.put("EmailEncodingKey", "ISO-8859-1");
		return user;
	}

}
