package de.westnordost.streetcomplete.quests.lanes

import de.westnordost.osmapi.map.data.BoundingBox
import de.westnordost.osmapi.map.data.Element
import de.westnordost.osmfeatures.FeatureDictionary
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.elementgeometry.ElementGeometry
import de.westnordost.streetcomplete.data.osm.osmquest.OsmElementQuestType
import de.westnordost.streetcomplete.data.osm.changes.StringMapChangesBuilder
import de.westnordost.streetcomplete.data.osm.mapdata.OverpassMapDataAndGeometryApi
import de.westnordost.streetcomplete.data.tagfilters.FiltersParser
import de.westnordost.streetcomplete.data.tagfilters.getQuestPrintStatement
import de.westnordost.streetcomplete.data.tagfilters.toGlobalOverpassBBox
import java.util.concurrent.FutureTask

class AddLanes(
    private val overpassApi: OverpassMapDataAndGeometryApi,
    private val featureDictionaryFuture: FutureTask<FeatureDictionary>
) : OsmElementQuestType<LanesAnswer> {

    private val filter by lazy { FiltersParser().parse("""
        ways with
          highway = unclassified
          and
          (
            !lanes
            or
            lanes ~ 0|1.5|-1|none|no|yes
          )
        """.trimIndent()
    )}

    override val commitMessage = "Determine lanes"
    override val wikiLink = "Key:lanes"
    override val icon = R.drawable.ic_quest_street_lanes

    override fun getTitle(tags: Map<String, String>) = R.string.quest_lanes_title

    override fun getTitleArgs(tags: Map<String, String>, featureName: Lazy<String?>): Array<String> {
        val name = tags["name"]
        return if (name != null) arrayOf(name) else arrayOf()
    }

    override fun download(bbox: BoundingBox, handler: (element: Element, geometry: ElementGeometry?) -> Unit): Boolean {
        return overpassApi.query(getOverpassQuery(bbox)) { element, geometry ->
            handler(element, geometry)
        }
    }

    override fun isApplicableTo(element: Element) =
        filter.matches(element)

    override fun createForm() = AddLanesForm()

    override fun applyAnswerTo(answer: LanesAnswer, changes: StringMapChangesBuilder) {
        when(answer) {
            is NoLaneMarkings -> {
                changes.add("lane_markings", "no")
                changes.delete("lanes")
            }
            is Lanes -> changes.add("lanes", answer.lanes)
        }
    }

    private fun getOverpassQuery(bbox: BoundingBox) =
        bbox.toGlobalOverpassBBox() + "\n" + filter.toOverpassQLString() + getQuestPrintStatement()
}
