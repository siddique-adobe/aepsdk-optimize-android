/*
 Copyright 2021 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.optimize;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.MonitorExtension;
import com.adobe.marketing.mobile.TestHelper;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class OptimizeFunctionalTests {

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(new TestHelper.SetupCoreRule()).
            around(new TestHelper.RegisterMonitorExtensionRule());

    @Before
    public void setup() throws Exception {
        MobileCore.setApplication(ApplicationProvider.getApplicationContext());
        MobileCore.setLogLevel(LoggingMode.VERBOSE);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        MobileCore.registerExtensions(Arrays.asList(
                Optimize.EXTENSION
        ), o -> countDownLatch.countDown());
        Assert.assertTrue(countDownLatch.await(1000, TimeUnit.MILLISECONDS));
        TestHelper.resetTestExpectations();
    }

    @After
    public void tearDown() {
        NamedCollection configDataStore = ServiceProvider.getInstance().getDataStoreService().getNamedCollection(OptimizeTestConstants.CONFIG_DATA_STORE);
        if(configDataStore != null) {
            configDataStore.removeAll();
        }
    }

    //1
    @Test
    public void testExtensionVersion() {
        Assert.assertEquals(OptimizeTestConstants.EXTENSION_VERSION, Optimize.extensionVersion());
    }

    //2a
    @Test
    public void testUpdatePropositions_validDecisionScope() throws InterruptedException {
        //Setup
        final String decisionScopeName = "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==";
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        //Action
        Optimize.updatePropositions(Collections.singletonList(new DecisionScope(decisionScopeName)), null, null);

        //Assert
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        List<Event> eventsListEdge = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertNotNull(eventsListEdge);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertEquals(1, eventsListEdge.size());
        Event event = eventsListOptimize.get(0);
        Map<String, Object> eventData = event.getEventData();
        Assert.assertEquals(OptimizeTestConstants.EventType.OPTIMIZE, event.getType());
        Assert.assertEquals(OptimizeTestConstants.EventSource.REQUEST_CONTENT, event.getSource());
        Assert.assertTrue(eventData.size() > 0);
        Assert.assertEquals("updatepropositions", eventData.get("requesttype"));
        List<Map<String, String>> decisionScopes = (List<Map<String, String>>) eventData.get("decisionscopes");
        Assert.assertEquals(1, decisionScopes.size());
        Assert.assertEquals(decisionScopeName, decisionScopes.get(0).get("name"));

        //Validating Event data of Edge Request event
        Event edgeEvent = eventsListEdge.get(0);
        Assert.assertNotNull(edgeEvent);
        Map<String, Object> edgeEventData = edgeEvent.getEventData();
        Assert.assertNotNull(edgeEventData);
        Assert.assertTrue(edgeEventData.size() > 0);
        Assert.assertEquals("personalization.request", ((Map<String, Object>) edgeEventData.get("xdm")).get("eventType"));
        Map<String, Object> personalizationMap = (Map<String, Object>) ((Map<String, Object>) edgeEventData.get("query")).get("personalization");
        List<String> decisionScopeList = (List<String>) personalizationMap.get("decisionScopes");
        Assert.assertNotNull(decisionScopeList);
        Assert.assertEquals(1, decisionScopeList.size());
        Assert.assertEquals(decisionScopeName, decisionScopeList.get(0));
    }

    //2b
    @Test
    public void testUpdatePropositions_validNonEncodedDecisionScope() throws InterruptedException {
        //Setup
        final String activityId = "xcore:offer-activity:1111111111111111";
        final String placementId = "xcore:offer-placement:1111111111111111";

        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        //Action
        Optimize.updatePropositions(Collections.singletonList(new DecisionScope(activityId, placementId)), null, null);

        //Assert
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        List<Event> eventsListEdge = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertNotNull(eventsListEdge);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertEquals(1, eventsListEdge.size());
        Event event = eventsListOptimize.get(0);
        Map<String, Object> eventData = event.getEventData();
        Assert.assertEquals(OptimizeTestConstants.EventType.OPTIMIZE, event.getType());
        Assert.assertEquals(OptimizeTestConstants.EventSource.REQUEST_CONTENT, event.getSource());
        Assert.assertTrue(eventData.size() > 0);
        Assert.assertEquals("updatepropositions", eventData.get("requesttype"));
        List<Map<String, String>> decisionScopes = (List<Map<String, String>>) eventData.get("decisionscopes");
        Assert.assertEquals(1, decisionScopes.size());
        Assert.assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", decisionScopes.get(0).get("name"));

        //Validating Event data of Edge Request event
        Event edgeEvent = eventsListEdge.get(0);
        Assert.assertNotNull(edgeEvent);
        Map<String, Object> edgeEventData = edgeEvent.getEventData();
        Assert.assertNotNull(edgeEventData);
        Assert.assertTrue(edgeEventData.size() > 0);
        Assert.assertEquals("personalization.request", ((Map<String, Object>) edgeEventData.get("xdm")).get("eventType"));
        Map<String, Object> personalizationMap = (Map<String, Object>) ((Map<String, Object>) edgeEventData.get("query")).get("personalization");
        List<String> decisionScopeList = (List<String>) personalizationMap.get("decisionScopes");
        Assert.assertNotNull(decisionScopeList);
        Assert.assertEquals(1, decisionScopeList.size());
        Assert.assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", decisionScopeList.get(0));
    }

    //2c
    @Test
    public void testUpdatePropositions_validNonEncodedDecisionScopeWithItemCount() throws InterruptedException {
        //Setup
        final String activityId = "xcore:offer-activity:1111111111111111";
        final String placementId = "xcore:offer-placement:1111111111111111";
        final int itemCount = 30;

        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        //Action
        Optimize.updatePropositions(Collections.singletonList(new DecisionScope(activityId, placementId, itemCount)), null, null);

        //Assert
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        List<Event> eventsListEdge = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertNotNull(eventsListEdge);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertEquals(1, eventsListEdge.size());
        Event event = eventsListOptimize.get(0);
        Map<String, Object> eventData = event.getEventData();
        Assert.assertEquals(OptimizeTestConstants.EventType.OPTIMIZE, event.getType());
        Assert.assertEquals(OptimizeTestConstants.EventSource.REQUEST_CONTENT, event.getSource());
        Assert.assertTrue(eventData.size() > 0);
        Assert.assertEquals("updatepropositions", eventData.get("requesttype"));
        List<Map<String, String>> decisionScopes = (List<Map<String, String>>) eventData.get("decisionscopes");
        Assert.assertEquals(1, decisionScopes.size());
        Assert.assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEiLCJpdGVtQ291bnQiOjMwfQ==", decisionScopes.get(0).get("name"));

        //Validating Event data of Edge Request event
        Event edgeEvent = eventsListEdge.get(0);
        Assert.assertNotNull(edgeEvent);
        Map<String, Object> edgeEventData = edgeEvent.getEventData();
        Assert.assertNotNull(edgeEventData);
        Assert.assertTrue(edgeEventData.size() > 0);
        Assert.assertEquals("personalization.request", ((Map<String, Object>) edgeEventData.get("xdm")).get("eventType"));
        Map<String, Object> personalizationMap = (Map<String, Object>) ((Map<String, Object>) edgeEventData.get("query")).get("personalization");
        List<String> decisionScopeList = (List<String>) personalizationMap.get("decisionScopes");
        Assert.assertNotNull(decisionScopeList);
        Assert.assertEquals(1, decisionScopeList.size());
        Assert.assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEiLCJpdGVtQ291bnQiOjMwfQ==", decisionScopeList.get(0));
    }

    //3
    @Test
    public void testUpdatePropositions_validDecisionScopeWithXdmAndDataAndDatasetId() throws InterruptedException {
        //Setup
        final String decisionScopeName = "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==";
        final String optimizeDatasetId = "111111111111111111111111";
        Map<String, Object> xdmMap = new HashMap<String, Object>() {
            {
                put("MyXDMKey", "MyXDMValue");
            }
        };

        Map<String, Object> dataMap = new HashMap<String, Object>() {
            {
                put("MyDataKey", "MyDataValue");
            }
        };

        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        configData.put("optimize.datasetId", optimizeDatasetId);
        updateConfiguration(configData);

        Optimize.updatePropositions(Collections.singletonList(new DecisionScope(decisionScopeName)), xdmMap, dataMap);

        //Assert
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        List<Event> eventsListEdge = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertNotNull(eventsListEdge);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertEquals(1, eventsListEdge.size());
        Event event = eventsListOptimize.get(0);
        Map<String, Object> eventData = event.getEventData();
        Assert.assertEquals(OptimizeTestConstants.EventType.OPTIMIZE, event.getType());
        Assert.assertEquals(OptimizeTestConstants.EventSource.REQUEST_CONTENT, event.getSource());
        Assert.assertTrue(eventData.size() > 0);
        Assert.assertEquals("MyXDMValue", ((Map<String, String>) eventData.get("xdm")).get("MyXDMKey"));
        Assert.assertEquals("MyDataValue", ((Map<String, String>) eventData.get("data")).get("MyDataKey"));
        Assert.assertEquals("updatepropositions", eventData.get("requesttype"));
        List<Map<String, String>> decisionScopes = (List<Map<String, String>>) eventData.get("decisionscopes");
        Assert.assertEquals(1, decisionScopes.size());
        Assert.assertEquals(decisionScopeName, decisionScopes.get(0).get("name"));

        //Validating Event data of Edge Request event
        Event edgeEvent = eventsListEdge.get(0);
        Assert.assertNotNull(edgeEvent);
        Map<String, Object> edgeEventData = edgeEvent.getEventData();
        Assert.assertNotNull(edgeEventData);
        Assert.assertTrue(edgeEventData.size() > 0);
        Assert.assertEquals(optimizeDatasetId, edgeEventData.get("datasetId"));
        Assert.assertEquals("personalization.request", ((Map<String, Object>) edgeEventData.get("xdm")).get("eventType"));
        Assert.assertEquals("MyXDMValue", ((Map<String, Object>) edgeEventData.get("xdm")).get("MyXDMKey"));
        Map<String, Object> personalizationMap = (Map<String, Object>) ((Map<String, Object>) edgeEventData.get("query")).get("personalization");
        List<String> decisionScopeList = (List<String>) personalizationMap.get("decisionScopes");
        Assert.assertNotNull(decisionScopeList);
        Assert.assertEquals(1, decisionScopeList.size());
        Assert.assertEquals(decisionScopeName, decisionScopeList.get(0));
        Assert.assertEquals("MyDataValue", ((Map<String, Object>) edgeEventData.get("data")).get("MyDataKey"));
    }

    //4
    @Test
    public void testUpdatePropositions_multipleValidDecisionScope() throws InterruptedException {
        //Setup
        final String decisionScopeName1 = "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==";
        final String decisionScopeName2 = "MyMbox";
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        //Action
        Optimize.updatePropositions(
                Arrays.asList(new DecisionScope(decisionScopeName1), new DecisionScope(decisionScopeName2))
                , null, null);

        //Assert
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        List<Event> eventsListEdge = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertNotNull(eventsListEdge);
        Assert.assertEquals(1, eventsListEdge.size());
        Event event = eventsListOptimize.get(0);
        Map<String, Object> eventData = event.getEventData();
        Assert.assertEquals(OptimizeTestConstants.EventType.OPTIMIZE, event.getType());
        Assert.assertEquals(OptimizeTestConstants.EventSource.REQUEST_CONTENT, event.getSource());
        Assert.assertTrue(eventData.size() > 0);
        Assert.assertEquals("updatepropositions", eventData.get("requesttype"));
        List<Map<String, String>> decisionScopes = (List<Map<String, String>>) eventData.get("decisionscopes");
        Assert.assertEquals(2, decisionScopes.size());
        Assert.assertEquals(decisionScopeName1, decisionScopes.get(0).get("name"));
        Assert.assertEquals(decisionScopeName2, decisionScopes.get(1).get("name"));

        //Validating Event data of Edge Request event
        Event edgeEvent = eventsListEdge.get(0);
        Assert.assertNotNull(edgeEvent);
        Map<String, Object> edgeEventData = edgeEvent.getEventData();
        Assert.assertNotNull(edgeEventData);
        Assert.assertTrue(edgeEventData.size() > 0);
        Assert.assertEquals("personalization.request", ((Map<String, Object>) edgeEventData.get("xdm")).get("eventType"));
        Map<String, Object> personalizationMap = (Map<String, Object>) ((Map<String, Object>) edgeEventData.get("query")).get("personalization");
        List<String> decisionScopeList = (List<String>) personalizationMap.get("decisionScopes");
        Assert.assertNotNull(decisionScopeList);
        Assert.assertEquals(2, decisionScopeList.size());
        Assert.assertEquals(decisionScopeName1, decisionScopeList.get(0));
        Assert.assertEquals(decisionScopeName2, decisionScopeList.get(1));
    }

    //5
    @Test
    public void testUpdatePropositions_ConfigNotAvailable() throws InterruptedException {
        //Setup
        final String decisionScopeName = "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==";
        clearUpdatedConfiguration();

        //Action
        Optimize.updatePropositions(Collections.singletonList(new DecisionScope(decisionScopeName)), null, null);

        //Assert
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        List<Event> eventsListEdge = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertTrue(eventsListEdge.isEmpty());
    }

    //6
    @Test
    public void testUpdatePropositions_validAndInvalidDecisionScopes() throws InterruptedException {
        //Setup
        final String decisionScopeName1 = "eyJhY3Rpdml0eUlkIjoiIiwicGxhY2VtZW50SWQiOiJ4Y29yZTpvZmZlci1wbGFjZW1lbnQ6MTExMTExMTExMTExMTExMSJ9";
        final String decisionScopeName2 = "MyMbox";
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        //Action
        Optimize.updatePropositions(
                Arrays.asList(new DecisionScope(decisionScopeName1), new DecisionScope(decisionScopeName2))
                , null, null);

        //Assert
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        List<Event> eventsListEdge = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertNotNull(eventsListEdge);
        Assert.assertEquals(1, eventsListEdge.size());
        Event event = eventsListOptimize.get(0);
        Map<String, Object> eventData = event.getEventData();
        Assert.assertEquals(OptimizeTestConstants.EventType.OPTIMIZE, event.getType());
        Assert.assertEquals(OptimizeTestConstants.EventSource.REQUEST_CONTENT, event.getSource());
        Assert.assertTrue(eventData.size() > 0);
        Assert.assertEquals("updatepropositions", eventData.get("requesttype"));
        List<Map<String, String>> decisionScopes = (List<Map<String, String>>) eventData.get("decisionscopes");
        Assert.assertEquals(1, decisionScopes.size());
        Assert.assertEquals(decisionScopeName2, decisionScopes.get(0).get("name"));

        //Validating Event data of Edge Request event
        Event edgeEvent = eventsListEdge.get(0);
        Assert.assertNotNull(edgeEvent);
        Map<String, Object> edgeEventData = edgeEvent.getEventData();
        Assert.assertNotNull(edgeEventData);
        Assert.assertTrue(edgeEventData.size() > 0);
        Assert.assertEquals("personalization.request", ((Map<String, Object>) edgeEventData.get("xdm")).get("eventType"));
        Map<String, Object> personalizationMap = (Map<String, Object>) ((Map<String, Object>) edgeEventData.get("query")).get("personalization");
        List<String> decisionScopeList = (List<String>) personalizationMap.get("decisionScopes");
        Assert.assertNotNull(decisionScopeList);
        Assert.assertEquals(1, decisionScopeList.size());
        Assert.assertEquals(decisionScopeName2, decisionScopeList.get(0));
    }

    @Test
    public void testUpdatePropositions_validSurface() throws InterruptedException {
        //Setup
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        //Action
        Optimize.updatePropositionsForSurfacePaths(Collections.singletonList("myView#htmlElement"), null, null);

        //Assert
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        List<Event> eventsListEdge = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertNotNull(eventsListEdge);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertEquals(1, eventsListEdge.size());
        Event event = eventsListOptimize.get(0);
        Map<String, Object> eventData = event.getEventData();
        Assert.assertEquals(OptimizeTestConstants.EventType.OPTIMIZE, event.getType());
        Assert.assertEquals(OptimizeTestConstants.EventSource.REQUEST_CONTENT, event.getSource());
        Assert.assertTrue(eventData.size() > 0);
        Assert.assertEquals("updatepropositions", eventData.get("requesttype"));
        List<String> surfaces = (List<String>) eventData.get("surfaces");
        Assert.assertEquals(1, surfaces.size());
        Assert.assertEquals("myView#htmlElement", surfaces.get(0));

        //Validating Event data of Edge Request event
        Event edgeEvent = eventsListEdge.get(0);
        Assert.assertNotNull(edgeEvent);
        Map<String, Object> edgeEventData = edgeEvent.getEventData();
        Assert.assertNotNull(edgeEventData);
        Assert.assertTrue(edgeEventData.size() > 0);
        Assert.assertEquals("personalization.request", ((Map<String, Object>) edgeEventData.get("xdm")).get("eventType"));
        Map<String, Object> personalizationMap = (Map<String, Object>) ((Map<String, Object>) edgeEventData.get("query")).get("personalization");
        List<String> surfacesList = (List<String>) personalizationMap.get("surfaces");
        Assert.assertNotNull(surfacesList);
        Assert.assertEquals(1, surfacesList.size());
        Assert.assertEquals("mobileapp://com.adobe.marketing.mobile.optimize.test/myView#htmlElement", surfacesList.get(0));
    }

    @Test
    public void testUpdatePropositions_validSurfaceWithXdmAndDataAndDatasetId() throws InterruptedException {
        //Setup
        final String optimizeDatasetId = "111111111111111111111111";
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        configData.put("optimize.datasetId", optimizeDatasetId);
        updateConfiguration(configData);

        //Action
        Optimize.updatePropositionsForSurfacePaths(Collections.singletonList("myView#htmlElement"),
                new HashMap<String, Object>() {{
                    put("myXdmKey", "myXdmValue");
                }},
                new HashMap<String, Object>() {{
                    put("myKey", "myValue");
                }});

        //Assert
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        List<Event> eventsListEdge = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertNotNull(eventsListEdge);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertEquals(1, eventsListEdge.size());
        Event event = eventsListOptimize.get(0);
        Map<String, Object> eventData = event.getEventData();
        Assert.assertEquals(OptimizeTestConstants.EventType.OPTIMIZE, event.getType());
        Assert.assertEquals(OptimizeTestConstants.EventSource.REQUEST_CONTENT, event.getSource());
        Assert.assertTrue(eventData.size() > 0);
        Assert.assertEquals("updatepropositions", eventData.get("requesttype"));
        List<String> surfaces = (List<String>) eventData.get("surfaces");
        Assert.assertEquals(1, surfaces.size());
        Assert.assertEquals("myView#htmlElement", surfaces.get(0));
        Map<String, Object> xdm = (Map<String, Object>) eventData.get("xdm");
        Assert.assertEquals(1, xdm.size());
        Assert.assertEquals("myXdmValue", xdm.get("myXdmKey"));
        Map<String, Object> data = (Map<String, Object>) eventData.get("data");
        Assert.assertEquals(1, data.size());
        Assert.assertEquals("myValue", data.get("myKey"));

        //Validating Event data of Edge Request event
        Event edgeEvent = eventsListEdge.get(0);
        Assert.assertNotNull(edgeEvent);
        Map<String, Object> edgeEventData = edgeEvent.getEventData();
        Assert.assertNotNull(edgeEventData);
        Assert.assertTrue(edgeEventData.size() > 0);
        Assert.assertEquals(optimizeDatasetId, edgeEventData.get("datasetId"));
        Assert.assertEquals("personalization.request", ((Map<String, Object>) edgeEventData.get("xdm")).get("eventType"));
        Map<String, Object> personalizationMap = (Map<String, Object>) ((Map<String, Object>) edgeEventData.get("query")).get("personalization");
        List<String> surfacesList = (List<String>) personalizationMap.get("surfaces");
        Assert.assertNotNull(surfacesList);
        Assert.assertEquals(1, surfacesList.size());
        Assert.assertEquals("mobileapp://com.adobe.marketing.mobile.optimize.test/myView#htmlElement", surfacesList.get(0));
        Map<String, Object> xdmEdgeEvent = (Map<String, Object>) edgeEventData.get("xdm");
        Assert.assertEquals(2, xdmEdgeEvent.size());
        Assert.assertEquals("myXdmValue", xdmEdgeEvent.get("myXdmKey"));
        Assert.assertEquals("personalization.request", xdmEdgeEvent.get("eventType"));
        Map<String, Object> dataEdgeEvent = (Map<String, Object>) edgeEventData.get("data");
        Assert.assertEquals(1, dataEdgeEvent.size());
        Assert.assertEquals("myValue", dataEdgeEvent.get("myKey"));
    }

    @Test
    public void testUpdatePropositions_multipleValidSurface() throws InterruptedException {
        //Setup
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        //Action
        Optimize.updatePropositionsForSurfacePaths(new ArrayList<String>() {{
            add("myView/mySubview1");
            add("myView/mySubview2");
        }}, null, null);

        //Assert
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        List<Event> eventsListEdge = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertNotNull(eventsListEdge);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertEquals(1, eventsListEdge.size());
        Event event = eventsListOptimize.get(0);
        Map<String, Object> eventData = event.getEventData();
        Assert.assertEquals(OptimizeTestConstants.EventType.OPTIMIZE, event.getType());
        Assert.assertEquals(OptimizeTestConstants.EventSource.REQUEST_CONTENT, event.getSource());
        Assert.assertTrue(eventData.size() > 0);
        Assert.assertEquals("updatepropositions", eventData.get("requesttype"));
        List<String> surfaces = (List<String>) eventData.get("surfaces");
        Assert.assertEquals(2, surfaces.size());
        Assert.assertEquals("myView/mySubview1", surfaces.get(0));
        Assert.assertEquals("myView/mySubview2", surfaces.get(1));

        //Validating Event data of Edge Request event
        Event edgeEvent = eventsListEdge.get(0);
        Assert.assertNotNull(edgeEvent);
        Map<String, Object> edgeEventData = edgeEvent.getEventData();
        Assert.assertNotNull(edgeEventData);
        Assert.assertTrue(edgeEventData.size() > 0);
        Assert.assertEquals("personalization.request", ((Map<String, Object>) edgeEventData.get("xdm")).get("eventType"));
        Map<String, Object> personalizationMap = (Map<String, Object>) ((Map<String, Object>) edgeEventData.get("query")).get("personalization");
        List<String> surfacesList = (List<String>) personalizationMap.get("surfaces");
        Assert.assertNotNull(surfacesList);
        Assert.assertEquals(2, surfacesList.size());
        Assert.assertEquals("mobileapp://com.adobe.marketing.mobile.optimize.test/myView/mySubview1", surfacesList.get(0));
        Assert.assertEquals("mobileapp://com.adobe.marketing.mobile.optimize.test/myView/mySubview2", surfacesList.get(1));
    }

    @Test
    public void testUpdatePropositions_noSurface() throws InterruptedException {
        //Setup
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        //Action
        Optimize.updatePropositionsForSurfacePaths(new ArrayList<>(), null, null);

        //Assert
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        Assert.assertTrue(eventsListOptimize.isEmpty());
    }

    @Test
    public void testUpdatePropositions_emptySurface() throws InterruptedException {
        //Setup
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        //Action
        Optimize.updatePropositionsForSurfacePaths(new ArrayList<String>() {{ add(""); }}, null, null);

        //Assert
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        Assert.assertTrue(eventsListOptimize.isEmpty());
    }

    @Test
    public void testUpdatePropositions_validAndInvalidSurfaces() throws InterruptedException {
        //Setup
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        //Action
        Optimize.updatePropositionsForSurfacePaths(new ArrayList<String>() {{
            add("");
            add("myImageView#imageHtml");
        }}, null, null);

        //Assert
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        List<Event> eventsListEdge = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);

        Assert.assertNotNull(eventsListOptimize);
        Assert.assertEquals(1, eventsListOptimize.size());
        Assert.assertNotNull(eventsListEdge);
        Assert.assertEquals(1, eventsListEdge.size());
        Event event = eventsListOptimize.get(0);
        Map<String, Object> eventData = event.getEventData();
        Assert.assertEquals(OptimizeTestConstants.EventType.OPTIMIZE, event.getType());
        Assert.assertEquals(OptimizeTestConstants.EventSource.REQUEST_CONTENT, event.getSource());
        Assert.assertTrue(eventData.size() > 0);
        Assert.assertEquals("updatepropositions", eventData.get("requesttype"));
        List<String> surfaces = (List<String>) eventData.get("surfaces");
        Assert.assertEquals(1, surfaces.size());
        Assert.assertEquals("myImageView#imageHtml", surfaces.get(0));

        //Validating Event data of Edge Request event
        Event edgeEvent = eventsListEdge.get(0);
        Assert.assertNotNull(edgeEvent);
        Map<String, Object> edgeEventData = edgeEvent.getEventData();
        Assert.assertNotNull(edgeEventData);
        Assert.assertTrue(edgeEventData.size() > 0);
        Assert.assertEquals("personalization.request", ((Map<String, Object>) edgeEventData.get("xdm")).get("eventType"));
        Map<String, Object> personalizationMap = (Map<String, Object>) ((Map<String, Object>) edgeEventData.get("query")).get("personalization");
        List<String> surfacesList = (List<String>) personalizationMap.get("surfaces");
        Assert.assertNotNull(surfacesList);
        Assert.assertEquals(1, surfaces.size());
        Assert.assertEquals("mobileapp://com.adobe.marketing.mobile.optimize.test/myImageView#imageHtml", surfacesList.get(0));
    }

    //7a
    @Test
    public void testGetPropositions_decisionScopeInCache() throws InterruptedException, IOException {
        //setup
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);
        final String decisionScopeString = "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==";

        //Call updatePropositions to update the personalizationRequestEventId
        Optimize.updatePropositions(Collections.singletonList(new DecisionScope(decisionScopeString)), null, null);
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        Thread.sleep(1000);
        TestHelper.resetTestExpectations();

        //Send Edge Response event so that propositions will get cached by the Optimize SDK
        final String edgeResponseData = "{\n" +
                "                                  \"payload\": [\n" +
                "                                    {\n" +
                "                                        \"id\": \"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\n" +
                "                                        \"scope\": \"" + decisionScopeString + "\",\n" +
                "                                        \"activity\": {\n" +
                "                                            \"etag\": \"8\",\n" +
                "                                            \"id\": \"xcore:offer-activity:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"placement\": {\n" +
                "                                            \"etag\": \"1\",\n" +
                "                                            \"id\": \"xcore:offer-placement:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                \"etag\": \"10\",\n" +
                "                                                \"score\": 1,\n" +
                "                                                \"schema\": \"https://ns.adobe.com/experience/offer-management/content-component-html\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                    \"format\": \"text/html\",\n" +
                "                                                    \"content\": \"<h1>This is HTML content</h1>\",\n" +
                "                                                    \"characteristics\": {\n" +
                "                                                        \"testing\": \"true\"\n" +
                "                                                    }\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    }\n" +
                "                                  ],\n" +
                "                                \"requestEventId\": \"" + eventsListOptimize.get(0).getUniqueIdentifier() + "\",\n" +
                "                                \"requestId\": \"BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB\",\n" +
                "                                \"type\": \"personalization:decisions\"\n" +
                "                              }";

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = objectMapper.readValue(edgeResponseData, new TypeReference<Map<String, Object>>() {});

        Event event = new Event.Builder(
                "AEP Response Event Handle",
                OptimizeTestConstants.EventType.EDGE,
                OptimizeTestConstants.EventSource.PERSONALIZATION).
                setEventData(eventData).
                build();

        //Action
        MobileCore.dispatchEvent(event);

        Thread.sleep(1000);
        TestHelper.resetTestExpectations();
        DecisionScope decisionScope = new DecisionScope(decisionScopeString);
        final Map<DecisionScope, Proposition> propositionMap = new HashMap<>();
        final ADBCountDownLatch countDownLatch = new ADBCountDownLatch(1);
        Optimize.getPropositions(Collections.singletonList(decisionScope), new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                Assert.fail("Error in getting cached propositions");
            }

            @Override
            public void call(Map<DecisionScope, Proposition> decisionScopePropositionMap) {
                propositionMap.putAll(decisionScopePropositionMap);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await(10, TimeUnit.SECONDS);
        //Assertions
        List<Event> optimizeResponseEventsList = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.RESPONSE_CONTENT);

        Assert.assertNotNull(optimizeResponseEventsList);
        Assert.assertEquals(1, optimizeResponseEventsList.size());
        Assert.assertNull(optimizeResponseEventsList.get(0).getEventData().get("responseerror"));
        Assert.assertEquals(1, propositionMap.size());
        Proposition proposition = propositionMap.get(decisionScope);
        Assert.assertNotNull(proposition);
        Assert.assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", proposition.getId());
        Assert.assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", proposition.getScope());
        Assert.assertEquals(1, proposition.getOffers().size());

        Offer offer = proposition.getOffers().get(0);
        Assert.assertEquals("xcore:personalized-offer:1111111111111111", offer.getId());
        Assert.assertEquals("10", offer.getEtag());
        Assert.assertEquals(1, offer.getScore());
        Assert.assertEquals("https://ns.adobe.com/experience/offer-management/content-component-html", offer.getSchema());
        Assert.assertEquals(OfferType.HTML, offer.getType());
        Assert.assertEquals("<h1>This is HTML content</h1>", offer.getContent());
        Assert.assertEquals(1, offer.getCharacteristics().size());
        Assert.assertEquals("true", offer.getCharacteristics().get("testing"));
    }

    //7b
    @Test
    public void testGetPropositions_decisionScopeInCacheFromTargetResponseWithClickTracking() throws ClassCastException, InterruptedException,IOException {
        //setup
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);
        final String decisionScopeString = "myMbox1";

        //Call updatePropositions to update the personalizationRequestEventId
        Optimize.updatePropositions(Collections.singletonList(new DecisionScope(decisionScopeString)), null, null);
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        Thread.sleep(1000);
        TestHelper.resetTestExpectations();

        //Send Edge Response event so that propositions will get cached by the Optimize SDK
        final String edgeResponseData = "{\n" +
                "                                  \"payload\": [\n" +
                "                                    {\n" +
                "                                        \"id\": \"AT:eyJhY3Rpdml0eUlkIjoiMTExMTExIiwiZXhwZXJpZW5jZUlkIjoiMCJ9\",\n" +
                "                                        \"scope\": \"" + decisionScopeString + "\",\n" +
                "                                        \"scopeDetails\": {\n" +
                "                                            \"decisionProvider\": \"TGT\",\n" +
                "                                            \"activity\": {\n" +
                "                                               \"id\": \"111111\"\n" +
                "                                             },\n" +
                "                                            \"experience\": {\n" +
                "                                               \"id\": \"0\"\n" +
                "                                             },\n" +
                "                                            \"strategies\": [\n" +
                "                                               {\n" +
                "                                                  \"step\": \"entry\",\n" +
                "                                                  \"algorithmID\": \"0\",\n" +
                "                                                  \"trafficType\": \"0\"\n" +
                "                                               },\n" +
                "                                               {\n" +
                "                                                  \"step\": \"display\",\n" +
                "                                                  \"algorithmID\": \"0\",\n" +
                "                                                  \"trafficType\": \"0\"\n" +
                "                                               }\n" +
                "                                             ],\n" +
                "                                            \"characteristics\": {\n" +
                "                                               \"stateToken\": \"SGFZpwAqaqFTayhAT2xsgzG3+2fw4m+O9FK8c0QoOHfxVkH1ttT1PGBX3/jV8a5uFF0fAox6CXpjJ1PGRVQBjHl9Zc6mRxY9NQeM7rs/3Es1RHPkzBzyhpVS6eg9q+kw\",\n" +
                "                                               \"eventTokens\": {\n" +
                "                                                   \"display\": \"MmvRrL5aB4Jz36JappRYg2qipfsIHvVzTQxHolz2IpSCnQ9Y9OaLL2gsdrWQTvE54PwSz67rmXWmSnkXpSSS2Q==\",\n" +
                "                                                   \"click\": \"EZDMbI2wmAyGcUYLr3VpmA==\"\n" +
                "                                               }\n" +
                "                                             }\n" +
                "                                        },\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"0\",\n" +
                "                                                \"schema\": \"https://ns.adobe.com/personalization/json-content-item\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"id\": \"0\",\n" +
                "                                                    \"format\": \"application/json\",\n" +
                "                                                    \"content\": {\n" +
                "                                                       \"device\": \"mobile\"\n" +
                "                                                     }\n" +
                "                                                }\n" +
                "                                            },\n" +
                "                                            {\n" +
                "                                                \"id\": \"111111\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"type\": \"click\",\n" +
                "                                                    \"format\": \"application/vnd.adobe.target.metric\"\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    }\n" +
                "                                  ],\n" +
                "                                \"requestEventId\": \"" + eventsListOptimize.get(0).getUniqueIdentifier() + "\",\n" +
                "                                \"requestId\": \"BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB\",\n" +
                "                                \"type\": \"personalization:decisions\"\n" +
                "                              }";

        final ObjectMapper objectMapper = new ObjectMapper();
        final Map<String, Object> eventData = objectMapper.readValue(edgeResponseData, new TypeReference<Map<String, Object>>() {});

        final Event event = new Event.Builder(
                "AEP Response Event Handle",
                OptimizeTestConstants.EventType.EDGE,
                OptimizeTestConstants.EventSource.PERSONALIZATION).
                setEventData(eventData).
                build();

        //Action
        MobileCore.dispatchEvent(event);

        Thread.sleep(1000);
        TestHelper.resetTestExpectations();
        final DecisionScope decisionScope = new DecisionScope(decisionScopeString);
        final Map<DecisionScope, Proposition> propositionMap = new HashMap<>();
        final ADBCountDownLatch countDownLatch = new ADBCountDownLatch(1);
        Optimize.getPropositions(Collections.singletonList(decisionScope), new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                Assert.fail("Error in getting cached propositions");
            }

            @Override
            public void call(Map<DecisionScope, Proposition> decisionScopePropositionMap) {
                propositionMap.putAll(decisionScopePropositionMap);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await(1, TimeUnit.SECONDS);
        //Assertions
        final List<Event> optimizeResponseEventsList = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.RESPONSE_CONTENT);
        Assert.assertNotNull(optimizeResponseEventsList);
        Assert.assertEquals(1, optimizeResponseEventsList.size());
        Assert.assertNull(optimizeResponseEventsList.get(0).getEventData().get("responseerror"));
        Assert.assertEquals(1, propositionMap.size());

        final Proposition proposition = propositionMap.get(decisionScope);
        Assert.assertNotNull(proposition);
        Assert.assertEquals("AT:eyJhY3Rpdml0eUlkIjoiMTExMTExIiwiZXhwZXJpZW5jZUlkIjoiMCJ9", proposition.getId());
        Assert.assertEquals("myMbox1", proposition.getScope());

        final Map<String, Object> scopeDetails = proposition.getScopeDetails();
        Assert.assertEquals(5, scopeDetails.size());
        Assert.assertEquals("TGT", scopeDetails.get("decisionProvider"));
        final Map<String, Object> activity = (Map<String, Object>)scopeDetails.get("activity");
        Assert.assertNotNull(activity);
        Assert.assertEquals(1, activity.size());
        Assert.assertEquals("111111", activity.get("id"));
        final Map<String, Object> experience = (Map<String, Object>)scopeDetails.get("experience");
        Assert.assertNotNull(experience);
        Assert.assertEquals(1, experience.size());
        Assert.assertEquals("0", experience.get("id"));
        final List<Map<String, Object>> strategies = (List<Map<String, Object>>)scopeDetails.get("strategies");
        Assert.assertNotNull(strategies);
        Assert.assertEquals(2, strategies.size());
        final Map<String, Object> strategy0 = strategies.get(0);
        Assert.assertNotNull(strategy0);
        Assert.assertEquals(3, strategy0.size());
        Assert.assertEquals("entry", strategy0.get("step"));
        Assert.assertEquals("0", strategy0.get("algorithmID"));
        Assert.assertEquals("0", strategy0.get("trafficType"));
        final Map<String, Object> strategy1 = strategies.get(1);
        Assert.assertNotNull(strategy1);
        Assert.assertEquals(3, strategy1.size());
        Assert.assertEquals("display", strategy1.get("step"));
        Assert.assertEquals("0", strategy1.get("algorithmID"));
        Assert.assertEquals("0", strategy1.get("trafficType"));
        final Map <String, Object> characteristics = (Map<String, Object>)scopeDetails.get("characteristics");
        Assert.assertNotNull(characteristics);
        Assert.assertEquals(2, characteristics.size());
        Assert.assertEquals("SGFZpwAqaqFTayhAT2xsgzG3+2fw4m+O9FK8c0QoOHfxVkH1ttT1PGBX3/jV8a5uFF0fAox6CXpjJ1PGRVQBjHl9Zc6mRxY9NQeM7rs/3Es1RHPkzBzyhpVS6eg9q+kw", characteristics.get("stateToken"));
        final Map<String, Object> eventTokens = (Map<String, Object>)characteristics.get("eventTokens");
        Assert.assertNotNull(eventTokens);
        Assert.assertEquals(2, eventTokens.size());
        Assert.assertEquals("MmvRrL5aB4Jz36JappRYg2qipfsIHvVzTQxHolz2IpSCnQ9Y9OaLL2gsdrWQTvE54PwSz67rmXWmSnkXpSSS2Q==", eventTokens.get("display"));
        Assert.assertEquals("EZDMbI2wmAyGcUYLr3VpmA==", eventTokens.get("click"));

        Assert.assertEquals(1, proposition.getOffers().size());
        final Offer offer = proposition.getOffers().get(0);
        Assert.assertEquals("0", offer.getId());
        Assert.assertEquals("https://ns.adobe.com/personalization/json-content-item", offer.getSchema());
        Assert.assertEquals(OfferType.JSON, offer.getType());
        Assert.assertEquals("{\"device\":\"mobile\"}", offer.getContent());
        Assert.assertNull(offer.getCharacteristics());
        Assert.assertNull(offer.getLanguage());
    }

    //8
    @Test
    public void testGetPropositions_notAllDecisionScopesInCache() throws IOException, InterruptedException {
        //setup
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);
        final String decisionScopeString = "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==";

        //Call updatePropositions to update the personalizationRequestEventId
        Optimize.updatePropositions(Collections.singletonList(new DecisionScope(decisionScopeString)), null, null);
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        Thread.sleep(1000);
        TestHelper.resetTestExpectations();

        //Send Edge Response event so that propositions will get cached by the Optimize SDK
        final String edgeResponseData = "{\n" +
                "                                  \"payload\": [\n" +
                "                                    {\n" +
                "                                        \"id\": \"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\n" +
                "                                        \"scope\": \"" + decisionScopeString + "\",\n" +
                "                                        \"activity\": {\n" +
                "                                            \"etag\": \"8\",\n" +
                "                                            \"id\": \"xcore:offer-activity:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"placement\": {\n" +
                "                                            \"etag\": \"1\",\n" +
                "                                            \"id\": \"xcore:offer-placement:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                \"etag\": \"10\",\n" +
                "                                                \"schema\": \"https://ns.adobe.com/experience/offer-management/content-component-html\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                    \"format\": \"text/html\",\n" +
                "                                                    \"content\": \"<h1>This is HTML content</h1>\",\n" +
                "                                                    \"characteristics\": {\n" +
                "                                                        \"testing\": \"true\"\n" +
                "                                                    }\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    }\n" +
                "                                  ],\n" +
                "                                \"requestEventId\": \"" + eventsListOptimize.get(0).getUniqueIdentifier() + "\",\n" +
                "                                \"requestId\": \"BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB\",\n" +
                "                                \"type\": \"personalization:decisions\"\n" +
                "                              }";

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = objectMapper.readValue(edgeResponseData, Map.class);

        Event event = new Event.Builder(
                "AEP Response Event Handle",
                OptimizeTestConstants.EventType.EDGE,
                OptimizeTestConstants.EventSource.PERSONALIZATION).
                setEventData(eventData).build();

        //Action
        MobileCore.dispatchEvent(event);

        Thread.sleep(1000);
        TestHelper.resetTestExpectations();
        DecisionScope decisionScope1 = new DecisionScope(decisionScopeString);
        DecisionScope decisionScope2 = new DecisionScope("myMbox");
        final Map<DecisionScope, Proposition> propositionMap = new HashMap<>();
        final ADBCountDownLatch countDownLatch = new ADBCountDownLatch(1);
        Optimize.getPropositions(Arrays.asList(decisionScope1, decisionScope2), new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                Assert.fail("Error in getting cached propositions");
            }

            @Override
            public void call(Map<DecisionScope, Proposition> decisionScopePropositionMap) {
                propositionMap.putAll(decisionScopePropositionMap);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await(1, TimeUnit.SECONDS);
        //Assertions
        List<Event> optimizeResponseEventsList = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.RESPONSE_CONTENT);

        Assert.assertNotNull(optimizeResponseEventsList);
        Assert.assertEquals(1, optimizeResponseEventsList.size());
        Assert.assertNull(optimizeResponseEventsList.get(0).getEventData().get("responseerror"));
        Assert.assertEquals(1, propositionMap.size());

        Assert.assertTrue(propositionMap.containsKey(decisionScope1));
        Assert.assertFalse(propositionMap.containsKey(decisionScope2)); //Decision scope myMbox is not present in cache

        Proposition proposition = propositionMap.get(decisionScope1);
        Assert.assertNotNull(proposition);
        Assert.assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", proposition.getId());
        Assert.assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", proposition.getScope());
        Assert.assertEquals(1, proposition.getOffers().size());

        Offer offer = proposition.getOffers().get(0);
        Assert.assertEquals("xcore:personalized-offer:1111111111111111", offer.getId());
        Assert.assertEquals("https://ns.adobe.com/experience/offer-management/content-component-html", offer.getSchema());
        Assert.assertEquals(OfferType.HTML, offer.getType());
        Assert.assertEquals("<h1>This is HTML content</h1>", offer.getContent());
        Assert.assertEquals(1, offer.getCharacteristics().size());
        Assert.assertEquals("true", offer.getCharacteristics().get("testing"));
    }

    //9
    @Test
    public void testGetPropositions_noDecisionScopeInCache() throws IOException, InterruptedException {
        //setup
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);
        final String decisionScopeString = "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==";

        //Call updatePropositions to update the personalizationRequestEventId
        Optimize.updatePropositions(Collections.singletonList(new DecisionScope(decisionScopeString)), null, null);
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        Thread.sleep(1000);
        TestHelper.resetTestExpectations();

        //Send Edge Response event so that propositions will get cached by the Optimize SDK
        final String edgeResponseData = "{\n" +
                "                                  \"payload\": [\n" +
                "                                    {\n" +
                "                                        \"id\": \"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\n" +
                "                                        \"scope\": \"" + decisionScopeString + "\",\n" +
                "                                        \"activity\": {\n" +
                "                                            \"etag\": \"8\",\n" +
                "                                            \"id\": \"xcore:offer-activity:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"placement\": {\n" +
                "                                            \"etag\": \"1\",\n" +
                "                                            \"id\": \"xcore:offer-placement:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                \"etag\": \"10\",\n" +
                "                                                \"schema\": \"https://ns.adobe.com/experience/offer-management/content-component-html\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                    \"format\": \"text/html\",\n" +
                "                                                    \"content\": \"<h1>This is HTML content</h1>\",\n" +
                "                                                    \"characteristics\": {\n" +
                "                                                        \"testing\": \"true\"\n" +
                "                                                    }\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    }\n" +
                "                                  ],\n" +
                "                                \"requestEventId\": \"" + eventsListOptimize.get(0).getUniqueIdentifier() + "\",\n" +
                "                                \"requestId\": \"BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB\",\n" +
                "                                \"type\": \"personalization:decisions\"\n" +
                "                              }";

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = objectMapper.readValue(edgeResponseData, new TypeReference<Map<String, Object>>() {});

        Event event = new Event.Builder(
                "AEP Response Event Handle",
                OptimizeTestConstants.EventType.EDGE,
                OptimizeTestConstants.EventSource.PERSONALIZATION).
                setEventData(eventData).
                build();

        //Action
        MobileCore.dispatchEvent(event);

        Thread.sleep(1000);
        TestHelper.resetTestExpectations();
        DecisionScope decisionScope1 = new DecisionScope("myMbox1");
        DecisionScope decisionScope2 = new DecisionScope("myMbox2");
        final Map<DecisionScope, Proposition> propositionMap = new HashMap<>();
        final ADBCountDownLatch countDownLatch = new ADBCountDownLatch(1);
        Optimize.getPropositions(Arrays.asList(decisionScope1, decisionScope2), new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                Assert.fail("Error in getting cached propositions");
            }

            @Override
            public void call(Map<DecisionScope, Proposition> decisionScopePropositionMap) {
                propositionMap.putAll(decisionScopePropositionMap);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await(1, TimeUnit.SECONDS);
        //Assertions
        List<Event> optimizeResponseEventsList = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.RESPONSE_CONTENT);

        Assert.assertNotNull(optimizeResponseEventsList);
        Assert.assertEquals(1, optimizeResponseEventsList.size());
        Assert.assertNull(optimizeResponseEventsList.get(0).getEventData().get("responseerror"));
        Assert.assertEquals(0, propositionMap.size());

        Assert.assertFalse(propositionMap.containsKey(decisionScope1)); //Decision scope myMbox1 is not present in cache
        Assert.assertFalse(propositionMap.containsKey(decisionScope2)); //Decision scope myMbox2 is not present in cache
    }

    //10
    @Test
    public void testGetPropositions_emptyCache() throws InterruptedException {
        //setup
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);
        final DecisionScope decisionScope1 = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
        final DecisionScope decisionScope2 = new DecisionScope("myMbox");

        //Action
        final ADBCountDownLatch countDownLatch = new ADBCountDownLatch(1);
        final Map<DecisionScope, Proposition> propositionMap = new HashMap<>();
        TestHelper.resetTestExpectations();
        Optimize.getPropositions(Arrays.asList(decisionScope1, decisionScope2), new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                Assert.fail("Error in getting Propositions.");
            }

            @Override
            public void call(Map<DecisionScope, Proposition> decisionScopePropositionMap) {
                propositionMap.putAll(decisionScopePropositionMap);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await(1, TimeUnit.SECONDS);
        //Assertions
        List<Event> optimizeResponseEventsList = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.RESPONSE_CONTENT);

        Assert.assertNotNull(optimizeResponseEventsList);
        Assert.assertEquals(1, optimizeResponseEventsList.size());
        Assert.assertNull(optimizeResponseEventsList.get(0).getEventData().get("responseerror"));
        Assert.assertEquals(0, propositionMap.size());

        Assert.assertFalse(propositionMap.containsKey(decisionScope1));
        Assert.assertFalse(propositionMap.containsKey(decisionScope2));
    }

    @Test
    public void testGetPropositions_surfaceInCache() throws InterruptedException, IOException {
        //setup
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        //Call updatePropositions to update the personalizationRequestEventId
        Optimize.updatePropositionsForSurfacePaths(Collections.singletonList("myView#htmlElement"), null, null);
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        Thread.sleep(1000);
        TestHelper.resetTestExpectations();

        //Send Edge Response event so that propositions will get cached by the Optimize SDK
        final String edgeResponseData = "{\n" +
                "                                  \"payload\": [\n" +
                "                                    {\n" +
                "                                        \"id\": \"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\n" +
                "                                        \"scope\": \"mobileapp://com.adobe.marketing.mobile.optimize.test/myView#htmlElement\",\n" +
                "                                        \"scopeDetails\": {\n" +
                "                                            \"correlationID\": \"cccccccc-cccc-cccc-cccc-cccccccccccc\",\n" +
                "                                            \"characteristics\": {\n" +
                "                                               \"eventToken\": \"someToken\"\n" +
                "                                            },\n" +
                "                                            \"decisionProvider\": \"AJO\",\n" +
                "                                            \"activity\": {\n" +
                "                                               \"id\": \"dddddddd-dddd-dddd-dddd-dddddddddddd#eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee\"\n" +
                "                                            }\n" +
                "                                        },\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb\",\n" +
                "                                                \"schema\": \"https://ns.adobe.com/personalization/html-content-item\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"content\": \"<h1>This is HTML content</h1>\"\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    }\n" +
                "                                  ],\n" +
                "                                \"requestEventId\": \"" + eventsListOptimize.get(0).getUniqueIdentifier() + "\",\n" +
                "                                \"requestId\": \"FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF\",\n" +
                "                                \"type\": \"personalization:decisions\"\n" +
                "                              }";

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = objectMapper.readValue(edgeResponseData, new TypeReference<Map<String, Object>>() {});

        Event event = new Event.Builder(
                "AEP Response Event Handle",
                OptimizeTestConstants.EventType.EDGE,
                OptimizeTestConstants.EventSource.PERSONALIZATION).
                setEventData(eventData).
                build();

        //Action
        MobileCore.dispatchEvent(event);

        Thread.sleep(1000);
        TestHelper.resetTestExpectations();
        final Map<String, Proposition> propositionMap = new HashMap<>();
        final ADBCountDownLatch countDownLatch = new ADBCountDownLatch(1);
        Optimize.getPropositionsForSurfacePaths(Collections.singletonList("myView#htmlElement"), new AdobeCallbackWithError<Map<String, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                Assert.fail("Error in getting cached propositions");
            }

            @Override
            public void call(Map<String, Proposition> surfacePropositionMap) {
                propositionMap.putAll(surfacePropositionMap);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await(1, TimeUnit.SECONDS);
        //Assertions
        List<Event> optimizeResponseEventsList = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.RESPONSE_CONTENT);

        Assert.assertNotNull(optimizeResponseEventsList);
        Assert.assertEquals(1, optimizeResponseEventsList.size());
        Assert.assertNull(optimizeResponseEventsList.get(0).getEventData().get("responseerror"));
        Assert.assertEquals(1, propositionMap.size());
        Proposition proposition = propositionMap.get("myView#htmlElement");
        Assert.assertNotNull(proposition);
        Assert.assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", proposition.getId());
        Assert.assertEquals("mobileapp://com.adobe.marketing.mobile.optimize.test/myView#htmlElement", proposition.getScope());
        Assert.assertEquals(1, proposition.getOffers().size());
        Assert.assertEquals(4, proposition.getScopeDetails().size());
        Assert.assertEquals("cccccccc-cccc-cccc-cccc-cccccccccccc", proposition.getScopeDetails().get("correlationID"));
        Assert.assertEquals("AJO", proposition.getScopeDetails().get("decisionProvider"));
        Map<String, Object> characteristics = (Map<String, Object>) proposition.getScopeDetails().get("characteristics");
        Assert.assertEquals("someToken", characteristics.get("eventToken"));
        Map<String, Object> activity = (Map<String, Object>) proposition.getScopeDetails().get("activity");
        Assert.assertEquals("dddddddd-dddd-dddd-dddd-dddddddddddd#eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee", activity.get("id"));

        Assert.assertEquals(1, proposition.getOffers().size());
        final Offer offer = proposition.getOffers().get(0);
        Assert.assertEquals("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", offer.getId());
        Assert.assertEquals("https://ns.adobe.com/personalization/html-content-item", offer.getSchema());
        Assert.assertEquals(OfferType.UNKNOWN, offer.getType());
        Assert.assertEquals("<h1>This is HTML content</h1>", offer.getContent());
    }

    @Test
    public void testGetPropositions_noSurfaceInCache() throws InterruptedException, IOException {
        //setup
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        // Action
        final Map<String, Proposition> propositionMap = new HashMap<>();
        final ADBCountDownLatch countDownLatch = new ADBCountDownLatch(1);
        Optimize.getPropositionsForSurfacePaths(Collections.singletonList("myView#htmlElement"), new AdobeCallbackWithError<Map<String, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                Assert.fail("Error in getting cached propositions");
            }

            @Override
            public void call(Map<String, Proposition> surfacePropositionMap) {
                propositionMap.putAll(surfacePropositionMap);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await(1, TimeUnit.SECONDS);
        //Assertions
        List<Event> optimizeResponseEventsList = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.RESPONSE_CONTENT);

        Assert.assertNotNull(optimizeResponseEventsList);
        Assert.assertEquals(1, optimizeResponseEventsList.size());
        Assert.assertNull(optimizeResponseEventsList.get(0).getEventData().get("responseerror"));
        Assert.assertEquals(0, propositionMap.size());
    }

    @Test
    public void testGetPropositions_noSurface() throws InterruptedException, IOException {
        //setup
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        // Action
        final Map<String, Proposition> propositionMap = new HashMap<>();
        final AdobeError[] error = new AdobeError[1];
        final ADBCountDownLatch countDownLatch = new ADBCountDownLatch(1);
        final ADBCountDownLatch countDownLatch2 = new ADBCountDownLatch(1);
        Optimize.getPropositionsForSurfacePaths(new ArrayList<>(), new AdobeCallbackWithError<Map<String, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                countDownLatch2.countDown();
                error[0] = adobeError;
            }

            @Override
            public void call(Map<String, Proposition> surfacePropositionMap) {
                propositionMap.putAll(surfacePropositionMap);
                countDownLatch.countDown();
            }
        });


        Assert.assertFalse(countDownLatch.await(1, TimeUnit.SECONDS));
        Assert.assertTrue(countDownLatch2.await(1, TimeUnit.SECONDS));
        Assert.assertEquals(AdobeError.UNEXPECTED_ERROR, error[0]);

        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        Assert.assertTrue(eventsListOptimize.isEmpty());
    }

    @Test
    public void testGetPropositions_emptySurface() throws InterruptedException, IOException {
        //setup
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        // Action
        final Map<String, Proposition> propositionMap = new HashMap<>();
        final AdobeError[] error = new AdobeError[1];
        final ADBCountDownLatch countDownLatch = new ADBCountDownLatch(1);
        final ADBCountDownLatch countDownLatch2 = new ADBCountDownLatch(1);
        Optimize.getPropositionsForSurfacePaths(Collections.singletonList(""), new AdobeCallbackWithError<Map<String, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                countDownLatch2.countDown();
                error[0] = adobeError;
            }

            @Override
            public void call(Map<String, Proposition> surfacePropositionMap) {
                propositionMap.putAll(surfacePropositionMap);
                countDownLatch.countDown();
            }
        });


        Assert.assertFalse(countDownLatch.await(1, TimeUnit.SECONDS));
        Assert.assertTrue(countDownLatch2.await(1, TimeUnit.SECONDS));
        Assert.assertEquals(AdobeError.UNEXPECTED_ERROR, error[0]);

        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        Assert.assertTrue(eventsListOptimize.isEmpty());
    }

    @Test
    public void testGetPropositions_validAndInvalidSurface() throws InterruptedException, IOException {
        //setup
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        //Call updatePropositions to update the personalizationRequestEventId
        Optimize.updatePropositionsForSurfacePaths(Collections.singletonList("myView#htmlElement"), null, null);
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        Thread.sleep(1000);
        TestHelper.resetTestExpectations();

        //Send Edge Response event so that propositions will get cached by the Optimize SDK
        final String edgeResponseData = "{\n" +
                "                                  \"payload\": [\n" +
                "                                    {\n" +
                "                                        \"id\": \"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\n" +
                "                                        \"scope\": \"mobileapp://com.adobe.marketing.mobile.optimize.test/myView#htmlElement\",\n" +
                "                                        \"scopeDetails\": {\n" +
                "                                            \"correlationID\": \"cccccccc-cccc-cccc-cccc-cccccccccccc\",\n" +
                "                                            \"characteristics\": {\n" +
                "                                               \"eventToken\": \"someToken\"\n" +
                "                                            },\n" +
                "                                            \"decisionProvider\": \"AJO\",\n" +
                "                                            \"activity\": {\n" +
                "                                               \"id\": \"dddddddd-dddd-dddd-dddd-dddddddddddd#eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee\"\n" +
                "                                            }\n" +
                "                                        },\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb\",\n" +
                "                                                \"schema\": \"https://ns.adobe.com/personalization/html-content-item\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"content\": \"<h1>This is HTML content</h1>\"\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    }\n" +
                "                                  ],\n" +
                "                                \"requestEventId\": \"" + eventsListOptimize.get(0).getUniqueIdentifier() + "\",\n" +
                "                                \"requestId\": \"FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF\",\n" +
                "                                \"type\": \"personalization:decisions\"\n" +
                "                              }";

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = objectMapper.readValue(edgeResponseData, new TypeReference<Map<String, Object>>() {});

        Event event = new Event.Builder(
                "AEP Response Event Handle",
                OptimizeTestConstants.EventType.EDGE,
                OptimizeTestConstants.EventSource.PERSONALIZATION).
                setEventData(eventData).
                build();

        //Action
        MobileCore.dispatchEvent(event);

        Thread.sleep(1000);
        TestHelper.resetTestExpectations();
        final Map<String, Proposition> propositionMap = new HashMap<>();
        final ADBCountDownLatch countDownLatch = new ADBCountDownLatch(1);
        Optimize.getPropositionsForSurfacePaths(new ArrayList<String>() {{
            add("myView#htmlElement");
            add("");
        }}, new AdobeCallbackWithError<Map<String, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                Assert.fail("Error in getting cached propositions");
            }

            @Override
            public void call(Map<String, Proposition> surfacePropositionMap) {
                propositionMap.putAll(surfacePropositionMap);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await(1, TimeUnit.SECONDS);
        //Assertions
        List<Event> eventsRequestListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        Assert.assertEquals(1, eventsRequestListOptimize.size());

        List<Event> optimizeResponseEventsList = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.RESPONSE_CONTENT);

        Assert.assertNotNull(optimizeResponseEventsList);
        Assert.assertEquals(1, optimizeResponseEventsList.size());
        Assert.assertNull(optimizeResponseEventsList.get(0).getEventData().get("responseerror"));
        Assert.assertEquals(1, propositionMap.size());
        Proposition proposition = propositionMap.get("myView#htmlElement");
        Assert.assertNotNull(proposition);
        Assert.assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", proposition.getId());
        Assert.assertEquals("mobileapp://com.adobe.marketing.mobile.optimize.test/myView#htmlElement", proposition.getScope());
        Assert.assertEquals(1, proposition.getOffers().size());
        Assert.assertEquals(4, proposition.getScopeDetails().size());
        Assert.assertEquals("cccccccc-cccc-cccc-cccc-cccccccccccc", proposition.getScopeDetails().get("correlationID"));
        Assert.assertEquals("AJO", proposition.getScopeDetails().get("decisionProvider"));
        Map<String, Object> characteristics = (Map<String, Object>) proposition.getScopeDetails().get("characteristics");
        Assert.assertEquals("someToken", characteristics.get("eventToken"));
        Map<String, Object> activity = (Map<String, Object>) proposition.getScopeDetails().get("activity");
        Assert.assertEquals("dddddddd-dddd-dddd-dddd-dddddddddddd#eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee", activity.get("id"));

        Assert.assertEquals(1, proposition.getOffers().size());
        final Offer offer = proposition.getOffers().get(0);
        Assert.assertEquals("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", offer.getId());
        Assert.assertEquals("https://ns.adobe.com/personalization/html-content-item", offer.getSchema());
        Assert.assertEquals(OfferType.UNKNOWN, offer.getType());
        Assert.assertEquals("<h1>This is HTML content</h1>", offer.getContent());
    }

    //11
    @Test
    public void testTrackPropositions_validPropositionInteractionsForDisplay() throws InterruptedException {
        //setup
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        Offer offer = new Offer.Builder("xcore:personalized-offer:1111111111111111", OfferType.TEXT, "Text Offer!!").build();
        Proposition proposition = new Proposition(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                Collections.singletonList(offer),
                "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==",
                Collections.emptyMap()
        );

        //Action
        TestHelper.resetTestExpectations();
        offer.displayed();

        //Assert
        List<Event> optimizeRequestEventsList = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        List<Event> edgeRequestEventList = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);

        Assert.assertNotNull(optimizeRequestEventsList);
        Assert.assertEquals(1, optimizeRequestEventsList.size());
        Assert.assertNotNull(edgeRequestEventList);
        Assert.assertEquals(1, edgeRequestEventList.size());

        Map<String, Object> xdm = (Map<String, Object>) edgeRequestEventList.get(0).getEventData().get("xdm");
        Assert.assertEquals("decisioning.propositionDisplay", xdm.get("eventType"));

        List<Map<String, Object>> propositionList = (List<Map<String, Object>>) ((Map<String, Object>) ((Map<String, Object>) xdm.get("_experience")).get("decisioning"))
                .get("propositions");
        Assert.assertNotNull(propositionList);
        Assert.assertEquals(1, propositionList.size());
        Map<String, Object> propositionData = propositionList.get(0);
        List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) propositionData.get("propositions");
        Assert.assertNotNull(propositionList);
        Assert.assertEquals(1, propositionList.size());
        Map<String, Object> propositionMap = propositionList.get(0);
        Assert.assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", propositionMap.get("id"));
        Assert.assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", propositionMap.get("scope"));
        Assert.assertTrue(((Map<String, Object>) propositionMap.get("scopeDetails")).isEmpty());
        List<Map<String, Object>> itemsList = (List<Map<String, Object>>) propositionMap.get("items");
        Assert.assertNotNull(itemsList);
        Assert.assertEquals(1, itemsList.size());
        Assert.assertEquals("xcore:personalized-offer:1111111111111111", itemsList.get(0).get("id"));
    }

    //12
    @Test
    public void testTrackPropositions_validPropositionInteractionsForTap() throws IOException, InterruptedException {
        //setup
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);
        final String testScopeDetails = "        {\n" +
                "        \"decisionProvider\": \"TGT\",\n" +
                "                \"activity\": {\n" +
                "        \"id\": \"125589\"\n" +
                "            },\n" +
                "        \"experience\": {\n" +
                "        \"id\": \"0\"\n" +
                "            },\n" +
                "        \"strategies\": [\n" +
                "                {\n" +
                "        \"algorithmID\": \"0\",\n" +
                "                \"trafficType\": \"0\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n";
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> testDecisionScopesMap = objectMapper.readValue(testScopeDetails, new TypeReference<Map<String, Object>>() {});

        Offer offer = new Offer.Builder("246315", OfferType.TEXT, "Text Offer!!").build();
        //Set the proposition soft reference to Offer
        Proposition proposition = new Proposition(
                "AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9",
                Collections.singletonList(offer),
                "myMbox",
                testDecisionScopesMap
        );

        //Action
        TestHelper.resetTestExpectations();
        offer.tapped();

        //Assert
        List<Event> optimizeRequestEventsList = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        List<Event> edgeRequestEventList = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);

        Assert.assertNotNull(optimizeRequestEventsList);
        Assert.assertEquals(1, optimizeRequestEventsList.size());
        Assert.assertNotNull(edgeRequestEventList);
        Assert.assertEquals(1, edgeRequestEventList.size());

        Map<String, Object> xdm = (Map<String, Object>) edgeRequestEventList.get(0).getEventData().get("xdm");
        Assert.assertEquals("decisioning.propositionInteract", xdm.get("eventType"));

        List<Map<String, Object>> propositionList = (List<Map<String, Object>>) ((Map<String, Object>) ((Map<String, Object>) xdm.get("_experience")).get("decisioning"))
                .get("propositions");
        Assert.assertNotNull(propositionList);
        Assert.assertEquals(1, propositionList.size());
        Map<String, Object> propositionData = propositionList.get(0);
        List<Map<String, Object>> propositionsList = (List<Map<String, Object>>) propositionData.get("propositions");
        Assert.assertNotNull(propositionList);
        Assert.assertEquals(1, propositionList.size());
        Map<String, Object> propositionMap = propositionList.get(0);
        Assert.assertEquals("AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9", propositionMap.get("id"));
        Assert.assertEquals("myMbox", propositionMap.get("scope"));
        Assert.assertEquals(testDecisionScopesMap, propositionMap.get("scopeDetails"));
        List<Map<String, Object>> itemsList = (List<Map<String, Object>>) propositionMap.get("items");
        Assert.assertNotNull(itemsList);
        Assert.assertEquals(1, itemsList.size());
        Assert.assertEquals("246315", itemsList.get(0).get("id"));
    }

    //13
    @Test
    public void testTrackPropositions_validPropositionInteractionsWithDatasetConfig() throws InterruptedException, IOException {
        //setup
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        configData.put("optimize.datasetId", "111111111111111111111111");
        updateConfiguration(configData);
        final String testDecisionScopes = "        {\n" +
                "        \"decisionProvider\": \"TGT\",\n" +
                "                \"activity\": {\n" +
                "        \"id\": \"125589\"\n" +
                "            },\n" +
                "        \"experience\": {\n" +
                "        \"id\": \"0\"\n" +
                "            },\n" +
                "        \"strategies\": [\n" +
                "                {\n" +
                "        \"algorithmID\": \"0\",\n" +
                "                \"trafficType\": \"0\"\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n";
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> testDecisionScopesMap = objectMapper.readValue(testDecisionScopes, new TypeReference<Map<String, Object>>() {});

        Offer offer = new Offer.Builder("246315", OfferType.TEXT, "Text Offer!!").build();
        Proposition proposition = new Proposition(
                "AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9",
                Collections.singletonList(offer),
                "myMbox",
                testDecisionScopesMap
        );

        //Action
        TestHelper.resetTestExpectations();
        offer.tapped();

        //Assert
        List<Event> optimizeRequestEventsList = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.OPTIMIZE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        List<Event> edgeRequestEventList = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);

        Assert.assertNotNull(optimizeRequestEventsList);
        Assert.assertEquals(1, optimizeRequestEventsList.size());
        Assert.assertNotNull(edgeRequestEventList);
        Assert.assertEquals(1, edgeRequestEventList.size());

        Map<String, Object> xdm = (Map<String, Object>) edgeRequestEventList.get(0).getEventData().get("xdm");
        Assert.assertEquals("decisioning.propositionInteract", xdm.get("eventType"));

        List<Map<String, Object>> propositionList = (List<Map<String, Object>>) ((Map<String, Object>) ((Map<String, Object>) xdm.get("_experience")).get("decisioning"))
                .get("propositions");
        Assert.assertNotNull(propositionList);
        Assert.assertEquals(1, propositionList.size());
        Assert.assertNotNull(propositionList);
        Assert.assertEquals(1, propositionList.size());
        Map<String, Object> propositionMap = propositionList.get(0);
        Assert.assertEquals("AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9", propositionMap.get("id"));
        Assert.assertEquals("myMbox", propositionMap.get("scope"));
        Assert.assertEquals(testDecisionScopesMap, propositionMap.get("scopeDetails"));
        List<Map<String, Object>> itemsList = (List<Map<String, Object>>) propositionMap.get("items");
        Assert.assertNotNull(itemsList);
        Assert.assertEquals(1, itemsList.size());
        Assert.assertEquals("246315", itemsList.get(0).get("id"));

        Assert.assertEquals("111111111111111111111111", edgeRequestEventList.get(0).getEventData().get("datasetId"));
    }

    //14
    @Test
    public void testClearCachedPropositions() throws InterruptedException, IOException {
        //setup
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);
        final String decisionScopeString = "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==";

        //Call updatePropositions to update the personalizationRequestEventId
        Optimize.updatePropositions(Collections.singletonList(new DecisionScope(decisionScopeString)), null, null);
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        Thread.sleep(1000);
        TestHelper.resetTestExpectations();

        //Send Edge Response event so that propositions will get cached by the Optimize SDK
        final String edgeResponseData = "{\n" +
                "                                  \"payload\": [\n" +
                "                                    {\n" +
                "                                        \"id\": \"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\n" +
                "                                        \"scope\": \"" + decisionScopeString + "\",\n" +
                "                                        \"activity\": {\n" +
                "                                            \"etag\": \"8\",\n" +
                "                                            \"id\": \"xcore:offer-activity:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"placement\": {\n" +
                "                                            \"etag\": \"1\",\n" +
                "                                            \"id\": \"xcore:offer-placement:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                \"etag\": \"10\",\n" +
                "                                                \"schema\": \"https://ns.adobe.com/experience/offer-management/content-component-html\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                    \"format\": \"text/html\",\n" +
                "                                                    \"content\": \"<h1>This is HTML content</h1>\",\n" +
                "                                                    \"characteristics\": {\n" +
                "                                                        \"testing\": \"true\"\n" +
                "                                                    }\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    }\n" +
                "                                  ],\n" +
                "                                \"requestEventId\": \"" + eventsListOptimize.get(0).getUniqueIdentifier() + "\",\n" +
                "                                \"requestId\": \"BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB\",\n" +
                "                                \"type\": \"personalization:decisions\"\n" +
                "                              }";

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = objectMapper.readValue(edgeResponseData, new TypeReference<Map<String, Object>>() {});

        Event event = new Event.Builder(
                "AEP Response Event Handle",
                OptimizeTestConstants.EventType.EDGE,
                OptimizeTestConstants.EventSource.PERSONALIZATION).
                setEventData(eventData).
                build();

        //Action
        MobileCore.dispatchEvent(event);

        Thread.sleep(1000);

        TestHelper.resetTestExpectations();
        DecisionScope decisionScope = new DecisionScope(decisionScopeString);
        final Map<DecisionScope, Proposition> propositionMap = new HashMap<>();
        final ADBCountDownLatch countDownLatch = new ADBCountDownLatch(1);
        Optimize.getPropositions(Collections.singletonList(decisionScope), new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                Assert.fail("Error in getting cached propositions");
            }

            @Override
            public void call(Map<DecisionScope, Proposition> decisionScopePropositionMap) {
                propositionMap.putAll(decisionScopePropositionMap);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await(1, TimeUnit.SECONDS);
        //Assertions
        Assert.assertEquals(1, propositionMap.size());

        //Action clear the cache
        Optimize.clearCachedPropositions();

        Thread.sleep(1000);

        final ADBCountDownLatch countDownLatch1 = new ADBCountDownLatch(1);
        propositionMap.clear();
        Optimize.getPropositions(Collections.singletonList(decisionScope), new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                Assert.fail("Error in getting cached propositions");
            }

            @Override
            public void call(Map<DecisionScope, Proposition> decisionScopePropositionMap) {
                propositionMap.putAll(decisionScopePropositionMap);
                countDownLatch1.countDown();
            }
        });
        countDownLatch.await(1, TimeUnit.SECONDS);

        Assert.assertTrue(propositionMap.isEmpty());
    }

    //15
    @Test
    public void testCoreResetIdentities() throws InterruptedException, IOException {
        //setup
        //Send Edge Response event so that propositions will get cached by the Optimize SDK
        final Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);
        final String decisionScopeString = "eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==";

        //Call updatePropositions to update the personalizationRequestEventId
        Optimize.updatePropositions(Collections.singletonList(new DecisionScope(decisionScopeString)), null, null);
        List<Event> eventsListOptimize = TestHelper.getDispatchedEventsWith(OptimizeTestConstants.EventType.EDGE, OptimizeTestConstants.EventSource.REQUEST_CONTENT, 1000);
        Thread.sleep(1000);
        TestHelper.resetTestExpectations();

        //Send Edge Response event so that propositions will get cached by the Optimize SDK
        final String edgeResponseData = "{\n" +
                "                                  \"payload\": [\n" +
                "                                    {\n" +
                "                                        \"id\": \"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\n" +
                "                                        \"scope\": \"" + decisionScopeString + "\",\n" +
                "                                        \"activity\": {\n" +
                "                                            \"etag\": \"8\",\n" +
                "                                            \"id\": \"xcore:offer-activity:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"placement\": {\n" +
                "                                            \"etag\": \"1\",\n" +
                "                                            \"id\": \"xcore:offer-placement:1111111111111111\"\n" +
                "                                        },\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                \"etag\": \"10\",\n" +
                "                                                \"schema\": \"https://ns.adobe.com/experience/offer-management/content-component-html\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                    \"format\": \"text/html\",\n" +
                "                                                    \"content\": \"<h1>This is HTML content</h1>\",\n" +
                "                                                    \"characteristics\": {\n" +
                "                                                        \"testing\": \"true\"\n" +
                "                                                    }\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    }\n" +
                "                                  ],\n" +
                "                                \"requestEventId\": \"" + eventsListOptimize.get(0).getUniqueIdentifier() + "\",\n" +
                "                                \"requestId\": \"BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB\",\n" +
                "                                \"type\": \"personalization:decisions\"\n" +
                "                              }";

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = objectMapper.readValue(edgeResponseData, new TypeReference<Map<String, Object>>() {});

        Event event = new Event.Builder(
                "AEP Response Event Handle",
                OptimizeTestConstants.EventType.EDGE,
                OptimizeTestConstants.EventSource.PERSONALIZATION).
                setEventData(eventData).
                build();

        //Action
        MobileCore.dispatchEvent(event);

        Thread.sleep(1000);

        TestHelper.resetTestExpectations();
        DecisionScope decisionScope = new DecisionScope(decisionScopeString);
        final Map<DecisionScope, Proposition> propositionMap = new HashMap<>();
        final ADBCountDownLatch countDownLatch = new ADBCountDownLatch(1);
        Optimize.getPropositions(Collections.singletonList(decisionScope), new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                Assert.fail("Error in getting cached propositions");
            }

            @Override
            public void call(Map<DecisionScope, Proposition> decisionScopePropositionMap) {
                propositionMap.putAll(decisionScopePropositionMap);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await(1, TimeUnit.SECONDS);
        //Assertions
        Assert.assertEquals(1, propositionMap.size());

        //Action: Trigger Identity Request reset event.
        MobileCore.resetIdentities();

        Thread.sleep(1000);

        //Assert
        TestHelper.resetTestExpectations();
        propositionMap.clear();
        final ADBCountDownLatch countDownLatch1 = new ADBCountDownLatch(1);
        Optimize.getPropositions(Collections.singletonList(decisionScope), new AdobeCallbackWithError<Map<DecisionScope, Proposition>>() {
            @Override
            public void fail(AdobeError adobeError) {
                Assert.fail("Error in getting cached propositions");
            }

            @Override
            public void call(Map<DecisionScope, Proposition> decisionScopePropositionMap) {
                propositionMap.putAll(decisionScopePropositionMap);
                countDownLatch1.countDown();
            }
        });

        countDownLatch1.await(1, TimeUnit.SECONDS);
        //Assertions
        Assert.assertTrue(propositionMap.isEmpty());
    }

    //16
    @Test
    public void testOfferGenerateDisplayInteractionXdm() throws IOException {
        //Setup
        final String validPropositionText = "{\n" +
                "  \"id\":\"de03ac85-802a-4331-a905-a57053164d35\",\n" +
                "  \"items\":[\n" +
                "    {\n" +
                "      \"id\":\"xcore:personalized-offer:1111111111111111\",\n" +
                "      \"etag\":\"10\",\n" +
                "      \"schema\":\"https://ns.adobe.com/experience/offer-management/content-component-html\",\n" +
                "      \"data\":{\n" +
                "        \"id\":\"xcore:personalized-offer:1111111111111111\",\n" +
                "        \"format\":\"text/html\",\n" +
                "        \"content\":\"<h1>This is a HTML content</h1>\"\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"placement\":{\n" +
                "    \"etag\":\"1\",\n" +
                "    \"id\":\"xcore:offer-placement:1111111111111111\"\n" +
                "  },\n" +
                "  \"activity\":{\n" +
                "    \"etag\":\"8\",\n" +
                "    \"id\":\"xcore:offer-activity:1111111111111111\"\n" +
                "  },\n" +
                "  \"scope\":\"eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==\"\n" +
                "}";

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> propositionData = objectMapper.readValue(validPropositionText, new TypeReference<Map<String, Object>>() {});
        Proposition proposition = Proposition.fromEventData(propositionData);
        assert proposition != null;
        Offer offer = proposition.getOffers().get(0);

        //Action
        final Map<String, Object> propositionInteractionXdm = offer.generateDisplayInteractionXdm();

        //Assert
        Assert.assertNotNull(propositionInteractionXdm);
        Assert.assertEquals("decisioning.propositionDisplay", propositionInteractionXdm.get("eventType"));
        final Map<String, Object> experience = (Map<String, Object>)propositionInteractionXdm.get("_experience");
        Assert.assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>)experience.get("decisioning");
        Assert.assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList = (List<Map<String, Object>>)decisioning.get("propositions");
        Assert.assertNotNull(propositionInteractionDetailsList);
        Assert.assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap = propositionInteractionDetailsList.get(0);
        Assert.assertEquals("de03ac85-802a-4331-a905-a57053164d35", propositionInteractionDetailsMap.get("id"));
        Assert.assertEquals("eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails = (Map<String, Object>)propositionInteractionDetailsMap.get("scopeDetails");
        Assert.assertNotNull(scopeDetails);
        Assert.assertTrue(scopeDetails.isEmpty());
        final List<Map<String, Object>> items = (List<Map<String, Object>>)propositionInteractionDetailsMap.get("items");
        Assert.assertNotNull(items);
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("xcore:personalized-offer:1111111111111111", items.get(0).get("id"));
    }

    //17
    @Test
    public void testOfferGenerateTapInteractionXdm() throws IOException {
        //Setup
        final String validProposition = "{\n" +
                "  \"id\": \"AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9\",\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"id\": \"246315\",\n" +
                "      \"schema\": \"https://ns.adobe.com/personalization/json-content-item\",\n" +
                "      \"data\": {\n" +
                "        \"id\": \"246315\",\n" +
                "        \"format\": \"application/json\",\n" +
                "        \"content\": {\n" +
                "          \"testing\": \"ho-ho\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"scope\": \"myMbox\",\n" +
                "  \"scopeDetails\": {\n" +
                "    \"decisionProvider\": \"TGT\",\n" +
                "    \"activity\": {\n" +
                "      \"id\": \"125589\"\n" +
                "    },\n" +
                "    \"experience\": {\n" +
                "      \"id\": \"0\"\n" +
                "    },\n" +
                "    \"strategies\": [\n" +
                "      {\n" +
                "        \"algorithmID\": \"0\",\n" +
                "        \"trafficType\": \"0\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> propositionData = objectMapper.readValue(validProposition, new TypeReference<Map<String, Object>>() {});
        Proposition proposition = Proposition.fromEventData(propositionData);
        assert proposition != null;
        Offer offer = proposition.getOffers().get(0);

        //Action
        Map<String, Object> propositionTapInteractionXdm = offer.generateTapInteractionXdm();

        //Assert
        // verify
        Assert.assertNotNull(propositionTapInteractionXdm);
        Assert.assertEquals("decisioning.propositionInteract", propositionTapInteractionXdm.get("eventType"));
        final Map<String, Object> experience = (Map<String, Object>)propositionTapInteractionXdm.get("_experience");
        Assert.assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>)experience.get("decisioning");
        Assert.assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList = (List<Map<String, Object>>)decisioning.get("propositions");
        Assert.assertNotNull(propositionInteractionDetailsList);
        Assert.assertEquals(1, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap = propositionInteractionDetailsList.get(0);
        Assert.assertEquals("AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9", propositionInteractionDetailsMap.get("id"));
        Assert.assertEquals("myMbox", propositionInteractionDetailsMap.get("scope"));
        final Map<String, Object> scopeDetails = (Map<String, Object>) propositionInteractionDetailsMap.get("scopeDetails");
        Assert.assertNotNull(scopeDetails);
        Assert.assertTrue(scopeDetails.size() > 0);
        final List<Map<String, Object>> items = (List<Map<String, Object>>)propositionInteractionDetailsMap.get("items");
        Assert.assertNotNull(items);
        Assert.assertEquals(1, items.size());
        Assert.assertEquals("246315", items.get(0).get("id"));
    }

    //18
    @Test
    public void testPropositionGenerateReferenceXdm() throws IOException {
        //Setup
        final String validProposition = "{\n" +
                "  \"id\":\"de03ac85-802a-4331-a905-a57053164d35\",\n" +
                "  \"items\":[\n" +
                "    {\n" +
                "      \"id\":\"xcore:personalized-offer:1111111111111111\",\n" +
                "      \"etag\":\"10\",\n" +
                "      \"schema\":\"https://ns.adobe.com/experience/offer-management/content-component-html\",\n" +
                "      \"data\":{\n" +
                "        \"id\":\"xcore:personalized-offer:1111111111111111\",\n" +
                "        \"format\":\"text/html\",\n" +
                "        \"content\":\"<h1>This is a HTML content</h1>\"\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"placement\":{\n" +
                "    \"etag\":\"1\",\n" +
                "    \"id\":\"xcore:offer-placement:1111111111111111\"\n" +
                "  },\n" +
                "  \"activity\":{\n" +
                "    \"etag\":\"8\",\n" +
                "    \"id\":\"xcore:offer-activity:1111111111111111\"\n" +
                "  },\n" +
                "  \"scope\":\"eydhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==\"\n" +
                "}";

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> propositionData = objectMapper.readValue(validProposition, new TypeReference<Map<String, Object>>() {});

        final Proposition proposition = Proposition.fromEventData(propositionData);

        // Action
        assert proposition != null;
        final Map<String, Object> propositionReferenceXdm = proposition.generateReferenceXdm();

        // verify
        Assert.assertNotNull(propositionReferenceXdm);
        final Map<String, Object> experience = (Map<String, Object>)propositionReferenceXdm.get("_experience");
        Assert.assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>)experience.get("decisioning");
        Assert.assertNotNull(decisioning);
        Assert.assertEquals("de03ac85-802a-4331-a905-a57053164d35", decisioning.get("propositionID"));
    }


    @Test
    public void testOnPropositionsUpdate_validPropositionForSurface() throws InterruptedException, IOException {
        //Setup
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        //Action
        Optimize.onPropositionsUpdate(propositionMap -> {

            // verify
            Assert.assertEquals(1, propositionMap.size());
            DecisionScope decisionScope = new DecisionScope("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==");
            Assert.assertNotNull(propositionMap.get(decisionScope));

            Proposition proposition = propositionMap.get(decisionScope);
            Assert.assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", proposition.getId());
            Assert.assertEquals("eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==", proposition.getScope());
            Assert.assertEquals(1, proposition.getOffers().size());
            Assert.assertEquals("xcore:personalized-offer:1111111111111111", proposition.getOffers().get(0).getId());
            Assert.assertEquals("https://ns.adobe.com/experience/offer-management/content-component-text", proposition.getOffers().get(0).getSchema());
            Assert.assertEquals(OfferType.TEXT,  proposition.getOffers().get(0).getType()); // Offer format is not returned in Edge response
            Assert.assertEquals("This is a plain text content!",  proposition.getOffers().get(0).getContent());
        });

        // Send Optimize Notification event so that setPropositionsHandler will get called
        final String optimizeNotificationData = "{\n" +
                "                                   \"propositions\": [\n" +
                "                                    {\n" +
                "                                        \"id\": \"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\n" +
                "                                        \"scope\": \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTExMTExMTExMTExMTExMSIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjExMTExMTExMTExMTExMTEifQ==\",\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                \"schema\": \"https://ns.adobe.com/experience/offer-management/content-component-text\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"id\": \"xcore:personalized-offer:1111111111111111\",\n" +
                "                                                    \"type\": 2,\n" +
                "                                                    \"content\": \"This is a plain text content!\"\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    }\n" +
                "                                   ]\n" +
                "                               }";


        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = objectMapper.readValue(optimizeNotificationData, new TypeReference<Map<String, Object>>() {});

        Event event = new Event.Builder(
                "Personalization Notification",
                OptimizeTestConstants.EventType.OPTIMIZE,
                OptimizeTestConstants.EventSource.NOTIFICATION).
                setEventData(eventData).
                build();

        MobileCore.dispatchEvent(event);
    }

    @Test
    public void testOnPropositionsUpdate_emptyPropositionArray() throws InterruptedException, IOException {
        //Setup
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        // onPropositionsUpdate should not be called for empty propositions in personalization notification response
        //Action
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Optimize.onPropositionsUpdate(propositionMap -> {
            countDownLatch.countDown();
        });

        //Send Optimize Notification event so that setPropositionsHandler will get called
        final String optimizeNotificationData = "{\n" +
                "                                   \"propositions\": []\n" +
                "                               }";


        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = objectMapper.readValue(optimizeNotificationData, new TypeReference<Map<String, Object>>() {});

        Event event = new Event.Builder(
                "Personalization Notification",
                OptimizeTestConstants.EventType.OPTIMIZE,
                OptimizeTestConstants.EventSource.NOTIFICATION).
                setEventData(eventData).
                build();

        MobileCore.dispatchEvent(event);

        // verify
        Assert.assertFalse(countDownLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testOnPropositionsUpdate_nullPropositionArray() throws InterruptedException, IOException {
        //Setup
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        // onPropositionsUpdate should not be called for null propositions in personalization notification response
        //Action
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Optimize.onPropositionsUpdate(propositionMap -> {
            countDownLatch.countDown();
        });

        // Send Optimize Notification event so that setPropositionsHandler will get called
        Event event = new Event.Builder(
                "Personalization Notification",
                OptimizeTestConstants.EventType.OPTIMIZE,
                OptimizeTestConstants.EventSource.NOTIFICATION).
                setEventData(null).
                build();

        MobileCore.dispatchEvent(event);

        // verify
        Assert.assertFalse(countDownLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testSetPropositionsHandler_validPropositionForSurface() throws InterruptedException, IOException {
        //Setup
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        //Action
        Optimize.setIsPropositionsListenerRegistered(false);
        Optimize.setPropositionsHandler(propositionMap -> {

            // verify
            Assert.assertEquals(1, propositionMap.size());
            Assert.assertNotNull(propositionMap.get("mobileapp://com.adobe.marketing.mobile.optimize.test/myView#htmlElement"));

            Proposition proposition = propositionMap.get("myView#htmlElement");
            Assert.assertEquals("2cceff23-5eea-4bad-af5f-abb1aca1ea2e", proposition.getId());
            Assert.assertEquals("mobileapp://com.adobe.marketing.mobile.optimize.test/myView#htmlElement", proposition.getScope());
            Assert.assertEquals(1, proposition.getOffers().size());
            Assert.assertEquals("fd125be6-f505-4640-ba26-9527c682e1a8", proposition.getOffers().get(0).getId());
            Assert.assertEquals("https://ns.adobe.com/personalization/html-content-item", proposition.getOffers().get(0).getSchema());
            Assert.assertEquals(OfferType.UNKNOWN,  proposition.getOffers().get(0).getType()); // Offer format is not returned in Edge response
            Assert.assertEquals("<h3>This is a HTML content!</h3>",  proposition.getOffers().get(0).getContent());
        });

        // Send Optimize Notification event so that setPropositionsHandler will get called
        final String optimizeNotificationData = "{\n" +
                "                                   \"propositions\": [\n" +
                "                                    {\n" +
                "                                        \"id\": \"2cceff23-5eea-4bad-af5f-abb1aca1ea2e\",\n" +
                "                                        \"scope\": \"mobileapp://com.adobe.marketing.mobile.optimize.test/myView#htmlElement\",\n" +
                "                                        \"items\": [\n" +
                "                                            {\n" +
                "                                                \"id\": \"fd125be6-f505-4640-ba26-9527c682e1a8\",\n" +
                "                                                \"schema\": \"https://ns.adobe.com/personalization/html-content-item\",\n" +
                "                                                \"data\": {\n" +
                "                                                    \"type\": 0,\n" +
                "                                                    \"content\": \"<h3>This is a HTML content!</h3>\"\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        ]\n" +
                "                                    }\n" +
                "                                   ]\n" +
                "                               }";


        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = objectMapper.readValue(optimizeNotificationData, new TypeReference<Map<String, Object>>() {});

        Event event = new Event.Builder(
                "Personalization Notification",
                OptimizeTestConstants.EventType.OPTIMIZE,
                OptimizeTestConstants.EventSource.NOTIFICATION).
                setEventData(eventData).
                build();

        MobileCore.dispatchEvent(event);
    }

    @Test
    public void testSetPropositionsHandler_emptyPropositionArray() throws InterruptedException, IOException {
        //Setup
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        // onPropositionsUpdate should not be called for empty propositions in personalization notification response
        //Action
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Optimize.setIsPropositionsListenerRegistered(false);
        Optimize.setPropositionsHandler(propositionMap -> {
            countDownLatch.countDown();
        });

        //Send Optimize Notification event so that setPropositionsHandler will get called
        final String optimizeNotificationData = "{\n" +
                "                                   \"propositions\": []\n" +
                "                               }";


        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> eventData = objectMapper.readValue(optimizeNotificationData, new TypeReference<Map<String, Object>>() {});

        Event event = new Event.Builder(
                "Personalization Notification",
                OptimizeTestConstants.EventType.OPTIMIZE,
                OptimizeTestConstants.EventSource.NOTIFICATION).
                setEventData(eventData).
                build();

        MobileCore.dispatchEvent(event);

        // verify
        Assert.assertFalse(countDownLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testSetPropositionsHandler_nullPropositionArray() throws InterruptedException, IOException {
        //Setup
        Map<String, Object> configData = new HashMap<>();
        configData.put("edge.configId", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        updateConfiguration(configData);

        // onPropositionsUpdate should not be called for null propositions in personalization notification response
        //Action
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Optimize.setIsPropositionsListenerRegistered(false);
        Optimize.setPropositionsHandler(propositionMap -> {
            countDownLatch.countDown();
        });

        // Send Optimize Notification event so that setPropositionsHandler will get called
        Event event = new Event.Builder(
                "Personalization Notification",
                OptimizeTestConstants.EventType.OPTIMIZE,
                OptimizeTestConstants.EventSource.NOTIFICATION).
                setEventData(null).
                build();

        MobileCore.dispatchEvent(event);

        // verify
        Assert.assertFalse(countDownLatch.await(1, TimeUnit.SECONDS));
    }

    private void updateConfiguration(final Map<String, Object> config) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        MonitorExtension.configurationAwareness(configurationState -> latch.countDown());
        MobileCore.updateConfiguration(config);
        Assert.assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    private void clearUpdatedConfiguration() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        MonitorExtension.configurationAwareness(configurationState -> latch.countDown());
        MobileCore.clearUpdatedConfiguration();
        Assert.assertTrue(latch.await(2, TimeUnit.SECONDS));
    }
}