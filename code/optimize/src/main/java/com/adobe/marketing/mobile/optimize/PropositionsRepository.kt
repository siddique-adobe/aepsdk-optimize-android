package com.adobe.marketing.mobile.optimize


interface PropositionsRepository {
    var pendingDisplayedPropositions: MutableList<OptimizeProposition>
    var pendingTappedPropositions: MutableList<OptimizeProposition>
    fun addDisplayedPropositions(propositions: List<OptimizeProposition>)
    fun clearDisplayedPropositions()
    fun addTappedPropositions(propositions: List<OptimizeProposition>)
    fun clearTappedPropositions()
    fun trackTapInteraction()
    fun trackDisplayInteraction()
}