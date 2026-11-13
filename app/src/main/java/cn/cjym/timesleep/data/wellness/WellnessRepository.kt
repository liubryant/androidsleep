package cn.cjym.timesleep.data.wellness

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable data class WellnessChoice(val text: String, val score: Int)
@Serializable data class WellnessQuestion(val id: String, val text: String, val dimension: String? = null, val choices: List<WellnessChoice>)
@Serializable data class Assessment(val id: String, val title: String, val subtitle: String, val introduction: String, val periodText: String, val questions: List<WellnessQuestion>)
@Serializable data class Interpretation(val maximum: Int, val level: String, val summary: String, val suggestions: List<String>)
@Serializable data class PersonalityProfile(val nickname: String, val summary: String, val strengths: String, val relationships: String, val workStyle: String, val growth: String)
@Serializable data class WellnessCatalog(val definitions: List<Assessment>, val profiles: Map<String, PersonalityProfile>, val interpretations: Map<String, List<Interpretation>>)
@Serializable data class Metric(val title: String, val value: Int)
@Serializable data class WellnessRecord(val id: String = UUID.randomUUID().toString(), val assessmentID: String, val title: String, val date: Long = System.currentTimeMillis(), val score: Int, val maximumScore: Int, val level: String, val summary: String, val suggestions: List<String>, val metrics: List<Metric> = emptyList())
@Serializable data class AssessmentDraft(val index: Int = 0, val answers: Map<String, Int> = emptyMap())
@Serializable data class InspirationTheme(val theme: String, val cover: Int, val quotes: List<String>)
data class Inspiration(val day: Int, val theme: String, val cover: Int, val quote: String) {
    val date: LocalDate get() = LocalDate.of(2026, 11, 4).plusDays(day.toLong())
}

object WellnessScoring {
    fun result(catalog: WellnessCatalog, definition: Assessment, answers: Map<String, Int>): WellnessRecord {
        require(definition.questions.all { q -> answers[q.id] != null && q.choices.any { it.score == answers[q.id] } }) { "请完成所有题目" }
        if (definition.id == "mbti") {
            val names = listOf(listOf("EI", "外向 E", "内向 I"), listOf("SN", "实感 S", "直觉 N"), listOf("TF", "思考 T", "情感 F"), listOf("JP", "判断 J", "感知 P"))
            var code = ""
            val metrics = names.map { dim ->
                val qs = definition.questions.filter { it.dimension == dim[0] }
                val position = (qs.sumOf { answers.getValue(it.id) } - qs.size).toDouble() / (qs.size * 3)
                val name = dim[if (position > .5) 2 else 1]
                code += name.last()
                Metric(name, (abs(position - .5) * 200).roundToInt())
            }
            val p = catalog.profiles.getValue(code)
            return WellnessRecord(assessmentID = definition.id, title = definition.title, score = metrics.sumOf { it.value } / 4, maximumScore = 100,
                level = "$code · ${p.nickname}", summary = p.summary, metrics = metrics,
                suggestions = listOf("优势：${p.strengths}", "沟通与关系：${p.relationships}", "学习与工作：${p.workStyle}", "成长提醒：${p.growth}"))
        }
        if (definition.id == "bigFive") {
            val metrics = listOf("外向性", "宜人性", "尽责性", "情绪稳定性", "开放性").map { name ->
                val qs = definition.questions.filter { it.dimension == name }
                Metric(name, (qs.sumOf { answers.getValue(it.id) } * 100.0 / qs.sumOf { it.choices.maxOf { c -> c.score } }).roundToInt())
            }
            val strongest = metrics.maxBy { it.value }.title
            return WellnessRecord(assessmentID = definition.id, title = definition.title, score = metrics.sumOf { it.value } / 5, maximumScore = 100, level = "个性画像", metrics = metrics,
                summary = "你目前表现更突出的倾向是${strongest}。五个维度共同构成完整画像，不代表能力高低，也不会把你限制在固定类型里。",
                suggestions = listOf("关注各维度在真实生活中的表现，而不是只看最高分。", "人格倾向较稳定，建议间隔 6 个月以上再测。", "把结果作为自我理解的起点，而不是固定标签。"))
        }
        val score = definition.questions.sumOf { answers.getValue(it.id) }
        val interpretation = catalog.interpretations.getValue(definition.id).first { score <= it.maximum }
        return WellnessRecord(assessmentID = definition.id, title = definition.title, score = score, maximumScore = definition.questions.sumOf { it.choices.maxOf { c -> c.score } }, level = interpretation.level, summary = interpretation.summary, suggestions = interpretation.suggestions)
    }
}

class WellnessRepository(context: Context) {
    private val prefs = context.getSharedPreferences("wellness_v1", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    val catalog = context.assets.open("wellness.json").bufferedReader().use { json.decodeFromString<WellnessCatalog>(it.readText()) }
    private val inspirations = context.assets.open("inspiration.json").bufferedReader().use { json.decodeFromString<List<InspirationTheme>>(it.readText()) }
    private val _records = MutableStateFlow(runCatching { json.decodeFromString<List<WellnessRecord>>(prefs.getString("records", "[]")!!) }.getOrDefault(emptyList()))
    val records = _records.asStateFlow()
    private val _favorites = MutableStateFlow(prefs.getStringSet("favorites", emptySet())!!.toSet())
    val favorites = _favorites.asStateFlow()
    fun toggleFavorite(id: String) {
        val next = if (id in _favorites.value) _favorites.value - id else _favorites.value + id
        prefs.edit().putStringSet("favorites", next).apply(); _favorites.value = next
    }
    fun save(record: WellnessRecord) {
        val next = (listOf(record) + _records.value.filterNot { it.id == record.id }).take(100)
        prefs.edit().putString("records", json.encodeToString(next)).remove("draft.${record.assessmentID}").apply(); _records.value = next
    }
    fun deleteRecord(id: String) {
        val next = _records.value.filterNot { it.id == id }
        prefs.edit().putString("records", json.encodeToString(next)).apply(); _records.value = next
    }
    fun clearHistory() { prefs.edit().remove("records").apply(); _records.value = emptyList() }
    fun draft(id: String) = runCatching { json.decodeFromString<AssessmentDraft>(prefs.getString("draft.$id", "{}")!!) }.getOrDefault(AssessmentDraft())
    fun saveDraft(id: String, draft: AssessmentDraft) { prefs.edit().putString("draft.$id", json.encodeToString(draft)).apply() }
    fun clearDraft(id: String) { prefs.edit().remove("draft.$id").apply() }
    fun today() = ChronoUnit.DAYS.between(LocalDate.of(2026, 11, 4), LocalDate.now()).toInt().coerceAtLeast(0)
    fun inspiration(day: Int): Inspiration {
        val edition = (day.coerceAtLeast(0) + 54) % 60
        val theme = inspirations[edition % 6]
        return Inspiration(day, theme.theme, theme.cover, theme.quotes[edition / 6])
    }
}
