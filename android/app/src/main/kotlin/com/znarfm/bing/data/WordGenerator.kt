package com.znarfm.bing.data

import java.util.Calendar
import kotlin.random.Random

object WordGenerator {
    private val cities = listOf(
        "tokyo", "paris", "london", "kyoto", "rome", "berlin", "madrid", "toronto", "sydney", "chicago",
        "seattle", "boston", "vancouver", "oslo", "vienna", "dublin", "florence", "singapore", "seoul", "amsterdam",
        "lisbon", "prague", "helsinki", "montreal", "copenhagen", "stockholm", "bangkok", "auckland", "melbourne", "edinburgh"
    )

    private val foods = listOf(
        "banana bread", "lasagna", "guacamole", "tomato soup", "apple pie", "pancakes", "hummus", "cheesecake",
        "fried rice", "oatmeal", "french toast", "lentil soup", "grilled cheese", "chocolate chip cookies", "smoothie",
        "tacos", "caesar salad", "pasta salad", "baked salmon", "waffles", "chili", "pizza dough", "mashed potatoes",
        "ramen", "vegetable stir fry", "sourdough bread", "miso soup", "avocado toast", "blueberry muffin", "mushroom risotto"
    )

    private val healthyFoods = listOf(
        "sourdough bread", "dark chocolate", "oatmeal", "greek yogurt", "avocado", "tofu", "green tea",
        "peanut butter", "quinoa", "popcorn", "almond milk", "cottage cheese", "chia seeds", "olive oil",
        "canned tuna", "brown rice", "kombucha", "hummus", "honey", "soy milk"
    )

    private val animals = listOf(
        "cats", "dogs", "dolphins", "elephants", "penguins", "whales", "wolves", "owls", "bears", "eagles",
        "horses", "otters", "pandas", "foxes", "tigers", "lions", "rabbits", "parrots", "hedgehogs", "hummingbirds",
        "koalas", "squirrels", "frogs", "turtles", "beavers", "giraffes", "zebras", "seals", "cheetahs", "chameleons"
    )

    private val animalVerbs = listOf(
        "purr", "bark", "migrate", "hibernate", "howl", "shed fur", "glow", "change color", "eat grass",
        "swim upstream", "sing in the morning", "sleep so much", "wag their tails", "make clicking sounds",
        "groom each other", "chirp at night"
    )

    private val hobbies = listOf(
        "gardening", "woodworking", "origami", "baking", "photography", "knitting", "chess", "pottery",
        "bird watching", "rock climbing", "calligraphy", "guitar", "watercolor painting", "running", "yoga",
        "archery", "astronomy", "camping", "cycling", "fishing", "sketching", "leathercraft", "sculpting",
        "candle making", "kayaking"
    )

    private val products = listOf(
        "laptop", "headphones", "desk chair", "running shoes", "backpack", "smartwatch", "coffee maker",
        "bicycle", "monitor", "air purifier", "camping tent", "mechanical keyboard", "water bottle", "yoga mat",
        "blender", "tablet", "acoustic guitar", "bookshelf", "microwave oven", "printer", "vacuum cleaner",
        "umbrella", "power bank", "sunglasses", "tea kettle", "slow cooker", "digital camera", "winter coat",
        "sleeping bag", "desk lamp"
    )

    private val places = listOf(
        "coffee shop", "bakery", "public library", "farmers market", "hardware store", "post office",
        "bike shop", "park", "car wash", "gym", "pharmacy", "grocery store", "pet store", "bookstore",
        "art museum", "botanical garden", "dry cleaners", "community center", "flower shop", "flea market"
    )

    private val concepts = listOf(
        "photosynthesis", "solar eclipse", "plate tectonics", "aurora borealis", "machine learning",
        "gravity", "biodiversity", "cryptography", "thermodynamics", "renaissance art", "ocean currents",
        "renewable energy", "nanotechnology", "game theory", "astronomy", "microbiology", "robotics",
        "dark matter", "fermentation", "cellular respiration", "string theory", "volcanic eruptions",
        "sound waves", "hydrologic cycle", "black holes", "genetics", "sedimentary rock", "deep sea trenches",
        "quantum mechanics", "ecosystem resilience"
    )

    private val sports = listOf(
        "marathon", "triathlon", "tennis tournament", "golf tournament", "track and field meet",
        "swim meet", "cross country championship", "cycling road race", "rowing regatta", "gymnastics competition",
        "volleyball tournament", "curling championship"
    )

    private val comparisons = listOf(
        "butter vs margarine", "baking soda vs baking powder", "weather vs climate", "astronomy vs astrology",
        "cold brew vs iced coffee", "stocks vs bonds", "hardware vs software", "hiking vs trekking",
        "frog vs toad", "alligator vs crocodile", "sea lion vs seal", "wasp vs hornet",
        "rabbit vs hare", "lemon vs lime", "sweet potato vs yam", "quinoa vs brown rice",
        "green tea vs black tea", "olive oil vs avocado oil", "cotton vs linen", "porcelain vs ceramic"
    )

    private fun List<String>.pick(random: Random): String = this[random.nextInt(size)]

    /** [count] distinct queries for one run (duplicates may not earn points). */
    fun distinctQueries(count: Int, random: Random = Random.Default): List<String> {
        val seen = LinkedHashSet<String>()
        var attempts = 0
        while (seen.size < count && attempts < count * 50) {
            seen.add(randomSentence(random = random))
            attempts++
        }
        return seen.toList()
    }

    /** Generates a realistic search query; wordCount is accepted for backward compatibility. */
    fun randomSentence(wordCount: Int = 5, random: Random = Random.Default): String {
        if (wordCount <= 0) return ""
        return when (random.nextInt(30)) {
            0 -> "how to clean ${products.pick(random)}"
            1 -> "how to fix ${products.pick(random)}"
            2 -> "how to use ${products.pick(random)}"
            3 -> "how to reset ${products.pick(random)}"
            4 -> "how to make ${foods.pick(random)}"
            5 -> "how to cook ${foods.pick(random)}"
            6 -> "how to learn ${hobbies.pick(random)}"
            7 -> "how to start ${hobbies.pick(random)}"
            8 -> "how to draw ${animals.pick(random)}"
            9 -> "best ${products.pick(random)} ${Calendar.getInstance().get(Calendar.YEAR)}"
            10 -> "best ${products.pick(random)} for beginners"
            11 -> "${places.pick(random)} near me"
            12 -> "best ${places.pick(random)} near me"
            13 -> "${places.pick(random)} open now"
            14 -> "${foods.pick(random)} recipe"
            15 -> "easy ${foods.pick(random)} recipe"
            16 -> "is ${healthyFoods.pick(random)} healthy"
            17 -> "calories in ${foods.pick(random)}"
            18 -> "what is ${concepts.pick(random)}"
            19 -> "how does ${concepts.pick(random)} work"
            20 -> "${concepts.pick(random)} facts"
            21 -> "${concepts.pick(random)} explained"
            22 -> "${cities.pick(random)} weather"
            23 -> "things to do in ${cities.pick(random)}"
            24 -> "${animals.pick(random)} facts"
            25 -> "why do ${animals.pick(random)} ${animalVerbs.pick(random)}"
            26 -> "beginner guide to ${hobbies.pick(random)}"
            27 -> "${sports.pick(random)} schedule"
            28 -> comparisons.pick(random)
            else -> {
                val p1 = products.pick(random)
                var p2 = products.pick(random)
                while (p2 == p1) {
                    p2 = products.pick(random)
                }
                "$p1 vs $p2"
            }
        }
    }
}
