package com.omniroute.app.feature.home

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniroute.app.R
import com.omniroute.app.core.network.dto.TripPlanDto
import com.omniroute.app.core.ui.theme.LocalAppPalette
import com.omniroute.app.core.ui.theme.LocalJourneySeason
import com.omniroute.app.feature.auth.heroSeason

@Composable
internal fun SeasonCover(onClick: () -> Unit) {
    val season = LocalJourneySeason.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 186.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF211D1B))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.BottomStart,
    ) {
        Image(
            painter = painterResource(season.heroSeason().imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.25f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.83f),
                    ),
                ),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_season_enter),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
                Text(
                    text = stringResource(season.titleRes),
                    fontSize = 27.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(bottom = 4.dp).size(21.dp),
            )
        }
    }
}

@Composable
internal fun NewTripButton(onClick: () -> Unit) {
    val palette = LocalAppPalette.current
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.brandDark,
            contentColor = palette.onBrand,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(21.dp))
            Text(
                text = stringResource(R.string.home_new_trip),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
internal fun EmptyTrips() {
    val palette = LocalAppPalette.current
    Column {
        HorizontalDivider(color = palette.border)
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.bodyLarge,
            color = palette.muted,
            modifier = Modifier.padding(vertical = 20.dp),
        )
    }
}

@Composable
internal fun SavedTripRow(trip: TripPlanDto, onClick: () -> Unit) {
    val context = LocalContext.current
    val palette = LocalAppPalette.current
    val title = tripTitle(context, trip)
    val dayCount = trip.days.coerceAtLeast(1)
    val detail = buildList {
        trip.destCity?.trim()?.takeIf { it.isNotEmpty() && it !in title }?.let { add(it) }
        add(stringResource(R.string.home_trip_days, dayCount))
        if (trip.travelers > 0) add(stringResource(R.string.home_trip_travelers, trip.travelers))
        trip.createdAt?.take(10)?.takeIf { it.length == 10 }?.let { add(it) }
    }.joinToString(" · ")

    Column {
        HorizontalDivider(color = palette.border)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.brandTint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.DateRange,
                    contentDescription = null,
                    tint = palette.brandDark,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = palette.muted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun tripTitle(context: Context, trip: TripPlanDto): String {
    val title = trip.title?.trim().orEmpty()
    if (title.isNotEmpty()) {
        return title
    }
    val city = trip.destCity?.trim().orEmpty()
    return if (city.isNotEmpty()) {
        context.getString(R.string.home_trip_title, city)
    } else {
        context.getString(R.string.home_trip_unnamed)
    }
}

