package de.westnordost.streetcomplete.quests.lanes

sealed class LanesAnswer

data class Lanes(val lanes:String) : LanesAnswer()
//data class Width(val width:String) : LanesAnswer()
object NoLaneMarkings : LanesAnswer()
