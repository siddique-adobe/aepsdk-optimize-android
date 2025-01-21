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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Extension;
import java.util.List;
import java.util.Map;

/** Public class containing APIs for the Optimize extension. */
public class Optimize {
    public static final Class<? extends Extension> EXTENSION = OptimizeExtension.class;

    private Optimize() {}

    /**
     * Returns the version of the {@code Optimize} extension.
     *
     * @return {@link String} containing the current installed version of this extension.
     */
    @NonNull public static String extensionVersion() {
        return OptimizeConstants.EXTENSION_VERSION;
    }

    /**
     * This API dispatches an Event for the Edge network extension to fetch decision propositions,
     * for the provided decision scopes list, from the decisioning services enabled in the
     * Experience Edge network.
     *
     * <p>The returned decision propositions are cached in-memory in the Optimize SDK extension and
     * can be retrieved using {@link #getPropositions(List, AdobeCallback)} API.
     *
     * @param decisionScopes {@code List<DecisionScope>} containing scopes for which offers need to
     *     be updated.
     * @param xdm {@code Map<String, Object>} containing additional XDM-formatted data to be sent in
     *     the personalization query request.
     * @param data {@code Map<String, Object>} containing additional free-form data to be sent in
     *     the personalization query request.
     */
    @Deprecated
    public static void updatePropositions(
            @NonNull final List<DecisionScope> decisionScopes,
            @Nullable final Map<String, Object> xdm,
            @Nullable final Map<String, Object> data) {

        updatePropositions(decisionScopes, xdm, data, null);
    }

    /**
     * This API dispatches an Event for the Edge network extension to fetch decision propositions,
     * for the provided decision scopes list, from the decisioning services enabled in the
     * Experience Edge network.
     *
     * <p>The returned decision propositions are cached in-memory in the Optimize SDK extension and
     * can be retrieved using {@link #getPropositions(List, AdobeCallback)} API.
     *
     * @param decisionScopes {@code List<DecisionScope>} containing scopes for which offers need to
     *     be updated.
     * @param xdm {@code Map<String, Object>} containing additional XDM-formatted data to be sent in
     *     the personalization query request.
     * @param data {@code Map<String, Object>} containing additional free-form data to be sent in
     *     the personalization query request.
     * @param callback {@code AdobeCallback<Map<DecisionScope, OptimizeProposition>>} which will be
     *     invoked when decision propositions are received from the Edge network.
     */
    public static void updatePropositions(
            @NonNull final List<DecisionScope> decisionScopes,
            @Nullable final Map<String, Object> xdm,
            @Nullable final Map<String, Object> data,
            @Nullable final AdobeCallback<Map<DecisionScope, OptimizeProposition>> callback) {
        OptimizeImpl.INSTANCE.updatePropositionsInternal(decisionScopes, xdm, data, callback);
    }

    /**
     * This API dispatches an Event for the Edge network extension to fetch decision propositions,
     * for the provided decision scopes list, from the decisioning services enabled in the
     * Experience Edge network.
     *
     * <p>The returned decision propositions are cached in-memory in the Optimize SDK extension and
     * can be retrieved using {@link #getPropositions(List, double, AdobeCallback)} API.
     *
     * @param decisionScopes {@code List<DecisionScope>} containing scopes for which offers need to
     *     be updated.
     * @param xdm {@code Map<String, Object>} containing additional XDM-formatted data to be sent in
     *     the personalization query request.
     * @param data {@code Map<String, Object>} containing additional free-form data to be sent in
     *     the personalization query request.
     * @param timeoutSeconds {@code Double} containing additional configurable timeout(seconds) to
     *     be sent in the personalization query request.
     * @param callback {@code AdobeCallback<Map<DecisionScope, OptimizeProposition>>} which will be
     *     invoked when decision propositions are received from the Edge network.
     */
    public static void updatePropositions(
            @NonNull final List<DecisionScope> decisionScopes,
            @Nullable final Map<String, Object> xdm,
            @Nullable final Map<String, Object> data,
            final double timeoutSeconds,
            @Nullable final AdobeCallback<Map<DecisionScope, OptimizeProposition>> callback) {
        OptimizeImpl.INSTANCE.updatePropositionsInternal(
                decisionScopes, xdm, data, timeoutSeconds, callback);
    }

    /**
     * This API retrieves the previously fetched propositions, for the provided decision scopes,
     * from the in-memory extension propositions cache.
     *
     * @param decisionScopes {@code List<DecisionScope>} containing scopes for which offers need to
     *     be requested.
     * @param callback {@code AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>>} which
     *     will be invoked when decision propositions are retrieved from the local cache.
     */
    public static void getPropositions(
            @NonNull final List<DecisionScope> decisionScopes,
            @NonNull final AdobeCallback<Map<DecisionScope, OptimizeProposition>> callback) {
        OptimizeImpl.INSTANCE.getPropositionsInternal(decisionScopes, callback);
    }

    /**
     * This API retrieves the previously fetched propositions, for the provided decision scopes,
     * from the in-memory extension propositions cache.
     *
     * @param decisionScopes {@code List<DecisionScope>} containing scopes for which offers need to
     *     be requested.
     * @param callback {@code AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>>} which
     *     will be invoked when decision propositions are retrieved from the local cache.
     */
    public static void getPropositions(
            @NonNull final List<DecisionScope> decisionScopes,
            final double timeoutSeconds,
            @NonNull final AdobeCallback<Map<DecisionScope, OptimizeProposition>> callback) {
        OptimizeImpl.INSTANCE.getPropositionsInternal(decisionScopes, timeoutSeconds, callback);
    }

    /**
     * This API registers a permanent callback which is invoked whenever the Edge extension
     * dispatches a response Event received from the Experience Edge Network upon a personalization
     * query.
     *
     * <p>The personalization query requests can be triggered by the {@link
     * Optimize#updatePropositions(List, Map, Map)} API, Edge extension {@code
     * sendEvent(ExperienceEvent, EdgeCallback)} API or launch consequence rules.
     *
     * @param callback {@code AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>>} which
     *     will be invoked when decision propositions are received from the Edge network.
     */
    public static void onPropositionsUpdate(
            @NonNull final AdobeCallback<Map<DecisionScope, OptimizeProposition>> callback) {
        OptimizeImpl.INSTANCE.onPropositionsUpdate(callback);
    }

    /** Clears the client-side in-memory propositions cache. */
    public static void clearCachedPropositions() {
        OptimizeImpl.INSTANCE.clearCachedPropositions();
    }
}
