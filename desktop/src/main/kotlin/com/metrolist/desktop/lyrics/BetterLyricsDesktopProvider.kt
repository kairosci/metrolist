package com.metrolist.desktop.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

object BetterLyricsDesktopProvider : LyricsProvider {
    override val name = "BetterLyrics"

    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { isLenient = true; ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }
            defaultRequest {
                url("https://lyrics-api.boidu.dev")
                headers { append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36") }
            }
            expectSuccess = false
        }
    }

    override fun isEnabled(): Boolean = true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = runCatching {
        val response = client.get("/getLyrics") {
            parameter("s", title)
            parameter("a", artist)
            if (duration > 0) parameter("d", duration)
            if (!album.isNullOrBlank()) parameter("al", album)
        }

        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("BetterLyrics API returned ${response.status}")
        }

        val ttml = response.body<TTMLResponse>().ttml?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("No TTML content")

        val parsedLines = DesktopTTMLParser.parseTTML(ttml)
        if (parsedLines.isEmpty()) {
            throw IllegalStateException("Failed to parse TTML lyrics")
        }

        DesktopTTMLParser.toLRC(parsedLines)
    }

    @Serializable
    data class TTMLResponse(
        val ttml: String? = null,
        val status: Int? = null,
        val error: String? = null,
    )
}

object DesktopTTMLParser {
    data class ParsedLine(
        val text: String,
        val startTime: Double,
        val words: List<ParsedWord>,
        val agent: String? = null,
        val isBackground: Boolean = false,
        val backgroundLines: List<ParsedLine> = emptyList(),
    )

    data class ParsedWord(
        val text: String,
        val startTime: Double,
        val endTime: Double,
        val hasTrailingSpace: Boolean = true,
    )

    private data class SpanInfo(
        val text: String,
        val startTime: Double,
        val endTime: Double,
        val hasTrailingSpace: Boolean,
    )

    private const val TTML_PARAMETER_NS = "http://www.w3.org/ns/ttml#parameter"

    fun parseTTML(ttml: String): List<ParsedLine> {
        val lines = mutableListOf<ParsedLine>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            try { factory.setFeature("http://xml.org/sax/features/external-general-entities", false) } catch (_: Exception) {}
            try { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false) } catch (_: Exception) {}
            try { factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) } catch (_: Exception) {}

            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ttml.byteInputStream())
            val root = doc.documentElement

            var globalOffset = 0.0
            val head = root.findChild("head")
            if (head != null) {
                val meta = head.findChild("metadata")
                if (meta != null) {
                    val audio = meta.findChild("audio")
                    if (audio != null) {
                        globalOffset = audio.getAttribute("lyricOffset").toDoubleOrNull() ?: 0.0
                    }
                }
            }

            val body = root.findChild("body")
            if (body != null) {
                walk(body, lines, globalOffset, null)
            }
        } catch (e: Exception) {
            return emptyList()
        }
        return lines
    }

    private fun walk(element: Element, lines: MutableList<ParsedLine>, offset: Double, parentAgent: String?) {
        val name = element.localName ?: element.nodeName.substringAfterLast(':')
        var currentAgent = parentAgent
        when (name) {
            "div" -> {
                val a = attr(element, "agent")
                if (a.isNotEmpty()) currentAgent = a
            }
            "p" -> {
                parseP(element, lines, offset, currentAgent)
                return
            }
        }
        var child = element.firstChild
        while (child != null) {
            if (child is Element) walk(child, lines, offset, currentAgent)
            child = child.nextSibling
        }
    }

    private fun parseP(p: Element, lines: MutableList<ParsedLine>, offset: Double, divAgent: String?) {
        var begin = p.getAttribute("begin")
        if (begin.isEmpty()) begin = p.getAttributeNS(TTML_PARAMETER_NS, "begin")
        if (begin.isEmpty()) begin = findFirstSpanBegin(p) ?: return

        val startTime = parseTime(begin) + offset
        val spanInfos = mutableListOf<SpanInfo>()
        val backgroundLines = mutableListOf<ParsedLine>()
        val agent = attr(p, "agent").ifEmpty { divAgent }
        val isPBackground = attr(p, "role") == "x-bg"

        var child = p.firstChild
        while (child != null) {
            if (child is Element) {
                val name = child.localName ?: child.nodeName.substringAfterLast(':')
                if (name == "span") {
                    val role = attr(child, "role")
                    when (role) {
                        "x-bg" -> {
                            if (isPBackground) parseWordSpan(child, offset, spanInfos)
                            else parseBackgroundSpan(child, startTime, offset)?.let { backgroundLines.add(it) }
                        }
                        "x-translation", "x-roman" -> {}
                        else -> parseWordSpan(child, offset, spanInfos)
                    }
                }
            }
            child = child.nextSibling
        }

        val words = mergeSpansIntoWords(spanInfos)
        val lineText = if (words.isEmpty()) getDirectText(p).trim() else buildLineText(words)

        if (lineText.isNotEmpty()) {
            val bgLines = if (backgroundLines.isNotEmpty()) {
                listOf(
                    ParsedLine(
                        text = backgroundLines.joinToString(" ") { it.text },
                        startTime = backgroundLines.minOf { it.startTime },
                        words = backgroundLines.flatMap { it.words },
                        isBackground = true,
                    )
                )
            } else emptyList()
            lines.add(ParsedLine(lineText, startTime, words, agent, isPBackground, bgLines))
        } else if (backgroundLines.isNotEmpty()) {
            lines.add(
                ParsedLine(
                    text = backgroundLines.joinToString(" ") { it.text },
                    startTime = backgroundLines.minOf { it.startTime },
                    words = backgroundLines.flatMap { it.words },
                    isBackground = true,
                )
            )
        }
    }

    private fun parseWordSpan(span: Element, offset: Double, spanInfos: MutableList<SpanInfo>) {
        val begin = timingAttr(span, "begin")
        val end = timingAttr(span, "end")
        val text = span.textContent ?: ""
        if (begin.isNotEmpty() && end.isNotEmpty()) {
            val hasSpace = text.isNotEmpty() && text.last().isWhitespace()
            spanInfos.add(SpanInfo(text, parseTime(begin) + offset, parseTime(end) + offset, hasSpace))
        }
    }

    private fun parseBackgroundSpan(span: Element, parentStart: Double, offset: Double): ParsedLine? {
        val begin = timingAttr(span, "begin")
        val start = if (begin.isNotEmpty()) parseTime(begin) + offset else parentStart
        val spanInfos = mutableListOf<SpanInfo>()

        var child = span.firstChild
        var hasSpans = false
        while (child != null) {
            if (child is Element) {
                val name = child.localName ?: child.nodeName.substringAfterLast(':')
                if (name == "span") {
                    hasSpans = true
                    val role = attr(child, "role")
                    if (role != "x-translation" && role != "x-roman") parseWordSpan(child, offset, spanInfos)
                }
            }
            child = child.nextSibling
        }

        if (!hasSpans) {
            return ParsedLine(span.textContent?.trim() ?: "", start, emptyList(), isBackground = true)
        }

        val words = mergeSpansIntoWords(spanInfos)
        val text = if (words.isEmpty()) getDirectText(span).trim() else buildLineText(words)
        return ParsedLine(text, start, words, isBackground = true)
    }

    private fun findFirstSpanBegin(p: Element): String? {
        var child = p.firstChild
        var best: String? = null
        var bestSeconds = Double.POSITIVE_INFINITY
        while (child != null) {
            if (child is Element) {
                val name = child.localName ?: child.nodeName.substringAfterLast(':')
                if (name == "span") {
                    val b = timingAttr(child, "begin")
                    if (b.isNotEmpty()) {
                        val s = parseTime(b)
                        if (s < bestSeconds) { bestSeconds = s; best = b }
                    }
                }
            }
            child = child.nextSibling
        }
        return best
    }

    private fun getDirectText(el: Element): String {
        val sb = StringBuilder()
        var child = el.firstChild
        while (child != null) {
            if (child.nodeType == Node.TEXT_NODE) sb.append(child.textContent)
            else if (child is Element) {
                val name = child.localName ?: child.nodeName.substringAfterLast(':')
                val role = attr(child, "role")
                if (name == "span" && role != "x-bg" && role != "x-translation" && role != "x-roman") {
                    sb.append(child.textContent)
                }
            }
            child = child.nextSibling
        }
        return sb.toString()
    }

    private fun buildLineText(words: List<ParsedWord>) = buildString {
        words.forEachIndexed { i, w ->
            append(w.text)
            if (w.hasTrailingSpace && !w.text.endsWith('-') && i < words.lastIndex) append(" ")
        }
    }.trim()

    private fun mergeSpansIntoWords(spanInfos: List<SpanInfo>): List<ParsedWord> {
        if (spanInfos.isEmpty()) return emptyList()
        val words = mutableListOf<ParsedWord>()
        var text = StringBuilder(spanInfos[0].text)
        var start = spanInfos[0].startTime
        var end = spanInfos[0].endTime

        for (i in 1 until spanInfos.size) {
            val curr = spanInfos[i]
            if (spanInfos[i - 1].hasTrailingSpace && !spanInfos[i - 1].text.endsWith('-')) {
                words.add(ParsedWord(text.toString(), start, end, true))
                text = StringBuilder(curr.text)
                start = curr.startTime
                end = curr.endTime
            } else {
                text.append(curr.text)
                end = curr.endTime
            }
        }
        words.add(ParsedWord(text.toString(), start, end, spanInfos.last().hasTrailingSpace))
        return words.map { it.copy(text = it.text.trim()) }.filter { it.text.isNotEmpty() }
    }

    fun toLRC(lines: List<ParsedLine>): String {
        val agentMap = mutableMapOf<String, String>()
        lines.forEach { line ->
            line.agent?.lowercase()?.let { raw ->
                if (raw == "v1" || raw == "v2" || raw == "v1000") agentMap[raw] = raw
            }
        }
        var nextNum = 1
        lines.forEach { line ->
            line.agent?.lowercase()?.let { raw ->
                if (!agentMap.containsKey(raw)) {
                    while (nextNum <= 2 && (agentMap.containsKey("v$nextNum") || agentMap.values.contains("v$nextNum"))) nextNum++
                    agentMap[raw] = if (nextNum <= 2) "v$nextNum" else "v1"
                }
            }
        }
        if (agentMap.containsKey("v1000") && agentMap.containsKey("v1")) agentMap["v1000"] = "v2"

        val hasBackgroundLine = lines.any { it.isBackground }
        val multi = agentMap.size > 1 || (agentMap.size == 1 && !agentMap.containsKey("v1")) || (hasBackgroundLine && agentMap.size == 1 && agentMap.containsKey("v1"))

        val sb = StringBuilder(lines.size * 128)
        var lastBg = false
        lines.forEach { line ->
            val time = formatLrcTime(line.startTime)
            val isBg = line.isBackground
            if (!isBg) lastBg = false
            val agentId = agentMap[line.agent?.lowercase()]
            val tag = when {
                isBg -> if (lastBg) "" else "{bg}"
                multi && agentId != null -> "{agent:$agentId}"
                else -> ""
            }
            if (isBg) lastBg = true

            sb.append(time).append(tag).append(line.text).append('\n')
            if (line.words.isNotEmpty()) {
                sb.append('<')
                line.words.forEachIndexed { i, w ->
                    sb.append(w.text).append(':').append(w.startTime).append(':').append(w.endTime)
                    if (i < line.words.lastIndex) sb.append('|')
                }
                sb.append(">\n")
            }
            line.backgroundLines.forEach { bg ->
                val bTag = if (lastBg) "" else "{bg}"
                sb.append(formatLrcTime(bg.startTime)).append(bTag).append(bg.text).append('\n')
                lastBg = true
                if (bg.words.isNotEmpty()) {
                    sb.append('<')
                    bg.words.forEachIndexed { i, w ->
                        sb.append(w.text).append(':').append(w.startTime).append(':').append(w.endTime)
                        if (i < bg.words.lastIndex) sb.append('|')
                    }
                    sb.append(">\n")
                }
            }
        }
        return sb.toString()
    }

    private fun formatLrcTime(time: Double): String {
        val ms = (time * 1000).toLong()
        val m = ms / 60000
        val s = (ms % 60000) / 1000
        val c = (ms % 1000) / 10
        return "[${m.pad(2)}:${s.pad(2)}.${c.pad(2)}]"
    }

    private fun Long.pad(len: Int): String = toString().padStart(len, '0')

    private fun parseTime(time: String): Double {
        val t = time.trim()
        val c1 = t.indexOf(':')
        if (c1 != -1) {
            val c2 = t.lastIndexOf(':')
            return if (c1 == c2) {
                (t.substring(0, c1).toIntOrNull() ?: 0) * 60.0 + (t.substring(c1 + 1).toDoubleOrNull() ?: 0.0)
            } else {
                (t.substring(0, c1).toIntOrNull() ?: 0) * 3600.0 + (t.substring(c1 + 1, c2).toIntOrNull() ?: 0) * 60.0 + (t.substring(c2 + 1).toDoubleOrNull() ?: 0.0)
            }
        }
        if (t.endsWith("ms")) return (t.substring(0, t.length - 2).toDoubleOrNull() ?: 0.0) / 1000.0
        val s = if (t.endsWith("s") || t.endsWith("m") || t.endsWith("h")) t.substring(0, t.length - 1) else t
        val v = s.toDoubleOrNull() ?: 0.0
        return when {
            t.endsWith("m") -> v * 60.0
            t.endsWith("h") -> v * 3600.0
            else -> v
        }
    }

    private fun Element.findChild(localName: String): Element? {
        var child = firstChild
        while (child != null) {
            if (child is Element) {
                val name = child.localName ?: child.nodeName.substringAfterLast(':')
                if (name == localName) return child
            }
            child = child.nextSibling
        }
        return null
    }

    private fun attr(el: Element, localName: String): String {
        val direct = el.getAttribute(localName)
        if (direct.isNotEmpty()) return direct
        val ttm = el.getAttribute("ttm:$localName")
        if (ttm.isNotEmpty()) return ttm
        return el.getAttributeNS("http://www.w3.org/ns/ttml#metadata", localName)
    }

    private fun timingAttr(el: Element, localName: String): String {
        val direct = el.getAttribute(localName)
        if (direct.isNotEmpty()) return direct
        return el.getAttributeNS(TTML_PARAMETER_NS, localName)
    }
}
