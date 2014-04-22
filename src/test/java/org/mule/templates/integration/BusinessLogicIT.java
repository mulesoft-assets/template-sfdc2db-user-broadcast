package org.mule.templates.integration;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.tck.junit4.rule.DynamicPort;

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
	public void testGatherDataFlow() throws Exception {
//		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("gatherDataFlow");
//		flow.initialise();
//
//		MuleEvent event = flow.process(getTestEvent("", MessageExchangePattern.REQUEST_RESPONSE));
//		Set<String> flowVariables = event.getFlowVariableNames();
//		Assert.assertTrue("The variable leadsFromOrgA is missing.", flowVariables.contains(LEADS_FROM_ORG_A));
//		Assert.assertTrue("The variable leadsFromOrgB is missing.", flowVariables.contains(LEADS_FROM_ORG_B));
//
//		ConsumerIterator<Map<String, String>> leadsFromOrgA = event.getFlowVariable(LEADS_FROM_ORG_A);
//		ConsumerIterator<Map<String, String>> leadsFromOrgB = event.getFlowVariable(LEADS_FROM_ORG_B);
//		Assert.assertTrue("There should be leads in the variable leadsFromOrgA.", leadsFromOrgA.size() != 0);
//		Assert.assertTrue("There should be leads in the variable leadsFromOrgB.", leadsFromOrgB.size() != 0);
	}



	@SuppressWarnings("unchecked")
	private void updateUsers() throws Exception {
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("updateUserSubFlow");
		flow.initialise();
		MuleEvent event = flow.process(getTestEvent("", MessageExchangePattern.ONE_WAY));
		List<SaveResult> results = (List<SaveResult>) event.getMessage().getPayload();
		for (int i = 0; i < results.size(); i++) {
			System.err.println(results.get(i));
		}
	}


}
