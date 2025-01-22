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
package com.adobe.marketing.optimizeapp.viewmodels

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.AdobeError
import com.adobe.marketing.mobile.edge.identity.AuthenticatedState
import com.adobe.marketing.mobile.edge.identity.Identity
import com.adobe.marketing.mobile.edge.identity.IdentityItem
import com.adobe.marketing.mobile.edge.identity.IdentityMap
import com.adobe.marketing.mobile.optimize.AEPOptimizeError
import com.adobe.marketing.mobile.optimize.AdobeCallbackWithOptimizeError
import com.adobe.marketing.mobile.optimize.DecisionScope
import com.adobe.marketing.mobile.optimize.Optimize
import com.adobe.marketing.mobile.optimize.OptimizeProposition
import com.adobe.marketing.optimizeapp.impl.LogManager
import com.adobe.marketing.optimizeapp.models.OptimizePair

class MainViewModel : ViewModel() {

    //Settings textField Values
    var textAssuranceUrl by mutableStateOf("")
    var textOdeText by mutableStateOf("")
    var textOdeImage by mutableStateOf("")
    var textOdeHtml by mutableStateOf("")
    var textOdeJson by mutableStateOf("")

    var textTargetMbox by mutableStateOf("")
    var textTargetOrderId by mutableStateOf("")
    var textTargetOrderTotal by mutableStateOf("")
    var textTargetPurchaseId by mutableStateOf("")
    var textTargetProductId by mutableStateOf("")
    var textTargetProductCategoryId by mutableStateOf("")

    var targetParamsMbox = mutableStateListOf(OptimizePair("", ""))
    var targetParamsProfile = mutableStateListOf(OptimizePair("", ""))

    var optimizePropositionStateMap = mutableStateMapOf<String, OptimizeProposition>()

    //Visible logs for UI
    val logManager = LogManager()

    private val optimizePropositionUpdateCallback =
        object : AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>> {
            override fun call(propositions: Map<DecisionScope, OptimizeProposition>?) {
                logManager.addLog("onUpdateProposition | Success")
                propositions?.forEach {
                    optimizePropositionStateMap[it.key.name] = it.value
                }
            }

            override fun fail(error: AdobeError?) {
                logManager.addLog("onUpdateProposition | Failed | ${error?.errorName}")
                print("Error in updating OptimizeProposition:: ${error?.errorName ?: "Undefined"}.")
            }
        }

    init {
        Optimize.onPropositionsUpdate(optimizePropositionUpdateCallback)
    }

    //Begin: Calls to Optimize SDK APIs

    /**
     * Calls the Optimize SDK API to get the extension version see [Optimize.extensionVersion]
     */
    fun getOptimizeExtensionVersion(): String = Optimize.extensionVersion()

    /**
     * Calls the Optimize SDK API to get the Propositions see [Optimize.getPropositions]
     */
    fun getPropositions() {
        optimizePropositionStateMap.clear()

        val decisionScopeList = getDecisionScopes()
        val timeout = 1.0 //seconds
        val callback = object : AdobeCallbackWithError<Map<DecisionScope, OptimizeProposition>> {
            override fun call(propositions: Map<DecisionScope, OptimizeProposition>?) {
                logManager.addLog("Getting Propositions | Success")
                propositions?.forEach {
                    optimizePropositionStateMap[it.key.name] = it.value
                }
            }

            override fun fail(error: AdobeError?) {
                logManager.addLog("Getting Propositions | Failed | ${error?.errorName}")
                print("Error in getting Propositions.")
            }
        }

        logManager.addLog("Getting Propositions Called")
        Optimize.getPropositions(
            decisionScopeList,
            timeout,
            callback
        )
    }

    fun updatePropositions() {
        updateIdentity()

        val decisionScopeList = getDecisionScopes()
        val targetParams = getTargetParams()
        val data = getDataMap(targetParams)

        val callback =
            object : AdobeCallbackWithOptimizeError<Map<DecisionScope, OptimizeProposition>> {
                override fun call(propositions: Map<DecisionScope, OptimizeProposition>?) {
                    logManager.addLog("Update Propositions | Success")
                    Log.i("Optimize Test App", "Propositions updated successfully.")
                }

                override fun fail(error: AEPOptimizeError?) {
                    logManager.addLog("Update Propositions | Failed | ${error?.title}")
                    Log.i(
                        "Optimize Test App",
                        "Error in updating Propositions:: ${error?.title ?: "Undefined"}."
                    )
                }
            }

        optimizePropositionStateMap.clear()
        logManager.addLog("Update Propositions Called")
        Optimize.updatePropositions(
            decisionScopeList,
            mapOf(Pair("xdmKey", "1234")),
            data,
            5.0,
            callback
        )
    }

    /**
     * Calls the Optimize SDK API to clear the cached Propositions [Optimize.clearCachedPropositions]
     */
    fun clearCachedPropositions() {
        logManager.addLog("Clearing Propositions")
        optimizePropositionStateMap.clear()
        Optimize.clearCachedPropositions()
    }

    private fun updateIdentity() {
        // Send a custom Identity in IdentityMap as primary identifier to Edge network in personalization query request.
        val identityMap = IdentityMap()
        identityMap.addItem(
            IdentityItem("1111", AuthenticatedState.AUTHENTICATED, true),
            "userCRMID"
        )
        Identity.updateIdentities(identityMap)
    }

    //End: Calls to Optimize SDK APIs


    private fun getDecisionScopes(): List<DecisionScope> {
        return listOf(
            DecisionScope(textOdeText),
            DecisionScope(textOdeImage),
            DecisionScope(textOdeHtml),
            DecisionScope(textOdeJson),
            DecisionScope(textTargetMbox)
        )
    }

    private fun getDataMap(targetParams: Map<String, String>): MutableMap<String, Any> {
        val data = mutableMapOf<String, Any>()
        if (targetParams.isNotEmpty()) {
            data["__adobe"] = mapOf<String, Any>(Pair("target", targetParams))
        }
        data["dataKey"] = "5678"
        return data
    }

    private fun getTargetParams(): Map<String, String> {
        val targetParams = mutableMapOf<String, String>()

        if (textTargetMbox.isNotEmpty()) {
            targetParamsMbox.forEach {
                if (it.key.isNotEmpty() && it.value.isNotEmpty()) {
                    targetParams[it.key] = it.value
                }
            }

            targetParamsProfile.forEach {
                if (it.key.isNotEmpty() && it.value.isNotEmpty()) {
                    targetParams[it.key] = it.value
                }
            }

            if (isValidOrder) {
                targetParams["orderId"] = textTargetOrderId
                targetParams["orderTotal"] = textTargetOrderTotal
                targetParams["purchasedProductIds"] = textTargetPurchaseId
            }

            if (isValidProduct) {
                targetParams["productId"] = textTargetProductId
                targetParams["categoryId"] = textTargetProductCategoryId
            }
        }
        return targetParams
    }

    private val isValidOrder: Boolean
        get() = textTargetOrderId.isNotEmpty() && (textTargetOrderTotal.isNotEmpty()) && textTargetPurchaseId.isNotEmpty()

    private val isValidProduct: Boolean
        get() = textTargetProductId.isNotEmpty() && textTargetProductCategoryId.isNotEmpty()
}