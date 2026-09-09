package io.legado.app.ui.design.components.compose

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NgDismissibleDrawerTest {
    @Test fun smallMovementDoesNotDismissEvenWithVelocity() {
        assertFalse(shouldDismissDrawer(8f, 600f, 2000f, 1f))
    }

    @Test fun slowPullNeedsDistanceThreshold() {
        assertFalse(shouldDismissDrawer(80f, 600f, 0f, 1f))
        assertTrue(shouldDismissDrawer(96f, 600f, 0f, 1f))
    }

    @Test fun downwardFlingNeedsMinimumTravelAndCorrectDirection() {
        assertTrue(shouldDismissDrawer(24f, 600f, 1200f, 1f))
        assertFalse(shouldDismissDrawer(24f, 600f, -1200f, 1f))
    }

    @Test fun shortDrawersAndDensityUseEquivalentDistances() {
        assertTrue(shouldDismissDrawer(50f, 200f, 0f, 1f))
        assertFalse(shouldDismissDrawer(100f, 1200f, 0f, 2f))
        assertTrue(shouldDismissDrawer(192f, 1200f, 0f, 2f))
    }
}
