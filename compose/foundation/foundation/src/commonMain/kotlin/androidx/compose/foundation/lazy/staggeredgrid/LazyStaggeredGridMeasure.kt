/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.fastFold
import androidx.compose.foundation.fastMaxOfOrNull
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

@ExperimentalFoundationApi
internal fun LazyLayoutMeasureScope.measure(
    state: LazyStaggeredGridState,
    itemProvider: LazyLayoutItemProvider,
    resolvedSlotSums: IntArray,
    constraints: Constraints,
    isVertical: Boolean,
    beforeContentPadding: Int,
    afterContentPadding: Int,
): LazyStaggeredGridMeasureResult {
    val context = LazyStaggeredGridMeasureContext(
        state = state,
        itemProvider = itemProvider,
        resolvedSlotSums = resolvedSlotSums,
        constraints = constraints,
        isVertical = isVertical,
        beforeContentPadding = beforeContentPadding,
        afterContentPadding = afterContentPadding,
        measureScope = this
    )

    val initialItemIndices: IntArray
    val initialItemOffsets: IntArray

    Snapshot.withoutReadObservation {
        val firstVisibleIndices = state.scrollPosition.indices
        val firstVisibleOffsets = state.scrollPosition.offsets

        initialItemIndices =
            if (firstVisibleIndices.size == resolvedSlotSums.size) {
                firstVisibleIndices
            } else {
                // Grid got resized (or we are in a initial state)
                // Adjust indices accordingly
                context.spans.reset()
                IntArray(resolvedSlotSums.size).apply {
                    // Try to adjust indices in case grid got resized
                    for (lane in indices) {
                        this[lane] = if (lane < firstVisibleIndices.size) {
                            firstVisibleIndices[lane]
                        } else {
                            if (lane == 0) {
                                0
                            } else {
                                context.findNextItemIndex(this[lane - 1], lane)
                            }
                        }
                        // Ensure spans are updated to be in correct range
                        context.spans.setSpan(this[lane], lane)
                    }
                }
            }
        initialItemOffsets =
            if (firstVisibleOffsets.size == resolvedSlotSums.size) {
                firstVisibleOffsets
            } else {
                // Grid got resized (or we are in a initial state)
                // Adjust offsets accordingly
                IntArray(resolvedSlotSums.size).apply {
                    // Adjust offsets to match previously set ones
                    for (lane in indices) {
                        this[lane] = if (lane < firstVisibleOffsets.size) {
                            firstVisibleOffsets[lane]
                        } else {
                            if (lane == 0) 0 else this[lane - 1]
                        }
                    }
                }
            }
    }

    return context.measure(
        initialScrollDelta = state.scrollToBeConsumed.roundToInt(),
        initialItemIndices = initialItemIndices,
        initialItemOffsets = initialItemOffsets,
        canRestartMeasure = true,
    )
}

@OptIn(ExperimentalFoundationApi::class)
private class LazyStaggeredGridMeasureContext(
    val state: LazyStaggeredGridState,
    val itemProvider: LazyLayoutItemProvider,
    val resolvedSlotSums: IntArray,
    val constraints: Constraints,
    val isVertical: Boolean,
    val measureScope: LazyLayoutMeasureScope,
    val beforeContentPadding: Int,
    val afterContentPadding: Int,
) {
    val measuredItemProvider = LazyStaggeredGridMeasureProvider(
        isVertical,
        itemProvider,
        measureScope,
        resolvedSlotSums
    ) { index, key, placeables ->
        LazyStaggeredGridMeasuredItem(
            index,
            key,
            placeables,
            isVertical
        )
    }

    val spans = state.spans
}

@ExperimentalFoundationApi
private fun LazyStaggeredGridMeasureContext.measure(
    initialScrollDelta: Int,
    initialItemIndices: IntArray,
    initialItemOffsets: IntArray,
    canRestartMeasure: Boolean,
): LazyStaggeredGridMeasureResult {
    with(measureScope) {
        val itemCount = itemProvider.itemCount

        if (itemCount <= 0 || resolvedSlotSums.isEmpty()) {
            return LazyStaggeredGridMeasureResult(
                firstVisibleItemIndices = IntArray(0),
                firstVisibleItemScrollOffsets = IntArray(0),
                consumedScroll = 0f,
                measureResult = layout(constraints.minWidth, constraints.minHeight) {},
                canScrollForward = false,
                canScrollBackward = false,
                visibleItemsInfo = emptyList(),
                totalItemsCount = 0
            )
        }

        // todo(b/182882362): content padding

        val mainAxisAvailableSize =
            if (isVertical) constraints.maxHeight else constraints.maxWidth

        // represents the real amount of scroll we applied as a result of this measure pass.
        var scrollDelta = initialScrollDelta

        val firstItemIndices = initialItemIndices.copyOf()
        val firstItemOffsets = initialItemOffsets.copyOf()

        // update spans in case item count is lower than before
        ensureIndicesInRange(firstItemIndices, itemCount)

        // applying the whole requested scroll offset. we will figure out if we can't consume
        // all of it later
        firstItemOffsets.offsetBy(-scrollDelta)

        // if the current scroll offset is less than minimally possible
        if (firstItemIndices[0] == 0 && firstItemOffsets[0] < 0) {
            scrollDelta += firstItemOffsets[0]
            firstItemOffsets.fill(0)
        }

        // this will contain all the MeasuredItems representing the visible items
        val measuredItems = Array(resolvedSlotSums.size) {
            ArrayDeque<LazyStaggeredGridMeasuredItem>()
        }

        // include the start padding so we compose items in the padding area. before starting
        // scrolling forward we would remove it back
        firstItemOffsets.offsetBy(-beforeContentPadding)

        // define min and max offsets (min offset currently includes beforeContentPadding)
        val minOffset = -beforeContentPadding
        val maxOffset = mainAxisAvailableSize

        fun hasSpaceBehind(): Boolean {
            for (lane in firstItemIndices.indices) {
                val itemIndex = firstItemIndices[lane]
                val itemOffset = firstItemOffsets[lane]

                if (itemOffset < 0 && itemIndex > 0) {
                    return true
                }
            }

            return false
        }

        fun misalignedStart(laneIndex: Int): Boolean {
            // If we scrolled past the first item in the lane, we have a point of reference
            // to re-align items.
            // Case 1: Item offsets for first item are not aligned
            val misalignedOffsets = firstItemOffsets.any {
                it != firstItemOffsets[laneIndex]
            }
            // Case 2: Other lanes have more items than the current one
            val moreItemsInOtherLanes = firstItemIndices.indices.any { lane ->
                findPreviousItemIndex(firstItemIndices[lane], lane) != -1
            }
            // Case 3: the first item is in the wrong lane (it should always be in
            // the first one)
            val firstItemInWrongLane = spans.getSpan(0) != 0
            // If items are not aligned, reset all measurement data we gathered before and
            // proceed with initial measure
            return misalignedOffsets || moreItemsInOtherLanes || firstItemInWrongLane
        }

        // we had scrolled backward or we compose items in the start padding area, which means
        // items before current firstItemScrollOffset should be visible. compose them and update
        // firstItemScrollOffset
        while (hasSpaceBehind()) {
            val laneIndex = firstItemOffsets.indexOfMinValue()
            val previousItemIndex = findPreviousItemIndex(
                item = firstItemIndices[laneIndex],
                lane = laneIndex
            )

            if (previousItemIndex < 0) {
                break
            }

            if (spans.getSpan(previousItemIndex) == LazyStaggeredGridSpans.Unset) {
                spans.setSpan(previousItemIndex, laneIndex)
            }

            val measuredItem = measuredItemProvider.getAndMeasure(
                previousItemIndex,
                laneIndex
            )
            measuredItems[laneIndex].addFirst(measuredItem)

            firstItemIndices[laneIndex] = previousItemIndex
            firstItemOffsets[laneIndex] += measuredItem.sizeWithSpacings
        }

        // re-check if columns are aligned after measure
        if (firstItemOffsets.any { it < minOffset }) {
            val lane = firstItemOffsets.indexOfMinValue()
            if (misalignedStart(lane) && canRestartMeasure) {
                spans.reset()
                return measure(
                    initialScrollDelta = scrollDelta,
                    initialItemIndices = IntArray(firstItemIndices.size) { -1 },
                    initialItemOffsets = IntArray(firstItemOffsets.size) {
                        initialItemOffsets[lane]
                    },
                    canRestartMeasure = false
                )
            }
        }

        // if we were scrolled backward, but there were not enough items before. this means
        // not the whole scroll was consumed
        if (firstItemOffsets[0] < minOffset) {
            scrollDelta += firstItemOffsets[0]
            firstItemOffsets.offsetBy(minOffset - firstItemOffsets[0])
        }

        val currentItemIndices = initialItemIndices.copyOf().apply {
            // ensure indices match item count, in case it decreased
            ensureIndicesInRange(this, itemCount)
        }
        val currentItemOffsets = IntArray(initialItemOffsets.size) {
            -(initialItemOffsets[it] - scrollDelta)
        }

        // neutralize previously added start padding as we stopped filling the before content padding
        firstItemOffsets.offsetBy(beforeContentPadding)

        val maxMainAxis = (maxOffset + afterContentPadding).coerceAtLeast(0)

        // compose first visible items we received from state
        currentItemIndices.forEachIndexed { laneIndex, itemIndex ->
            if (itemIndex < 0) return@forEachIndexed

            val measuredItem = measuredItemProvider.getAndMeasure(itemIndex, laneIndex)
            currentItemOffsets[laneIndex] += measuredItem.sizeWithSpacings

            spans.setSpan(itemIndex, laneIndex)

            if (
                currentItemOffsets[laneIndex] <= minOffset &&
                measuredItem.index != itemCount - 1
            ) {
                // this item is offscreen and will not be placed. advance item index
                firstItemIndices[laneIndex] = -1
                firstItemOffsets[laneIndex] -= measuredItem.sizeWithSpacings
            } else {
                measuredItems[laneIndex].addLast(measuredItem)
            }
        }

        // then composing visible items forward until we fill the whole viewport.
        // we want to have at least one item in visibleItems even if in fact all the items are
        // offscreen, this can happen if the content padding is larger than the available size.
        while (
            currentItemOffsets.any { it <= maxMainAxis } || measuredItems.all { it.isEmpty() }
        ) {
            val currentLaneIndex = currentItemOffsets.indexOfMinValue()
            val nextItemIndex =
                findNextItemIndex(currentItemIndices[currentLaneIndex], currentLaneIndex)

            if (nextItemIndex >= itemCount) {
                // if any items changed its size, the spans may not behave correctly
                // there are no more items in this lane, but there could be more in others
                // recheck if we can add more items and reset spans accordingly
                var missedItemIndex = Int.MAX_VALUE
                currentItemIndices.forEachIndexed { laneIndex, i ->
                    if (laneIndex == currentLaneIndex) return@forEachIndexed
                    var itemIndex = findNextItemIndex(i, laneIndex)
                    while (itemIndex < itemCount) {
                        missedItemIndex = minOf(itemIndex, missedItemIndex)
                        spans.setSpan(itemIndex, LazyStaggeredGridSpans.Unset)
                        itemIndex = findNextItemIndex(itemIndex, laneIndex)
                    }
                }
                // there's at least one missed item which may fit current lane
                if (missedItemIndex != Int.MAX_VALUE && canRestartMeasure) {
                    // reset current lane to the missed item index and restart measure
                    initialItemIndices[currentLaneIndex] =
                        min(initialItemIndices[currentLaneIndex], missedItemIndex)
                    return measure(
                        initialScrollDelta = initialScrollDelta,
                        initialItemIndices = initialItemIndices,
                        initialItemOffsets = initialItemOffsets,
                        canRestartMeasure = false
                    )
                } else {
                    break
                }
            }

            if (firstItemIndices[currentLaneIndex] == -1) {
                firstItemIndices[currentLaneIndex] = nextItemIndex
            }
            spans.setSpan(nextItemIndex, currentLaneIndex)

            val measuredItem =
                measuredItemProvider.getAndMeasure(nextItemIndex, currentLaneIndex)
            currentItemOffsets[currentLaneIndex] += measuredItem.sizeWithSpacings

            if (
                currentItemOffsets[currentLaneIndex] <= minOffset &&
                measuredItem.index != itemCount - 1
            ) {
                // this item is offscreen and will not be placed. advance item index
                firstItemIndices[currentLaneIndex] = -1
                firstItemOffsets[currentLaneIndex] -= measuredItem.sizeWithSpacings
            } else {
                measuredItems[currentLaneIndex].addLast(measuredItem)
            }

            currentItemIndices[currentLaneIndex] = nextItemIndex
        }

        // we didn't fill the whole viewport with items starting from firstVisibleItemIndex.
        // lets try to scroll back if we have enough items before firstVisibleItemIndex.
        if (currentItemOffsets.all { it < maxOffset }) {
            val maxOffsetLane = currentItemOffsets.indexOfMaxValue()
            val toScrollBack = maxOffset - currentItemOffsets[maxOffsetLane]
            firstItemOffsets.offsetBy(-toScrollBack)
            currentItemOffsets.offsetBy(toScrollBack)
            while (
                firstItemOffsets.any { it < beforeContentPadding }
            ) {
                val laneIndex = firstItemOffsets.indexOfMinValue()
                val currentIndex =
                    if (firstItemIndices[laneIndex] == -1) {
                        itemCount
                    } else {
                        firstItemIndices[laneIndex]
                    }

                val previousIndex =
                    findPreviousItemIndex(currentIndex, laneIndex)

                if (previousIndex < 0) {
                    if (misalignedStart(laneIndex) && canRestartMeasure) {
                        spans.reset()
                        return measure(
                            initialScrollDelta = scrollDelta,
                            initialItemIndices = IntArray(firstItemIndices.size) { -1 },
                            initialItemOffsets = IntArray(firstItemOffsets.size) { 0 },
                            canRestartMeasure = false
                        )
                    }
                    break
                }

                spans.setSpan(previousIndex, laneIndex)

                val measuredItem = measuredItemProvider.getAndMeasure(
                    previousIndex,
                    laneIndex
                )
                measuredItems[laneIndex].addFirst(measuredItem)
                firstItemOffsets[laneIndex] += measuredItem.sizeWithSpacings
                firstItemIndices[laneIndex] = previousIndex
            }
            scrollDelta += toScrollBack

            val minOffsetLane = firstItemOffsets.indexOfMinValue()
            if (firstItemOffsets[minOffsetLane] < 0) {
                val offsetValue = firstItemOffsets[minOffsetLane]
                scrollDelta += offsetValue
                currentItemOffsets.offsetBy(offsetValue)
                firstItemOffsets.offsetBy(-offsetValue)
            }
        }

        // report the amount of pixels we consumed. scrollDelta can be smaller than
        // scrollToBeConsumed if there were not enough items to fill the offered space or it
        // can be larger if items were resized, or if, for example, we were previously
        // displaying the item 15, but now we have only 10 items in total in the data set.
        val consumedScroll = if (
            state.scrollToBeConsumed.roundToInt().sign == scrollDelta.sign &&
            abs(state.scrollToBeConsumed.roundToInt()) >= abs(scrollDelta)
        ) {
            scrollDelta.toFloat()
        } else {
            state.scrollToBeConsumed
        }

        // todo(b/182882362):
        // even if we compose items to fill before content padding we should ignore items fully
        // located there for the state's scroll position calculation (first item + first offset)

        // end measure

        val layoutWidth = if (isVertical) {
            constraints.maxWidth
        } else {
            constraints.constrainWidth(currentItemOffsets.max())
        }
        val layoutHeight = if (isVertical) {
            constraints.constrainHeight(currentItemOffsets.max())
        } else {
            constraints.maxHeight
        }

        // Placement

        val itemScrollOffsets = firstItemOffsets.copyOf().transform { -it }
        val positionedItems = MutableVector<LazyStaggeredGridPositionedItem>(
            capacity = measuredItems.sumOf { it.size }
        )
        while (measuredItems.any { it.isNotEmpty() }) {
            // find the next item to position
            val laneIndex = measuredItems.indexOfMinBy {
                it.firstOrNull()?.index ?: Int.MAX_VALUE
            }
            val item = measuredItems[laneIndex].removeFirst()

            // todo(b/182882362): arrangement/spacing support
            val mainAxisOffset = itemScrollOffsets[laneIndex]
            val crossAxisOffset = if (laneIndex == 0) 0 else resolvedSlotSums[laneIndex - 1]
            positionedItems.add(
                item.position(
                    laneIndex,
                    mainAxisOffset,
                    crossAxisOffset,
                )
            )
            itemScrollOffsets[laneIndex] += item.sizeWithSpacings
        }

        // todo: reverse layout support

        // End placement

        // only scroll backward if the first item is not on screen or fully visible
        val canScrollBackward = !(firstItemIndices[0] == 0 && firstItemOffsets[0] <= 0)
        // only scroll forward if the last item is not on screen or fully visible
        val canScrollForward = currentItemOffsets.any { it > maxOffset }

        @Suppress("UNCHECKED_CAST")
        return LazyStaggeredGridMeasureResult(
            firstVisibleItemIndices = firstItemIndices,
            firstVisibleItemScrollOffsets = firstItemOffsets,
            consumedScroll = consumedScroll,
            measureResult = layout(layoutWidth, layoutHeight) {
                positionedItems.forEach { item ->
                    item.place(this)
                }
            },
            canScrollForward = canScrollForward,
            canScrollBackward = canScrollBackward,
            visibleItemsInfo = positionedItems.asMutableList(),
            totalItemsCount = itemCount
        )
    }
}

private fun IntArray.offsetBy(delta: Int) {
    for (i in indices) {
        this[i] = this[i] + delta
    }
}

private fun IntArray.indexOfMinValue(): Int {
    var result = -1
    var min = Int.MAX_VALUE
    for (i in indices) {
        if (min > this[i]) {
            min = this[i]
            result = i
        }
    }

    return result
}

private inline fun <T> Array<T>.indexOfMinBy(block: (T) -> Int): Int {
    var result = -1
    var min = Int.MAX_VALUE
    for (i in indices) {
        val value = block(this[i])
        if (min > value) {
            min = value
            result = i
        }
    }

    return result
}

private fun IntArray.indexOfMaxValue(): Int {
    var result = -1
    var max = Int.MIN_VALUE
    for (i in indices) {
        if (max < this[i]) {
            max = this[i]
            result = i
        }
    }

    return result
}

private inline fun IntArray.transform(block: (Int) -> Int): IntArray {
    for (i in indices) {
        this[i] = block(this[i])
    }
    return this
}

private fun LazyStaggeredGridMeasureContext.ensureIndicesInRange(
    indices: IntArray,
    itemCount: Int
) {
    for (i in indices.indices) {
        while (indices[i] >= itemCount) {
            indices[i] = findPreviousItemIndex(indices[i], i)
        }
        if (indices[i] != -1) {
            // reserve item for span
            spans.setSpan(indices[i], i)
        }
    }
}

private fun LazyStaggeredGridMeasureContext.findPreviousItemIndex(item: Int, lane: Int): Int =
    spans.findPreviousItemIndex(item, lane)

private fun LazyStaggeredGridMeasureContext.findNextItemIndex(item: Int, lane: Int): Int =
    spans.findNextItemIndex(item, lane)

@OptIn(ExperimentalFoundationApi::class)
private class LazyStaggeredGridMeasureProvider(
    private val isVertical: Boolean,
    private val itemProvider: LazyLayoutItemProvider,
    private val measureScope: LazyLayoutMeasureScope,
    private val resolvedSlotSums: IntArray,
    private val measuredItemFactory: MeasuredItemFactory
) {
    private fun childConstraints(slot: Int): Constraints {
        val previousSum = if (slot == 0) 0 else resolvedSlotSums[slot - 1]
        val crossAxisSize = resolvedSlotSums[slot] - previousSum
        return if (isVertical) {
            Constraints.fixedWidth(crossAxisSize)
        } else {
            Constraints.fixedHeight(crossAxisSize)
        }
    }

    fun getAndMeasure(index: Int, slot: Int): LazyStaggeredGridMeasuredItem {
        val key = itemProvider.getKey(index)
        val placeables = measureScope.measure(index, childConstraints(slot))
        return measuredItemFactory.createItem(index, key, placeables)
    }
}

// This interface allows to avoid autoboxing on index param
private fun interface MeasuredItemFactory {
    fun createItem(
        index: Int,
        key: Any,
        placeables: List<Placeable>
    ): LazyStaggeredGridMeasuredItem
}

private class LazyStaggeredGridMeasuredItem(
    val index: Int,
    val key: Any,
    val placeables: List<Placeable>,
    val isVertical: Boolean
) {
    val sizeWithSpacings: Int = placeables.fastFold(0) { size, placeable ->
        size + if (isVertical) placeable.height else placeable.width
    }

    val crossAxisSize: Int = placeables.fastMaxOfOrNull {
        if (isVertical) it.width else it.height
    }!!

    fun position(
        lane: Int,
        mainAxis: Int,
        crossAxis: Int,
    ): LazyStaggeredGridPositionedItem =
        LazyStaggeredGridPositionedItem(
            offset = if (isVertical) {
                IntOffset(crossAxis, mainAxis)
            } else {
                IntOffset(mainAxis, crossAxis)
            },
            lane = lane,
            index = index,
            key = key,
            size = IntSize(sizeWithSpacings, crossAxisSize),
            placeables = placeables
        )
}

private class LazyStaggeredGridPositionedItem(
    override val offset: IntOffset,
    override val index: Int,
    override val lane: Int,
    override val key: Any,
    override val size: IntSize,
    private val placeables: List<Placeable>
) : LazyStaggeredGridItemInfo {
    fun place(scope: Placeable.PlacementScope) = with(scope) {
        placeables.fastForEach { placeable ->
            placeable.placeWithLayer(offset)
        }
    }
}
