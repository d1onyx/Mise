package com.d1onix.dishlab.feature.recipes.presentation

import androidx.compose.runtime.Composable
import com.d1onix.dishlab.domain.model.RecipeDifficulty
import com.d1onix.dishlab.domain.model.TimeBucket
import com.d1onix.dishlab.feature.recipes.resources.Res
import com.d1onix.dishlab.feature.recipes.resources.difficulty_easy
import com.d1onix.dishlab.feature.recipes.resources.difficulty_hard
import com.d1onix.dishlab.feature.recipes.resources.difficulty_medium
import com.d1onix.dishlab.feature.recipes.resources.time_over_30
import com.d1onix.dishlab.feature.recipes.resources.time_under_15
import com.d1onix.dishlab.feature.recipes.resources.time_under_30
import org.jetbrains.compose.resources.stringResource

/** Display text for the domain enums — the enums themselves carry no copy. */
@Composable
fun difficultyLabel(difficulty: RecipeDifficulty): String = stringResource(
    when (difficulty) {
        RecipeDifficulty.Easy -> Res.string.difficulty_easy
        RecipeDifficulty.Medium -> Res.string.difficulty_medium
        RecipeDifficulty.Hard -> Res.string.difficulty_hard
    }
)

@Composable
fun timeBucketLabel(bucket: TimeBucket): String = stringResource(
    when (bucket) {
        TimeBucket.Under15 -> Res.string.time_under_15
        TimeBucket.Under30 -> Res.string.time_under_30
        TimeBucket.Over30 -> Res.string.time_over_30
    }
)
