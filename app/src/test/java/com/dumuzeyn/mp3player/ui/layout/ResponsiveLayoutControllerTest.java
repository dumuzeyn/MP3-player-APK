package com.dumuzeyn.mp3player.ui.layout;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ResponsiveLayoutControllerTest {
    @Test
    public void tabletModeStartsAtSixHundredDp() {
        assertFalse(ResponsiveLayoutController.isTabletWidth(599));
        assertTrue(ResponsiveLayoutController.isTabletWidth(600));
        assertTrue(ResponsiveLayoutController.isTabletWidth(840));
    }
}
