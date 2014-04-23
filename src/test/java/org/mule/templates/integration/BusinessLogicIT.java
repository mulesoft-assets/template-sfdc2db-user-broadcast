package org.mule.templates.integration;

import static junit.framework.Assert.assertEquals;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.templates.test.utils.ListenerProbe;

import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the flows for this Mule Template that make calls to external systems.
 */
public class BusinessLogicIT extends AbstractTemplateTestCase {

	protected static final String TEMPLATE_NAME = "lead-aggregation";
//	private static final String LEADS_FROM_ORG_A = "leadsFromOrgA";
//	private static final String LEADS_FROM_ORG_B = "leadsFromOrgB";
//	private List<Map<String, Object>> createdLeadsInA = new ArrayList<Map<String, Object>>();
//	private List<Map<String, Object>> createdLeadsInB = new ArrayList<Map<String, Object>>();

	@Rule
	public DynamicPort port = new DynamicPort("http.port");

	@Before
	public void setUp() throws Exception {
		updateUsers();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testMainFlow() throws Exception {
		// Run poll and wait for it to run
		runSchedulersOnce("mainFlow");
		waitForPollToRun();

//		// Wait for the batch job executed by the poll flow to finish
//		helper.awaitJobTermination(TIMEOUT_SEC * 1000, 500);
//		helper.assertJobWasSuccessful();
//
//		assertEquals("The contact should not have been sync", null, invokeRetrieveFlow(retrieveContactFromBFlow, createdContactsInA.get(0)));
//		assertEquals("The contact should not have been sync", null, invokeRetrieveFlow(retrieveContactFromBFlow, createdContactsInA.get(1)));
//
//		Map<String, Object> contacPayload = invokeRetrieveFlow(retrieveContactFromBFlow, createdContactsInA.get(2));
//		assertEquals("The contact should have been sync", createdContactsInA.get(2).get("Email"), contacPayload.get("Email"));
//
//		Map<String, Object> accountPayload = invokeRetrieveFlow(retrieveAccountFlowFromB, createdAccountsInA.get(0));
//		Assert.assertEquals("The contact should belong to a different account ", accountPayload.get("Id"), contacPayload.get("AccountId"));

	}
	
	
	protected void waitForPollToRun() {
		System.out.println("Waiting for poll to run ones...");
		pollProber.check(new ListenerProbe(pipelineListener));
		System.out.println("Poll flow done");
	}

	@SuppressWarnings("unchecked")
	private void updateUsers() throws Exception {
		// actualise lastModifiedDate of some user, so he will be queried subsequently by the poll
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("updateUserSubFlow");
		flow.initialise();
		MuleEvent event = flow.process(getTestEvent("", MessageExchangePattern.REQUEST_RESPONSE));
		List<SaveResult> results = (List<SaveResult>) event.getMessage().getPayload();
		for (int i = 0; i < results.size(); i++) {
			System.err.println(results.get(i));
		}
	}


}
