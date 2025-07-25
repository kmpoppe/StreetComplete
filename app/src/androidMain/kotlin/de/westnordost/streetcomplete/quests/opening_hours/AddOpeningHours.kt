package de.westnordost.streetcomplete.quests.opening_hours

import de.westnordost.osm_opening_hours.parser.toOpeningHoursOrNull
import de.westnordost.osmfeatures.Feature
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.elementfilter.filters.RelativeDate
import de.westnordost.streetcomplete.data.elementfilter.filters.TagOlderThan
import de.westnordost.streetcomplete.data.elementfilter.toElementFilterExpression
import de.westnordost.streetcomplete.data.osm.geometry.ElementGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataWithGeometry
import de.westnordost.streetcomplete.data.osm.osmquests.OsmElementQuestType
import de.westnordost.streetcomplete.data.quest.AndroidQuest
import de.westnordost.streetcomplete.data.user.achievements.EditTypeAchievement.CITIZEN
import de.westnordost.streetcomplete.osm.Tags
import de.westnordost.streetcomplete.osm.isPlaceOrDisusedPlace
import de.westnordost.streetcomplete.osm.opening_hours.parser.isSupportedOpeningHours
import de.westnordost.streetcomplete.osm.updateCheckDateForKey
import de.westnordost.streetcomplete.osm.updateWithCheckDate

class AddOpeningHours(
    private val getFeature: (Element) -> Feature?
) : OsmElementQuestType<OpeningHoursAnswer>, AndroidQuest {

    /* See also AddWheelchairAccessBusiness and AddPlaceName, which has a similar list and is/should
       be ordered in the same way for better overview */
    private val filter by lazy { ("""
        nodes, ways with
        (
          (
            (
              shop and shop !~ no|vacant
              or amenity = bicycle_parking and bicycle_parking = building
              or amenity = parking and parking = multi-storey
              or amenity = recycling and recycling_type = centre
              or amenity = social_facility and social_facility ~ food_bank|clothing_bank|soup_kitchen|dairy_kitchen
              or amenity = toilets and (fee = yes or toilets:disposal = flush)
              or tourism = information and information = office
              or """ +

        // The common list is shared by the name quest, the opening hours quest and the wheelchair quest.
        // So when adding other tags to the common list keep in mind that they need to be appropriate for all those quests.
        // Independent tags can by added in the "opening_hours only" tab.

        mapOf(
            "amenity" to arrayOf(
                // common
                "restaurant", "cafe", "ice_cream", "fast_food", "bar", "pub", "biergarten",         // eat & drink
                "food_court", "nightclub",
                "cinema", "planetarium", "casino",                                                  // amenities
                "townhall", "courthouse", "embassy", "community_centre", "youth_centre", "library", // civic
                "driving_school", "music_school", "prep_school", "language_school", "dive_centre",  // learning
                "dancing_school", "ski_school", "flight_school", "surf_school", "sailing_school",
                "cooking_school",
                "bank", "bureau_de_change", "money_transfer", "post_office", "marketplace",         // commercial
                "internet_cafe", "payment_centre",
                "car_wash", "car_rental", "fuel",                                                   // car stuff
                "dentist", "doctors", "clinic", "pharmacy", "veterinary",                           // health
                "animal_boarding", "animal_shelter", "animal_breeding",                             // animals
                "coworking_space",                                                                  // work

                // name & opening hours
                "boat_rental"

                // not ATM because too often it's simply 24/7 and too often it is confused with
                // a bank that might be just next door because the app does not tell the user what
                // kind of object this is about
            ),
            "tourism" to arrayOf(
                // common
                "zoo", "aquarium", "theme_park", "gallery", "museum"
                // and tourism = information, see above
            ),
            "leisure" to arrayOf(
                // common
                "fitness_centre", "golf_course", "water_park", "miniature_golf", "bowling_alley",
                "amusement_arcade", "adult_gaming_centre", "tanning_salon", "sauna",

                // not sports_centre, dance etc because these are often sports clubs which have no
                // walk-in opening hours but training times
            ),
            "office" to arrayOf(
                // common
                "insurance", "government", "travel_agent", "tax_advisor", "religion",
                "employment_agency", "diplomatic", "coworking", "energy_supplier",
                "estate_agent", "lawyer", "telecommunication", "educational_institution",
                "association", "ngo", "it", "accountant", "property_management",
            ),
            "craft" to arrayOf(
                // common
                "carpenter", "shoemaker", "tailor", "photographer", "dressmaker",
                "electronics_repair", "key_cutter", "stonemason", "bookbinder",
                "jeweller", "sailmaker", "jeweller", "watchmaker", "clockmaker",
                "locksmith",  "window_construction",
            ),
            "healthcare" to arrayOf(
                // common
                "pharmacy", "doctor", "clinic", "dentist", "centre", "physiotherapist",
                "laboratory", "alternative", "psychotherapist", "optometrist", "podiatrist",
                "nurse", "counselling", "speech_therapist", "blood_donation", "sample_collection",
                "occupational_therapist", "dialysis", "vaccination_centre", "audiologist",
                "blood_bank", "nutrition_counselling",
            ),
        ).map { it.key + " ~ " + it.value.joinToString("|") }.joinToString("\n or ") + "\n" + """
            )
            and (!opening_hours or opening_hours older today -1 years)
          )
          or (
            opening_hours older today -1 years
            and (
              leisure = park
              or barrier
              or amenity ~ toilets|bicycle_rental|charging_station
            )
          )
        )
        and access !~ private|no
        and (
          name or brand or noname = yes or name:signed = no
          or barrier
          or amenity ~ toilets|bicycle_rental
        )
        and opening_hours:signed != no
    """).toElementFilterExpression() }

    override val changesetComment = "Survey opening hours"
    override val wikiLink = "Key:opening_hours"
    override val icon = R.drawable.ic_quest_opening_hours
    override val isReplacePlaceEnabled = true
    override val achievements = listOf(CITIZEN)

    private val olderThan1Year = TagOlderThan("opening_hours", RelativeDate(-365f))

    override fun getTitle(tags: Map<String, String>): Int {
        // treat invalid opening hours like it is not set at all
        val oh = tags["opening_hours"]?.toOpeningHoursOrNull(lenient = true)
        val hasSupportedOpeningHours = oh != null && oh.isSupportedOpeningHours()
        return if (hasSupportedOpeningHours) {
            R.string.quest_openingHours_resurvey_title
        } else {
            R.string.quest_openingHours_title
        }
    }

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> =
        mapData.filter { isApplicableTo(it) }

    override fun isApplicableTo(element: Element): Boolean {
        if (!filter.matches(element)) return false
        // only show places that can be named somehow
        if (!hasName(element)) return false
        // no opening_hours yet -> new survey
        val ohStr = element.tags["opening_hours"] ?: return true
        /* don't show if it was recently checked (actually already checked by filter, but it is a
           performance improvement to avoid parsing the opening hours en masse if possible) */
        if (!olderThan1Year.matches(element)) return false
        // invalid opening_hours rules -> applicable because we want to ask for opening hours again
        // be strict
        val oh = ohStr.toOpeningHoursOrNull(lenient = false) ?: return true
        // only display supported rules, however, those that are supported but have colliding
        // weekdays should be shown (->resurveyed), as they are likely mistakes
        return oh.rules.all { rule -> rule.isSupportedOpeningHours() } && !oh.containsTimePoints()
    }

    override fun getHighlightedElements(element: Element, getMapData: () -> MapDataWithGeometry) =
        getMapData().asSequence().filter { it.isPlaceOrDisusedPlace() }

    override fun createForm() = AddOpeningHoursForm()

    override fun applyAnswerTo(answer: OpeningHoursAnswer, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        if (answer is NoOpeningHoursSign) {
            tags["opening_hours:signed"] = "no"
            tags.updateCheckDateForKey("opening_hours")
            // don't delete current opening hours: these may be the correct hours, they are just not visible anywhere on the door
        } else {
            val openingHoursString = when (answer) {
                is RegularOpeningHours  -> answer.hours.toString()
                is AlwaysOpen           -> "24/7"
                is DescribeOpeningHours -> "\"" + answer.text.replace("\"", "") + "\""
                NoOpeningHoursSign      -> throw IllegalStateException()
            }
            tags.updateWithCheckDate("opening_hours", openingHoursString)
            if (tags["opening_hours:signed"] == "no") {
                tags.remove("opening_hours:signed")
            }
        }
        tags.remove("opening_hours:covid19")
    }

    private fun hasName(element: Element) = hasProperName(element.tags) || hasFeatureName(element)

    private fun hasProperName(tags: Map<String, String>): Boolean =
        tags.containsKey("name") || tags.containsKey("brand")

    private fun hasFeatureName(element: Element) = getFeature(element)?.name != null
}
