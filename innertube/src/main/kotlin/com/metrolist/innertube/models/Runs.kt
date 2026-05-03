package com.metrolist.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Runs(
    val runs: List<Run>?,
)

@Serializable
data class Run(
    val text: String,
    val navigationEndpoint: NavigationEndpoint?,
)

fun List<Run>.splitBySeparator(): List<List<Run>> {
    val res = mutableListOf<List<Run>>()
    var tmp = mutableListOf<Run>()
    forEach { run ->
        if (run.text == " • ") {
            res.add(tmp)
            tmp = mutableListOf()
        } else {
            tmp.add(run)
        }
    }
    res.add(tmp)
    return res
}

/**
 * Splits artist runs by conjunction separators (&, ,) to properly handle
 * cases where multiple artists are displayed as "Artist1 & Artist2" or "Artist1, Artist2"
 */
fun List<Run>.splitArtistsByConjunction(): List<Run> {
    val result = mutableListOf<Run>()
    forEach { run ->
        val text = run.text
        // Check if this run contains conjunction patterns
        if (text.contains(" & ") || text.contains(", ")) {
            // Split by both patterns and preserve navigationEndpoint if present
            val parts = text.split(Regex(" & |, "))
            parts.forEachIndexed { index, part ->
                if (part.isNotBlank()) {
                    result.add(Run(part.trim(), if (index == 0) run.navigationEndpoint else null))
                }
            }
        } else {
            result.add(run)
        }
    }
    return result
}

fun List<List<Run>>.clean(): List<List<Run>> =
    if (getOrNull(0)?.getOrNull(0)?.navigationEndpoint != null ||
        (getOrNull(0)?.getOrNull(0)?.text?.contains(regex = Regex("[&,]"))) != false
    ) {
        this
    } else {
        this.drop(1)
    }

fun List<Run>.oddElements() =
    filterIndexed { index, _ ->
        index % 2 == 0
    }
