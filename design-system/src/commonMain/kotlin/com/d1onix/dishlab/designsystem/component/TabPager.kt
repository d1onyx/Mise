package com.d1onix.dishlab.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import kotlinx.coroutines.launch

/**
 * A row of tabs above a swipeable [HorizontalPager] — tapping a tab and
 * swiping the page both move the same [PagerState], so either gesture works.
 *
 * Content beyond a single glance (nutrition tables, sourcing, extra photos)
 * belongs on later pages; the first page is what the caller expects the user
 * to see without any interaction at all.
 *
 * [modifier] must give this a bounded height (e.g. `Modifier.weight(1f)` in a
 * non-scrolling `Column`) — the pager fills it and each [page] scrolls its own
 * content independently, so swiping never has to resize around a taller page.
 */
@Composable
fun MiseTabPager(
    tabs: List<String>,
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState(pageCount = { tabs.size }),
    page: @Composable (index: Int) -> Unit,
) {
    val colors = MiseTheme.colors
    val scope = rememberCoroutineScope()

    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.panel, RoundedCornerShape(12.dp))
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            tabs.forEachIndexed { index, label ->
                val selected = state.currentPage == index
                val shape = RoundedCornerShape(9.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shape)
                        .background(if (selected) colors.lime else Color.Transparent, shape)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { scope.launch { state.animateScrollToPage(index) } },
                        )
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MiseTheme.typography.bodySmall,
                        color = if (selected) colors.onLime else colors.textMuted,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalPager(
            state = state,
            // Each page scrolls its own content; the pager itself only needs
            // the height its container already bounds (see MiseTabPager doc).
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { index -> page(index) }
    }
}
