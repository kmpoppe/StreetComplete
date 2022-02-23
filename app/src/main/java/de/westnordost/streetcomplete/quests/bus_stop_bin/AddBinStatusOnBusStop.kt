package de.westnordost.streetcomplete.quests.bus_stop_bin

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.meta.updateWithCheckDate
import de.westnordost.streetcomplete.data.osm.osmquests.OsmFilterQuestType
import de.westnordost.streetcomplete.data.osm.osmquests.Tags
import de.westnordost.streetcomplete.data.user.achievements.QuestTypeAchievement.CITIZEN
import de.westnordost.streetcomplete.ktx.arrayOfNotNull
import de.westnordost.streetcomplete.ktx.containsAnyKey
import de.westnordost.streetcomplete.ktx.toYesNo
import de.westnordost.streetcomplete.quests.YesNoQuestAnswerFragment

class AddBinStatusOnBusStop : OsmFilterQuestType<Boolean>() {

    override val elementFilter = """
        nodes with
        (
          (public_transport = platform and ~bus|trolleybus|tram ~ yes)
          or
          (highway = bus_stop and public_transport != stop_position)
        )
        and physically_present != no and naptan:BusStopType != HAR
        and (!bin or bin older today -4 years)
    """

    override val changesetComment = "Add whether a bus stop has a bin"
    override val wikiLink = "Key:bin"
    override val icon = R.drawable.ic_quest_bin_public_transport

    override val questTypeAchievements = listOf(CITIZEN)

    override fun getTitle(tags: Map<String, String>): Int {
        val hasName = tags.containsAnyKey("name", "ref")
        val isTram = tags["tram"] == "yes"
        val addRef = additionalShowRef(tags)
        val hasRef =
            addRef != ""
            && tags["name"] != ""
            && !(tags["name"]?.endsWith(addRef))!!
            && !(tags["name"]?.startsWith(addRef))!!
            && !(tags["name"]?.endsWith(tags["ref"]!!.takeLast(2)))!! // Relevant in PL only
        return when {
            isTram && hasName && hasRef -> R.string.quest_busStopBin_tram_name_ref_title
            isTram && hasName ->           R.string.quest_busStopBin_tram_name_title
            isTram ->                      R.string.quest_busStopBin_tram_title
            hasName && hasRef ->           R.string.quest_busStopBin_name_ref_title
            hasName ->                     R.string.quest_busStopBin_name_title
            else ->                        R.string.quest_busStopBin_title
        }
    }

    override fun getTitleArgs(tags: Map<String, String>, featureName: Lazy<String?>): Array<String> =
        arrayOfNotNull(tags["name"] ?: tags["ref"], additionalShowRef(tags))

    override fun createForm() = YesNoQuestAnswerFragment()

    override fun applyAnswerTo(answer: Boolean, tags: Tags, timestampEdited: Long) {
        tags.updateWithCheckDate("bin", answer.toYesNo())
    }
}
