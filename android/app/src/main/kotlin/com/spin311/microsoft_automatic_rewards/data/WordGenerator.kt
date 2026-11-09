package com.spin311.microsoft_automatic_rewards.data

import kotlin.random.Random

object WordGenerator {
    private val adjectives = listOf(
        "ancient", "bright", "calm", "clever", "cosmic", "crystal", "curious", "digital",
        "dynamic", "electric", "emerald", "endless", "gentle", "golden", "hidden", "infinite",
        "luminous", "modern", "mystic", "natural", "noble", "northern", "organic", "pacific",
        "parallel", "peaceful", "planetary", "polished", "quantum", "radiant", "rapid", "rustic",
        "sapphire", "seasonal", "silent", "silver", "solar", "southern", "stellar", " tropical",
        "urban", "velvet", "vibrant", "vintage", "vivid", "western", "wild", "wooden"
    ).map { it.trim() }

    private val nouns = listOf(
        "academy", "algorithm", "archipelago", "architecture", "astronomy", "atmosphere",
        "avalanche", "aviation", "battery", "biography", "botany", "boulevard", "calendar",
        "campground", "canyon", "cathedral", "ceramics", "championship", "chemistry", "chronicle",
        "cinema", "civilization", "climate", "compass", "concert", "constellation", "continent",
        "coral", "cuisine", "Expedition", "dictionary", "discovery", " documentary", "ecosystem",
        "eclipse", "economics", "engineering", "equinox", "estuary", "explorer", "festival",
        "fjord", "forest", "fountain", "galaxy", "garden", "geography", "geology", "glacier",
        "harbor", "harmony", "harvest", "heritage", "horizon", "hurricane", "innovation",
        "instrument", "island", "journal", "jungle", "keyboard", "kingdom", "laboratory",
        "lagoon", "landscape", "language", "lantern", "library", "lighthouse", "literature",
        "magnetism", "marathon", "meadow", "medicine", "melody", "meridian", "meteor",
        "microscope", "mineral", "monument", "mountain", "museum", "mythology", "navigation",
        "nebula", "network", "notebook", "oasis", "observatory", "ocean", "orchestra",
        "origami", "painting", "palace", "panorama", "paradox", "peninsula", "philosophy",
        "photography", "physics", "planet", "plateau", "poetry", "portrait", "pyramid",
        "rainforest", "recipe", "redwood", "reflection", "renaissance", "reservoir", "river",
        "robotics", "sanctuary", "satellite", "savanna", "sculpture", "seashore", "seminar",
        "serenity", "skyline", "solstice", "spectrum", "stadium", "starlight", "strategy",
        "summit", "sunflower", "symphony", "telescope", "temple", "terrace", "theater",
        "thunder", "timeline", "topography", "tournament", "tradition", "trail", "treasure",
        "tundra", "universe", "valley", "velocity", "village", "vineyard", "volcano",
        "voyage", "waterfall", "weather", "wilderness", "wildlife", "windmill", "workshop"
    ).map { it.trim().lowercase() }

    private val verbs = listOf(
        "explores", "discovers", "builds", "transforms", "illuminates", "connects", "shapes",
        "inspires", "reveals", "balances", "preserves", "accelerates", "guides", "captures",
        "enhances", "supports", "creates", "studies", "measures", "navigates", "cultivates",
        "improves", "designs", "observes", "organizes", "protects", "evolves", "expands"
    )

    private val contexts = listOf(
        "across history", "in modern science", "around the world", "for beginners",
        "in daily life", "during winter", "during summer", "in nature", "with technology",
        "for future generations", "in architecture", "through photography", "in classic art",
        "under the microscope", "beyond the horizon", "in coastal regions", "in mountain ranges",
        "for sustainable living", "in space exploration", "in culinary arts"
    )

    fun randomSentence(wordCount: Int = 5, random: Random = Random.Default): String {
        if (wordCount <= 0) return ""
        val pool = mutableListOf<String>()
        pool.add(adjectives[random.nextInt(adjectives.size)])
        pool.add(nouns[random.nextInt(nouns.size)])
        pool.add(verbs[random.nextInt(verbs.size)])
        pool.add(adjectives[random.nextInt(adjectives.size)])
        pool.add(nouns[random.nextInt(nouns.size)])
        while (pool.size < wordCount) {
            val contextWords = contexts[random.nextInt(contexts.size)].split(" ")
            pool.addAll(contextWords)
        }
        return pool.take(wordCount).joinToString(" ")
    }
}
