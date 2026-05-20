package com.meilluer.smartspacer_irctc

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.meilluer.smartspacer_irctc.data.TicketRepository.arrivalTime
import com.meilluer.smartspacer_irctc.data.TicketRepository.coachNumber
import com.meilluer.smartspacer_irctc.data.TicketRepository.departureTime
import com.meilluer.smartspacer_irctc.data.TicketRepository.fromPlatform
import com.meilluer.smartspacer_irctc.data.TicketRepository.fromStation
import com.meilluer.smartspacer_irctc.data.TicketRepository.journeyStarted
import com.meilluer.smartspacer_irctc.data.TicketRepository.nextStation
import com.meilluer.smartspacer_irctc.data.TicketRepository.seatNumber
import com.meilluer.smartspacer_irctc.data.TicketRepository.seatType
import com.meilluer.smartspacer_irctc.data.TicketRepository.target_visibility_flag
import com.meilluer.smartspacer_irctc.data.TicketRepository.toPlatform
import com.meilluer.smartspacer_irctc.data.TicketRepository.toStation
import com.meilluer.smartspacer_irctc.data.TicketRepository.trainName
import com.meilluer.smartspacer_irctc.data.TicketRepository.trainNumber

import android.graphics.drawable.Icon as AndroidIcon

class Target: SmartspacerTargetProvider() {
    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {

        if (!target_visibility_flag) return emptyList()

        val subtitleText = if (journeyStarted && nextStation.isNotEmpty()) {
            "Next: $nextStation • Arr: $arrivalTime"
        } else {
            "$seatNumber/$coachNumber/$seatType • $fromStation ( $departureTime ) plt.$fromPlatform -> $toStation ( $arrivalTime ) plt.$toPlatform"
        }

        val seatDisplayText = if (seatType.isNotEmpty()) "$coachNumber, $seatNumber ($seatType)" else "$coachNumber, $seatNumber"

        val targets = mutableListOf<SmartspaceTarget>()
        targets.add(
            TargetTemplate.Basic(
                id = "IRCTC_ticket",
                componentName = ComponentName(context!!, Target::class.java),
                title = Text("$trainNumber / $trainName"),
                subtitle = Text(subtitleText),
                icon = Icon(AndroidIcon.createWithResource(context, R.drawable.noun_train_8295307),shouldTint=false),

            ).create()
        )
        return targets
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            label = "Smartspacer IRCTC Ticket",
            description = "Dynamically display IRCTC Ticket",
            icon = android.graphics.drawable.Icon.createWithResource(context, R.drawable.noun_train_8295307),
        )
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
target_visibility_flag=false
        notifyChange()
        return true
    }
}