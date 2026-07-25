package com.d1onix.dishlab.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ScoreVerdictTest {

    @Test
    fun `70 and above is a buy`() {
        assertEquals(ScoreVerdict.Buy, ScoreVerdict.of(70))
        assertEquals(ScoreVerdict.Buy, ScoreVerdict.of(100))
    }

    @Test
    fun `45 to 69 is a maybe`() {
        assertEquals(ScoreVerdict.Maybe, ScoreVerdict.of(45))
        assertEquals(ScoreVerdict.Maybe, ScoreVerdict.of(69))
    }

    @Test
    fun `below 45 is a skip`() {
        assertEquals(ScoreVerdict.Skip, ScoreVerdict.of(44))
        assertEquals(ScoreVerdict.Skip, ScoreVerdict.of(0))
    }
}

class TimeBucketTest {

    @Test
    fun `buckets follow the prototype boundaries`() {
        assertEquals(true, TimeBucket.Under15.matches(14))
        assertEquals(false, TimeBucket.Under15.matches(15))
        assertEquals(true, TimeBucket.Under30.matches(29))
        assertEquals(false, TimeBucket.Under30.matches(30))
        assertEquals(true, TimeBucket.Over30.matches(30))
        assertEquals(false, TimeBucket.Over30.matches(29))
    }
}
