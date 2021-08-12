/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.wear.tiles

import androidx.core.content.ContextCompat
import androidx.wear.tiles.ActionBuilders
import androidx.wear.tiles.ColorBuilders.argb
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
import androidx.wear.tiles.DimensionBuilders.degrees
import androidx.wear.tiles.DimensionBuilders.dp
import androidx.wear.tiles.DimensionBuilders.expand
import androidx.wear.tiles.LayoutElementBuilders.ARC_ANCHOR_START
import androidx.wear.tiles.LayoutElementBuilders.Arc
import androidx.wear.tiles.LayoutElementBuilders.ArcLine
import androidx.wear.tiles.LayoutElementBuilders.Box
import androidx.wear.tiles.LayoutElementBuilders.Column
import androidx.wear.tiles.LayoutElementBuilders.FontStyles
import androidx.wear.tiles.LayoutElementBuilders.Image
import androidx.wear.tiles.LayoutElementBuilders.Layout
import androidx.wear.tiles.LayoutElementBuilders.Spacer
import androidx.wear.tiles.LayoutElementBuilders.Text
import androidx.wear.tiles.ModifiersBuilders.Background
import androidx.wear.tiles.ModifiersBuilders.Clickable
import androidx.wear.tiles.ModifiersBuilders.Corner
import androidx.wear.tiles.ModifiersBuilders.Modifiers
import androidx.wear.tiles.ModifiersBuilders.Padding
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.ResourceBuilders.AndroidImageResourceByResId
import androidx.wear.tiles.ResourceBuilders.ImageResource
import androidx.wear.tiles.ResourceBuilders.Resources
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileProviderService
import androidx.wear.tiles.TimelineBuilders.Timeline
import androidx.wear.tiles.TimelineBuilders.TimelineEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future

// TODO: Review Constants.
// Updating this version triggers a new call to onResourcesRequest(). This is useful for dynamic
// resources, the contents of which change even though their id stays the same (e.g. a graph).
// In this sample, our resources are all fixed, so we use a constant value.
private const val RESOURCES_VERSION = "1"

// dimensions
private val PROGRESS_BAR_THICKNESS = dp(6f)
private val BUTTON_SIZE = dp(48f)
private val BUTTON_RADIUS = dp(24f)
private val BUTTON_PADDING = dp(12f)
private val VERTICAL_SPACING_HEIGHT = dp(8f)

// Complete degrees for a circle (relates to [Arc] component)
private const val ARC_TOTAL_DEGREES = 360f

// identifiers
private const val ID_IMAGE_START_RUN = "image_start_run"
private const val ID_CLICK_START_RUN = "click_start_run"

/**
 * Creates a Fitness Tile, showing your progress towards a daily goal. The progress is defined
 * randomly, for demo purposes only. A new random progress is shown when the user taps the button.
 */
class GoalsTileService : TileProviderService() {
    // For coroutines, use a custom scope we can cancel when the service is destroyed
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    // TODO: Build a Tile.
    override fun onTileRequest(requestParams: TileRequest) = serviceScope.future {
        val goalProgress = GoalsRepository.getGoalProgress()
        val deviceParams = requestParams.deviceParameters

        // Creates Tile.
        Tile.builder()
            // If there are any graphics/images defined in the Tile's layout, the system will
            // retrieve them via onResourcesRequest() and match them with this version number.
            .setResourcesVersion(RESOURCES_VERSION)

            // Creates a timeline to hold one or more tile entries for a specific time periods.
            .setTimeline(
                Timeline.builder().addTimelineEntry(
                    TimelineEntry.builder().setLayout(
                        Layout.builder().setRoot(
                            rootBoxLayout(goalProgress,deviceParams!!)
                        )
                    )
                )
            ).build()
    }

    // TODO: Supply resources (graphics) for the Tile.
    override fun onResourcesRequest(requestParams: ResourcesRequest) = serviceScope.future {
        Resources.builder()
            .setVersion(RESOURCES_VERSION)
            .addIdToImageMapping(
                ID_IMAGE_START_RUN,
                ImageResource.builder().setAndroidResourceByResId(
                    AndroidImageResourceByResId.builder().setResourceId(R.drawable.ic_run)
                )
            )
            .build()
    }

    // TODO: Review onDestroy() - cancellation of the serviceScope
    override fun onDestroy() {
        super.onDestroy()
        // Cleans up the coroutine
        serviceScope.cancel()
    }

    // TODO: Create root Box layout and content.
    fun rootBoxLayout(goalProgress: GoalProgress, deviceParams: DeviceParameters): Box {
        return Box.builder()
            .setWidth(expand()).setHeight(expand())
            .addContent(progressArc(goalProgress.percentage))
            .addContent(
                Column.builder()
                    .addContent(
                        currentStepsText(goalProgress.current.toString(),deviceParams)
                    )
                    .addContent(
                        totalStepsCount(
                            resources.getString(R.string.goal, goalProgress.goal),
                            deviceParams
                        )
                    )
                    .addContent(
                        Spacer.builder().setHeight(VERTICAL_SPACING_HEIGHT)
                    )
                    .addContent(
                        startRunButton()
                    )
            )
            .build()
    }

    fun startRunButton(): Image {
        return Image.builder()
            .setWidth(BUTTON_SIZE)
            .setHeight(BUTTON_SIZE)
            .setResourceId(ID_IMAGE_START_RUN)
            .setModifiers(
                Modifiers.builder()
                    .setPadding(
                        Padding.builder()
                            .setStart(BUTTON_PADDING)
                            .setEnd(BUTTON_PADDING)
                            .setTop(BUTTON_PADDING)
                            .setBottom(BUTTON_PADDING)
                    )
                    .setBackground(
                        Background.builder()
                            .setCorner(Corner.builder().setRadius(BUTTON_RADIUS))
                            .setColor(argb(ContextCompat.getColor(this,R.color.primaryDark)))
                    )
                    .setClickable(
                        Clickable.builder()
                            .setId(ID_CLICK_START_RUN)
                            .setOnClick(ActionBuilders.LoadAction.builder())
                    )
            ).build()
    }

    fun currentStepsText(current : String, deviceParams: DeviceParameters): Text{
        return Text.builder()
            .setText(current)
            .setFontStyle(FontStyles.display2(deviceParams))
            .build()
    }

    fun totalStepsCount(goal : String, deviceParams: DeviceParameters): Text {
        return Text.builder()
            .setText(goal)
            .setFontStyle(FontStyles.title3(deviceParams))
            .build()
    }

    // TODO: Create a function that constructs an Arc representation of the current step progress.
    fun progressArc(percentage : Float): Arc {
        return Arc.builder()
            .addContent(
                ArcLine.builder()
                    .setLength(degrees(percentage * ARC_TOTAL_DEGREES))
                    .setColor(argb(ContextCompat.getColor(this,R.color.primary)))
                    .setThickness(PROGRESS_BAR_THICKNESS)
            )
            .setAnchorAngle(degrees(0.0f))
            .setAnchorType(ARC_ANCHOR_START)
            .build()
    }

    // TODO: Create functions that construct/stylize Text representations of the step count & goal.


    // TODO: Create a function that constructs/stylizes a clickable Image of a running icon.


}
