package cn.cjym.timesleep.data.wellness

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class WellnessScoringTest {
    private val catalog = Json.decodeFromString<WellnessCatalog>(File("src/main/assets/wellness.json").readText())
    @Test fun allAssessmentsHaveValidBoundaryResults() {
        assertEquals(7, catalog.definitions.size)
        assertEquals(87, catalog.definitions.sumOf { it.questions.size })
        catalog.definitions.forEach { definition ->
            for (high in listOf(false, true)) {
                val answers = definition.questions.associate { q -> q.id to if(high) q.choices.maxOf { it.score } else q.choices.minOf { it.score } }
                val result = WellnessScoring.result(catalog, definition, answers)
                assertTrue(result.level.isNotBlank())
                assertTrue(result.score in 0..result.maximumScore)
                assertTrue(result.metrics.all { it.value in 0..100 })
            }
        }
    }
    @Test fun mbtiEndpointsAndEveryProfileAreReachable() {
        val definition = catalog.definitions.first { it.id == "mbti" }
        for(mask in 0..15) {
            val dimensions = listOf("EI", "SN", "TF", "JP")
            val expected = dimensions.mapIndexed { i, letters -> letters[if(mask and (1 shl i) == 0) 0 else 1] }.joinToString("")
            val answers = definition.questions.associate { q -> q.id to if(mask and (1 shl dimensions.indexOf(q.dimension)) == 0) 1 else 4 }
            val result = WellnessScoring.result(catalog, definition, answers)
            assertTrue(result.level.startsWith(expected))
            assertEquals(100, result.score)
        }
    }
    @Test fun phqTwoThresholdIsThree() {
        val definition = catalog.definitions.first { it.id == "mood" }
        val low = definition.questions.associate { it.id to 0 }
        val below = WellnessScoring.result(catalog, definition, low + (definition.questions.first().id to 2))
        val at = WellnessScoring.result(catalog, definition, low + (definition.questions.first().id to 3))
        assertNotEquals(below.level, at.level)
    }
    @Test(expected = IllegalArgumentException::class) fun incompleteAnswersCannotBecomeReports() {
        WellnessScoring.result(catalog, catalog.definitions.first(), emptyMap())
    }
    @Test fun allArticleAssetsResolve() {
        val stories = Json { ignoreUnknownKeys=true }.decodeFromString<List<cn.cjym.timesleep.data.model.StoryItem>>(File("src/main/assets/StoryResources/contemporary_articles.json").readText())
        assertEquals(553, stories.size)
        assertEquals(stories.size, stories.map { it.id }.toSet().size)
        stories.forEach { story ->
            assertTrue(story.coverAssetPath, File("src/main/assets/${story.coverAssetPath}").exists())
            assertTrue(story.storyTextAssetPath, File("src/main/assets/${story.storyTextAssetPath}").exists())
        }
    }
}
