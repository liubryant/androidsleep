package cn.cjym.timesleep.ui

import org.junit.Assert.*
import org.junit.Test

class MainTabInitializationTest {
    @Test fun accessingStartDestinationFirstLeavesAllTabsInitialized() {
        // Startup reads Sounds before the bottom bar enumerates the tabs.
        assertEquals("sounds", MainTab.Sounds.route)
        val tabs = MainTab.entries
        assertEquals(listOf("sounds", "sleep", "stories", "profile"), tabs.map { it.route })
        assertEquals(4, tabs.map { it.route }.toSet().size)
        tabs.forEach { tab ->
            assertTrue(tab.label.isNotBlank())
            assertTrue(tab.icon.name.isNotBlank())
        }
    }
}
