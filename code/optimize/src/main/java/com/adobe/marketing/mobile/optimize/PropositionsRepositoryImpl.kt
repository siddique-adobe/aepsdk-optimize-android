package com.adobe.marketing.mobile.optimize

import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.services.Log

object PropositionsRepositoryImpl : PropositionsRepository {
    private val SELF_TAG: String = "PropositionsDisplayRepositoryImpl"

    override var pendingDisplayedPropositions: MutableList<OptimizeProposition> = mutableListOf()
    override var pendingTappedPropositions: MutableList<OptimizeProposition> = mutableListOf()

    override fun addDisplayedPropositions(propositions: List<OptimizeProposition>) {
        if (propositions.isNotEmpty()) {
            this.pendingDisplayedPropositions.addAll(propositions)
        }
    }

    override fun clearDisplayedPropositions() {
        if (pendingDisplayedPropositions.isNotEmpty()) {
            this.pendingDisplayedPropositions.clear()
        }
    }

    override fun addTappedPropositions(propositions: List<OptimizeProposition>) {
        if (propositions.isNotEmpty()) {
            this.pendingTappedPropositions.addAll(propositions)
        }
    }

    override fun clearTappedPropositions() {
        if (pendingDisplayedPropositions.isNotEmpty()) {
            this.pendingTappedPropositions.clear()
        }
    }

    override fun trackTapInteraction() {
        val xdm =
            generateInteractionXdm(OptimizeConstants.JsonValues.EE_EVENT_TYPE_PROPOSITION_INTERACT)
        dispatchTrackEvent(xdm)
        clearTappedPropositions()
    }

    override fun trackDisplayInteraction() {
        val xdm =
            generateInteractionXdm(OptimizeConstants.JsonValues.EE_EVENT_TYPE_PROPOSITION_DISPLAY)
        dispatchTrackEvent(xdm)
        clearDisplayedPropositions()
    }

    /**
     * Generates a map containing XDM formatted data for `Experience Event -
     * OptimizeProposition Interactions` field group from this `OptimizeProposition` offer and
     * given `experienceEventType`.
     *
     *
     * The method returns null if the proposition reference within the offer is released and no
     * longer valid.
     *
     * @param experienceEventType [String] containing the event type for the Experience Event
     * @return `Map<String, Object>` containing the XDM data for the proposition interaction.
     */
    private fun generateInteractionXdm(experienceEventType: String): Map<String, Any> {
        val decisioningPropositions: MutableList<Map<String, Any>> = ArrayList()
        if (pendingDisplayedPropositions.isNotEmpty()) {
            for (prop in pendingDisplayedPropositions) {
                val propositionItem: MutableMap<String, Any> = HashMap()
                val propositionItemsList: MutableList<Map<String, Any>> = ArrayList()
                val map = hashMapOf<String, Any>(
                    OptimizeConstants.JsonKeys.DECISIONING_PROPOSITIONS_ID to prop.id,
                    OptimizeConstants.JsonKeys.DECISIONING_PROPOSITIONS_SCOPE to prop.scope,
                    OptimizeConstants.JsonKeys.DECISIONING_PROPOSITIONS_SCOPEDETAILS to prop.scopeDetails
                ).apply {
                    if (prop.offers.isNotEmpty() && prop.offers[0].id != null) {
                        propositionItem[OptimizeConstants.JsonKeys.DECISIONING_PROPOSITIONS_ITEMS_ID] =
                            prop.offers[0].id
                        propositionItemsList.add(propositionItem)
                        put(
                            OptimizeConstants.JsonKeys.DECISIONING_PROPOSITIONS_ITEMS,
                            propositionItemsList
                        )
                    }
                }
                decisioningPropositions.add(map)
            }
        }
        val experienceDecisioning =
            hashMapOf(OptimizeConstants.JsonKeys.DECISIONING_PROPOSITIONS to decisioningPropositions)
        val experience =
            hashMapOf(OptimizeConstants.JsonKeys.EXPERIENCE_DECISIONING to experienceDecisioning)
        val xdm = hashMapOf(
            OptimizeConstants.JsonKeys.EXPERIENCE to experience,
            OptimizeConstants.JsonKeys.EXPERIENCE_EVENT_TYPE to experienceEventType
        )
        return xdm
    }

    private fun dispatchTrackEvent(xdm: Map<String, Any>?) {
        if (xdm.isNullOrEmpty()) {
            Log.debug(
                OptimizeConstants.LOG_TAG,
                SELF_TAG,
                "Failed to dispatch track propositions request event: xdm is null or empty."
            )
            return
        }

        val eventData = hashMapOf(
            OptimizeConstants.EventDataKeys.REQUEST_TYPE to OptimizeConstants.EventDataValues.REQUEST_TYPE_TRACK,
            OptimizeConstants.EventDataKeys.PROPOSITION_INTERACTIONS to xdm
        )

        //check: event type for tap and display are same or different
        val edgeEvent = Event.Builder(
            OptimizeConstants.EventNames.TRACK_PROPOSITIONS_REQUEST,
            OptimizeConstants.EventType.OPTIMIZE,
            OptimizeConstants.EventSource.REQUEST_CONTENT
        ).apply {
            setEventData(eventData)
        }.build()

        MobileCore.dispatchEvent(edgeEvent)
    }
}