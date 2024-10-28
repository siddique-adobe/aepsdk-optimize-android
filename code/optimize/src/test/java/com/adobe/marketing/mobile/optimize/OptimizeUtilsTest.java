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

import static com.adobe.marketing.mobile.optimize.OptimizeUtils.generateInteractionXdm;

import android.util.Base64;
import com.adobe.marketing.mobile.AdobeError;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.Silent.class)
@SuppressWarnings({"rawtypes"})
public class OptimizeUtilsTest {

    @Test
    public void testIsNullOrEmpty_nullMap() {
        // test
        Assert.assertTrue(OptimizeUtils.isNullOrEmpty((Map<String, Object>) null));
    }

    @Test
    public void testIsNullOrEmpty_emptyMap() {
        // test
        Assert.assertTrue(OptimizeUtils.isNullOrEmpty(new HashMap<>()));
    }

    @Test
    public void testIsNullOrEmpty_nonEmptyMap() {
        // test
        final Map<String, Object> map = new HashMap<>();
        map.put("key", "value");

        Assert.assertFalse(OptimizeUtils.isNullOrEmpty(map));
    }

    @Test
    public void testIsNullOrEmpty_nullList() {
        // test
        Assert.assertTrue(OptimizeUtils.isNullOrEmpty((List<Object>) null));
    }

    @Test
    public void testIsNullOrEmpty_emptyList() {
        // test
        Assert.assertTrue(OptimizeUtils.isNullOrEmpty(new ArrayList<>()));
    }

    @Test
    public void testIsNullOrEmpty_nonEmptyList() {
        // test
        final List<Object> list = new ArrayList<>();
        list.add("someString");

        Assert.assertFalse(OptimizeUtils.isNullOrEmpty(list));
    }

    @Test
    public void testIsNullOrEmpty_nullString() {
        // test
        final String input = null;
        Assert.assertTrue(OptimizeUtils.isNullOrEmpty(input));
    }

    @Test
    public void testIsNullOrEmpty_emptyString() {
        // test
        final String input = "";
        Assert.assertTrue(OptimizeUtils.isNullOrEmpty(input));
    }

    @Test
    public void testIsNullOrEmpty_nonEmptyString() {
        // test
        final String input = "This is a test string!";
        Assert.assertFalse(OptimizeUtils.isNullOrEmpty(input));
    }

    @Test
    public void testBase64encode_validString() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic
                    .when(
                            () ->
                                    Base64.encodeToString(
                                            ArgumentMatchers.any(), ArgumentMatchers.anyInt()))
                    .thenAnswer(
                            (Answer)
                                    invocation ->
                                            java.util.Base64.getEncoder()
                                                    .encodeToString(
                                                            (byte[]) invocation.getArguments()[0]));
            // test
            final String input = "This is a test string!";
            Assert.assertEquals(
                    "VGhpcyBpcyBhIHRlc3Qgc3RyaW5nIQ==", OptimizeUtils.base64Encode(input));
        }
    }

    @Test
    public void testBase64encode_emptyString() {
        // test
        final String input = "";
        Assert.assertEquals("", OptimizeUtils.base64Encode(input));
    }

    @Test
    public void testBase64decode_validString() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic
                    .when(
                            () ->
                                    Base64.decode(
                                            ArgumentMatchers.anyString(),
                                            ArgumentMatchers.anyInt()))
                    .thenAnswer(
                            (Answer<byte[]>)
                                    invocation ->
                                            java.util.Base64.getDecoder()
                                                    .decode((String) invocation.getArguments()[0]));
            // test
            final String input = "VGhpcyBpcyBhIHRlc3Qgc3RyaW5nIQ==";
            Assert.assertEquals("This is a test string!", OptimizeUtils.base64Decode(input));
        }
    }

    @Test
    public void testBase64decode_emptyString() {
        // test
        final String input = "";
        Assert.assertEquals("", OptimizeUtils.base64Decode(input));
    }

    @Test
    public void testBase64decode_invalidString() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic
                    .when(
                            () ->
                                    Base64.decode(
                                            ArgumentMatchers.anyString(),
                                            ArgumentMatchers.anyInt()))
                    .thenAnswer(
                            (Answer<byte[]>)
                                    invocation ->
                                            java.util.Base64.getDecoder()
                                                    .decode((String) invocation.getArguments()[0]));
            // test
            final String input = "VGhp=";
            Assert.assertNull(OptimizeUtils.base64Decode(input));
        }
    }

    @Test
    public void testBase64decode_mboxString() {
        try (MockedStatic<Base64> base64MockedStatic = Mockito.mockStatic(Base64.class)) {
            base64MockedStatic
                    .when(
                            () ->
                                    Base64.decode(
                                            ArgumentMatchers.anyString(),
                                            ArgumentMatchers.anyInt()))
                    .thenAnswer(
                            (Answer<byte[]>)
                                    invocation ->
                                            java.util.Base64.getDecoder()
                                                    .decode((String) invocation.getArguments()[0]));
            // test
            final String input = "myTargetLocationB";
            Assert.assertNull(OptimizeUtils.base64Decode(input));
        }
    }

    @Test
    public void testConvertToAdobeError_knownErrorCode() {
        Assert.assertEquals(AdobeError.UNEXPECTED_ERROR, OptimizeUtils.convertToAdobeError(0));
        Assert.assertEquals(AdobeError.CALLBACK_TIMEOUT, OptimizeUtils.convertToAdobeError(1));
        Assert.assertEquals(AdobeError.CALLBACK_NULL, OptimizeUtils.convertToAdobeError(2));
        Assert.assertEquals(
                AdobeError.EXTENSION_NOT_INITIALIZED, OptimizeUtils.convertToAdobeError(11));
    }

    @Test
    public void testConvertToAdobeError_unknownErrorCode() {
        Assert.assertEquals(AdobeError.UNEXPECTED_ERROR, OptimizeUtils.convertToAdobeError(123));
    }

    @Test
    public void testGenerateDisplayInteractionXdm_validPropositionFromTarget() throws Exception {
        // setup
        List<Map<String, Object>> propositionsList =
                new ObjectMapper()
                        .readValue(
                                Objects.requireNonNull(getClass().getClassLoader())
                                        .getResource("json/MULTIPLE_PROPOSITION_VALID_TARGET.json"),
                                List.class);
        List<OptimizeProposition> optimizePropositionList =
                propositionsList.stream().map(OptimizeProposition::fromEventData).toList();
        Assert.assertNotNull(optimizePropositionList);
        for (OptimizeProposition optimizeProposition : optimizePropositionList) {
            Assert.assertNotNull(optimizeProposition.getOffers());
            Assert.assertEquals(1, optimizeProposition.getOffers().size());
            Offer offer = optimizeProposition.getOffers().get(0);
            Assert.assertNotNull(offer);
        }
        Map<String, Object> xdm =
                generateInteractionXdm(
                        optimizePropositionList,
                        OptimizeConstants.JsonValues.EE_EVENT_TYPE_PROPOSITION_DISPLAY);
        Assert.assertNotNull(xdm);

        Assert.assertEquals("decisioning.propositionDisplay", xdm.get("eventType"));
        final Map<String, Object> experience = (Map<String, Object>) xdm.get("_experience");
        Assert.assertNotNull(experience);
        final Map<String, Object> decisioning = (Map<String, Object>) experience.get("decisioning");
        Assert.assertNotNull(decisioning);
        final List<Map<String, Object>> propositionInteractionDetailsList =
                (List<Map<String, Object>>) decisioning.get("propositions");
        Assert.assertNotNull(propositionInteractionDetailsList);
        Assert.assertEquals(2, propositionInteractionDetailsList.size());
        final Map<String, Object> propositionInteractionDetailsMap1 =
                propositionInteractionDetailsList.get(0);
        Assert.assertEquals(
                "AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ8",
                propositionInteractionDetailsMap1.get("id"));
        final Map<String, Object> propositionInteractionDetailsMap2 =
                propositionInteractionDetailsList.get(1);
        Assert.assertEquals(
                "AT:eyJhY3Rpdml0eUlkIjoiMTI1NTg5IiwiZXhwZXJpZW5jZUlkIjoiMCJ9",
                propositionInteractionDetailsMap2.get("id"));
        Assert.assertEquals("myMbox1", propositionInteractionDetailsMap1.get("scope"));
        Assert.assertEquals("myMbox2", propositionInteractionDetailsMap2.get("scope"));
        final Map<String, Object> scopeDetails1 =
                (Map<String, Object>) propositionInteractionDetailsMap1.get("scopeDetails");
        Assert.assertNotNull(scopeDetails1);
        Assert.assertEquals(4, scopeDetails1.size());
        Assert.assertEquals("TGT", scopeDetails1.get("decisionProvider"));
        final Map<String, Object> sdActivity1 = (Map<String, Object>) scopeDetails1.get("activity");
        Assert.assertEquals("125588", sdActivity1.get("id"));
        final Map<String, Object> sdExperience1 =
                (Map<String, Object>) scopeDetails1.get("experience");
        Assert.assertEquals("0", sdExperience1.get("id"));
        final List<Map<String, Object>> sdStrategies1 =
                (List<Map<String, Object>>) scopeDetails1.get("strategies");
        Assert.assertNotNull(sdStrategies1);
        Assert.assertEquals(1, sdStrategies1.size());
        Assert.assertEquals("0", sdStrategies1.get(0).get("algorithmID"));
        Assert.assertEquals("0", sdStrategies1.get(0).get("trafficType"));
        final List<Map<String, Object>> items1 =
                (List<Map<String, Object>>) propositionInteractionDetailsMap1.get("items");
        Assert.assertNotNull(items1);
        Assert.assertEquals(1, items1.size());
        Assert.assertEquals("246314", items1.get(0).get("id"));
        final Map<String, Object> scopeDetails2 =
                (Map<String, Object>) propositionInteractionDetailsMap2.get("scopeDetails");
        Assert.assertNotNull(scopeDetails2);
        Assert.assertEquals(4, scopeDetails2.size());
        Assert.assertEquals("TGT", scopeDetails2.get("decisionProvider"));
        final Map<String, Object> sdActivity2 = (Map<String, Object>) scopeDetails2.get("activity");
        Assert.assertEquals("125589", sdActivity2.get("id"));
        final Map<String, Object> sdExperience2 =
                (Map<String, Object>) scopeDetails2.get("experience");
        Assert.assertEquals("1", sdExperience2.get("id"));
        final List<Map<String, Object>> sdStrategies2 =
                (List<Map<String, Object>>) scopeDetails2.get("strategies");
        Assert.assertNotNull(sdStrategies2);
        Assert.assertEquals(1, sdStrategies2.size());
        Assert.assertEquals("1", sdStrategies2.get(0).get("algorithmID"));
        Assert.assertEquals("1", sdStrategies2.get(0).get("trafficType"));
        final List<Map<String, Object>> items2 =
                (List<Map<String, Object>>) propositionInteractionDetailsMap2.get("items");
        Assert.assertNotNull(items2);
        Assert.assertEquals(1, items2.size());
        Assert.assertEquals("246315", items2.get(0).get("id"));
    }
}
