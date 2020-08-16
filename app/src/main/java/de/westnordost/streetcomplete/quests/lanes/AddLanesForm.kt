package de.westnordost.streetcomplete.quests.lanes

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.view.View

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.quests.AbstractQuestFormAnswerFragment
import de.westnordost.streetcomplete.quests.OtherAnswer
import de.westnordost.streetcomplete.quests.lanes.LanesAnswer
import de.westnordost.streetcomplete.util.TextChangedWatcher
import kotlinx.android.synthetic.main.quest_lanes.*


class AddLanesForm : AbstractQuestFormAnswerFragment<LanesAnswer>() {

    override val contentLayoutResId = R.layout.quest_lanes

    override val otherAnswers = listOf(
        OtherAnswer(R.string.quest_lanes_no_markings) { confirmNoLanes() }
    )

    private val noOfLanes get() = lanesInput?.text?.toString().orEmpty().trim()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lanesInput.addTextChangedListener(TextChangedWatcher { checkIsFormComplete() })
    }

    override fun onClickOk() {
        applyAnswer(Lanes(noOfLanes))
    }

    private fun confirmNoLanes() {
        val ctx = context ?: return
        AlertDialog.Builder(ctx)
            .setTitle(R.string.quest_lanes_no_markings_title)
            .setPositiveButton(R.string.quest_lanes_yes_no_markings) { _, _ -> applyAnswer(NoLaneMarkings) }
            .setNegativeButton(R.string.quest_generic_confirmation_no,null)
            .show()
    }

    override fun isFormComplete() = lanesInput.text.toString() != ""
}
