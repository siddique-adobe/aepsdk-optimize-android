/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.optimize

object OfferExtension {
    fun List<Offer>?.displayed() {
        OptimizeUtils.trackWithData(this.generateDisplayInteractionXdm())
    }

    fun List<Offer>?.generateDisplayInteractionXdm(): Map<String, Any>? {
        return OptimizeUtils.generateInteractionXdm(
            OptimizeConstants.JsonValues.EE_EVENT_TYPE_PROPOSITION_DISPLAY,
            this?.toPropositionsList()
        )
    }

    private fun List<Offer>?.toPropositionsList(): List<OptimizeProposition>? {
        val propositions = this
            ?.mapNotNull { it.propositionReference?.get() }
            .takeIf { !it.isNullOrEmpty() }
        return propositions?.filter { proposition ->
            proposition.offers.any { offerInProp ->
                this?.any { it.id == offerInProp.id } == true
            }
        }
    }
}
