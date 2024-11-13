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

internal object OfferExtension {
    @JvmStatic
    fun List<Offer>?.toUniquePropositionsList(): List<OptimizeProposition>? {
        if (this.isNullOrEmpty()) return null
        val seenPropsIds = mutableSetOf<String>()
        val offerIds = this.mapNotNull { it.id }.toSet()
        return this
            .mapNotNull { it.propositionReference?.get() }
            .filter { proposition ->
                with(proposition) {
                    id !in seenPropsIds && offers.any { it.id in offerIds }
                }
            }.onEach { proposition ->
                seenPropsIds.add(proposition.id)
                val filteredOffers = proposition.offers.filter { it.id in offerIds }
                OptimizeProposition(
                    proposition.id,
                    filteredOffers,
                    proposition.scope,
                    proposition.scopeDetails
                )
            }
    }
}
