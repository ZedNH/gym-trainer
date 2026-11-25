package com.gymtrainer.app.domain

import java.util.Locale
import kotlin.random.Random

// =====================================================================================
//  Voice coach: what the trainer says during workouts, in English or Bangla (বাংলা).
//  Screens emit CoachCue events; CoachLines turns them into sentences for the chosen language.
// =====================================================================================

enum class CoachLanguage(val label: String, val locales: List<Locale>) {
    ENGLISH("English", listOf(Locale.US, Locale.UK, Locale.ENGLISH)),
    BANGLA("বাংলা", listOf(Locale("bn", "BD"), Locale("bn", "IN"), Locale("bn")));

    companion object {
        fun from(name: String?) = entries.firstOrNull { it.name == name } ?: ENGLISH
    }
}

/** The exercise a cue is about. */
data class CoachExercise(val id: String, val name: String, val pattern: String = "", val muscle: String = "")

sealed interface CoachCue {
    data class Ready(val first: CoachExercise) : CoachCue
    data class Work(val ex: CoachExercise, val reps: Int?, val seconds: Int?, val last: Boolean) : CoachCue
    data class Rest(val seconds: Int, val next: CoachExercise?) : CoachCue
    data class Count(val n: Int) : CoachCue
    data object Half : CoachCue
    data class Tip(val ex: CoachExercise) : CoachCue
    data object Motivate : CoachCue
    data object RestTip : CoachCue
    data object Done : CoachCue
    // Gym logger
    data class GymExercise(val ex: CoachExercise, val sets: Int, val repMin: Int, val repMax: Int) : CoachCue
    data class SetLogged(val setNumber: Int, val totalSets: Int) : CoachCue
    data object RestOver : CoachCue
}

object CoachLines {

    fun text(cue: CoachCue, lang: CoachLanguage, rnd: Random = Random): String =
        if (lang == CoachLanguage.BANGLA) bn(cue, rnd) else en(cue, rnd)

    // ---------------- English ----------------
    private fun en(c: CoachCue, rnd: Random): String = when (c) {
        is CoachCue.Ready -> "Get ready. First up: ${c.first.name}."
        is CoachCue.Work -> (if (c.last) "Last exercise! " else "") + c.ex.name + ". " +
            (if (c.seconds != null) "${c.seconds} seconds. Go!" else "${c.reps} reps. Go!")
        is CoachCue.Rest -> "Rest for ${c.seconds} seconds." + (c.next?.let { " Next: ${it.name}." } ?: "")
        is CoachCue.Count -> "${c.n}"
        CoachCue.Half -> "Halfway there. Keep going!"
        is CoachCue.Tip -> Tips.forExercise(c.ex, CoachLanguage.ENGLISH, rnd)
        CoachCue.Motivate -> listOf("Great work, keep it up!", "You've got this!", "Stay strong, almost there!",
            "Nice and steady. Breathe.", "Push a little more!").random(rnd)
        CoachCue.RestTip -> listOf("Take a sip of water.", "Breathe in through your nose, out through your mouth.",
            "Shake out your arms and legs.", "Slow your breathing down.").random(rnd)
        CoachCue.Done -> "Well done! Workout complete. Great job today."
        is CoachCue.GymExercise -> "${c.ex.name}. ${c.sets} sets of ${c.repMin} to ${c.repMax} reps. " +
            Tips.forExercise(c.ex, CoachLanguage.ENGLISH, rnd)
        is CoachCue.SetLogged -> if (c.setNumber >= c.totalSets) "All sets done. Nice work!" else "Set ${c.setNumber} done. Rest now."
        CoachCue.RestOver -> "Rest is over. Start your next set."
    }

    // ---------------- Bangla ----------------
    private fun bn(c: CoachCue, rnd: Random): String = when (c) {
        is CoachCue.Ready -> "প্রস্তুত হোন। প্রথম ব্যায়াম: ${BanglaNames.of(c.first)}।"
        is CoachCue.Work -> (if (c.last) "এটাই শেষ ব্যায়াম! " else "") + BanglaNames.of(c.ex) + "। " +
            (if (c.seconds != null) "${num(c.seconds)} সেকেন্ড। শুরু করুন!" else "${num(c.reps ?: 0)} বার। শুরু করুন!")
        is CoachCue.Rest -> "বিশ্রাম নিন, ${num(c.seconds)} সেকেন্ড।" + (c.next?.let { " পরের ব্যায়াম: ${BanglaNames.of(it)}।" } ?: "")
        is CoachCue.Count -> num(c.n)
        CoachCue.Half -> "অর্ধেক শেষ। চালিয়ে যান!"
        is CoachCue.Tip -> Tips.forExercise(c.ex, CoachLanguage.BANGLA, rnd)
        CoachCue.Motivate -> listOf("দারুণ হচ্ছে, চালিয়ে যান!", "আপনি পারবেন, থামবেন না!", "আর একটু, প্রায় শেষ!",
            "ছন্দ ধরে রাখুন, শ্বাস নিন।", "আরেকটু জোর দিন!").random(rnd)
        CoachCue.RestTip -> listOf("এক চুমুক পানি খেয়ে নিন।", "নাক দিয়ে শ্বাস নিন, মুখ দিয়ে ধীরে ছাড়ুন।",
            "হাত-পা একটু ঝাঁকিয়ে নিন।", "শ্বাস-প্রশ্বাস ধীরে ধীরে স্বাভাবিক করুন।").random(rnd)
        CoachCue.Done -> "অসাধারণ! আজকের ওয়ার্কআউট শেষ। খুব ভালো করেছেন!"
        is CoachCue.GymExercise -> "${BanglaNames.of(c.ex)}। ${num(c.sets)} সেট, প্রতি সেটে ${num(c.repMin)} থেকে ${num(c.repMax)} বার। " +
            Tips.forExercise(c.ex, CoachLanguage.BANGLA, rnd)
        is CoachCue.SetLogged -> if (c.setNumber >= c.totalSets) "সব সেট শেষ। দারুণ!" else "${num(c.setNumber)} নম্বর সেট শেষ। এখন বিশ্রাম নিন।"
        CoachCue.RestOver -> "বিশ্রাম শেষ। পরের সেট শুরু করুন।"
    }

    /** Bangla digits (Bangla TTS reads them naturally). */
    fun num(n: Int): String = n.toString().map { if (it.isDigit()) '০' + (it - '0') else it }.joinToString("")
}

/** Short form cues, picked by what kind of exercise it is. */
object Tips {
    private data class T(val keys: List<String>, val en: List<String>, val bn: List<String>)

    private val TABLE = listOf(
        T(listOf("plank", "bridge", "hollow"), listOf("Keep your body in one straight line.", "Squeeze your glutes and brace your belly.", "Don't let your hips sag."),
            listOf("শরীর একদম সোজা লাইনে রাখুন।", "পেট শক্ত করুন, নিতম্ব চেপে রাখুন।", "কোমর নিচে ঝুলে পড়তে দেবেন না।")),
        T(listOf("push-up", "pushup", "push up", "press-up"), listOf("Elbows at about 45 degrees, not flared out.", "Lower your chest all the way, then push the floor away.", "Keep your core tight, hips level."),
            listOf("কনুই শরীরের কাছাকাছি রাখুন, বেশি ছড়াবেন না।", "বুক মেঝের কাছে নামান, তারপর জোরে ঠেলে উঠুন।", "পেট শক্ত রাখুন, কোমর সমান রাখুন।")),
        T(listOf("squat", "sit"), listOf("Chest up, weight in your heels.", "Knees follow your toes; don't let them cave in.", "Sit back like into a chair."),
            listOf("বুক উঁচু রাখুন, ভর গোড়ালিতে রাখুন।", "হাঁটু পায়ের আঙুলের দিকে রাখুন, ভেতরে ঢুকতে দেবেন না।", "চেয়ারে বসার মতো পেছনে নামুন।")),
        T(listOf("lunge", "split", "step"), listOf("Front knee over your ankle.", "Torso tall, step long enough.", "Push through your front heel to come up."),
            listOf("সামনের হাঁটু গোড়ালির ঠিক ওপরে রাখুন।", "শরীর সোজা রাখুন, পা যথেষ্ট লম্বা করে ফেলুন।", "সামনের গোড়ালিতে ভর দিয়ে উঠে আসুন।")),
        T(listOf("deadlift", "good morning", "swing", "hinge", "romanian"), listOf("Hinge at the hips with a flat back.", "Keep the weight close to your legs.", "Drive your hips forward at the top."),
            listOf("পিঠ সোজা রেখে কোমর থেকে ঝুঁকুন।", "ওজন পায়ের কাছাকাছি রাখুন।", "ওপরে উঠে কোমর সামনে ঠেলে নিতম্ব চাপুন।")),
        T(listOf("crunch", "sit-up", "situp", "v up", "v-up", "leg raise", "jackknife", "tuck"), listOf("Move slowly; feel your abs work.", "Don't pull on your neck.", "Breathe out as you curl up."),
            listOf("ধীরে করুন, পেটের পেশিতে চাপ অনুভব করুন।", "ঘাড় টেনে ধরবেন না।", "ওপরে ওঠার সময় শ্বাস ছাড়ুন।")),
        T(listOf("twist", "rotation", "wiper", "woodchop", "chop"), listOf("Turn from your torso, not just your arms.", "Keep it controlled, no jerking.", "Brace your core the whole time."),
            listOf("শুধু হাত নয়, শরীরের মাঝখান থেকে ঘুরুন।", "নিয়ন্ত্রণ রেখে করুন, ঝাঁকি দেবেন না।", "পুরো সময় পেট শক্ত রাখুন।")),
        T(listOf("jump", "burpee", "jack", "skip", "knees", "climber", "hop", "bound", "kick"), listOf("Land softly with bent knees.", "Stay light on your feet.", "Keep a steady rhythm and keep breathing."),
            listOf("হাঁটু ভাঁজ করে আলতো করে নামুন।", "পায়ের পাতায় হালকা থাকুন।", "ছন্দ ধরে রাখুন, শ্বাস নিতে থাকুন।")),
        T(listOf("row", "pull", "pulldown", "chin", "face pull", "reverse fly", "rear delt"), listOf("Pull your shoulder blades together.", "Lead with your elbows.", "Don't shrug your shoulders."),
            listOf("কাঁধের হাড় দুটো পেছনে একসাথে চাপুন।", "কনুই দিয়ে টানুন।", "কাঁধ কানের দিকে তুলবেন না।")),
        T(listOf("curl"), listOf("Keep your elbows pinned to your sides.", "Lower slowly, don't swing.", "Squeeze at the top."),
            listOf("কনুই শরীরের পাশে স্থির রাখুন।", "ধীরে নামান, দোলাবেন না।", "ওপরে উঠে পেশি চেপে ধরুন।")),
        T(listOf("tricep", "dip", "skull", "kickback", "pushdown", "extension"), listOf("Only your elbows move.", "Fully straighten your arms.", "Control the way back."),
            listOf("শুধু কনুই নড়বে, বাকি সব স্থির।", "হাত পুরোপুরি সোজা করুন।", "ফেরার সময় নিয়ন্ত্রণ রাখুন।")),
        T(listOf("press", "raise", "fly", "flye"), listOf("Keep your core tight; don't arch your back.", "Move with control, up and down.", "Shoulders down, away from your ears."),
            listOf("পেট শক্ত রাখুন, পিঠ বাঁকাবেন না।", "ওঠা-নামা দুটোই নিয়ন্ত্রণে রাখুন।", "কাঁধ নিচে রাখুন, কানের দিকে তুলবেন না।")),
        T(listOf("calf"), listOf("Rise all the way up and pause.", "Lower slowly for a full stretch."),
            listOf("পুরোপুরি পায়ের আঙুলে উঠে একটু থামুন।", "ধীরে নামুন, পুরো টান অনুভব করুন।")),
        T(listOf("stretch", "pose", "circle", "rotation", "mobility"), listOf("Relax and breathe slowly.", "Go only as far as feels comfortable.", "No bouncing; hold it gently."),
            listOf("শরীর ঢিলে রাখুন, ধীরে শ্বাস নিন।", "যতটুকু আরামদায়ক, ততটুকুই করুন।", "ঝাঁকি দেবেন না, আলতো করে ধরে রাখুন।"))
    )

    private val GENERAL_EN = listOf("Focus on good form over speed.", "Breathe out on the effort.", "Keep your movements controlled.")
    private val GENERAL_BN = listOf("গতির চেয়ে সঠিক ভঙ্গি বেশি জরুরি।", "জোর দেওয়ার সময় শ্বাস ছাড়ুন।", "নড়াচড়া নিয়ন্ত্রণে রাখুন।")

    fun forExercise(ex: CoachExercise, lang: CoachLanguage, rnd: Random = Random): String {
        val n = (ex.name + " " + ex.pattern).lowercase()
        val t = TABLE.firstOrNull { row -> row.keys.any { it in n } }
        val pool = when {
            t == null -> if (lang == CoachLanguage.BANGLA) GENERAL_BN else GENERAL_EN
            lang == CoachLanguage.BANGLA -> t.bn
            else -> t.en
        }
        return pool.random(rnd)
    }
}

/** Bangla-script names for exercises, built word by word (Push-Up → পুশ-আপ, Dumbbell → ডাম্বেল). */
object BanglaNames {
    private val WORDS: Map<String, String> = mapOf(
        "dumbbell" to "ডাম্বেল", "dumbbells" to "ডাম্বেল", "db" to "ডাম্বেল", "press" to "প্রেস", "presses" to "প্রেস", "barbell" to "বারবেল",
        "cable" to "ক্যাবল", "band" to "ব্যান্ড", "bands" to "ব্যান্ড", "kettlebell" to "কেটলবেল", "kettlebells" to "কেটলবেল",
        "curl" to "কার্ল", "curls" to "কার্ল", "machine" to "মেশিন", "standing" to "দাঁড়িয়ে", "squat" to "স্কোয়াট", "squats" to "স্কোয়াট",
        "arm" to "হাত", "arms" to "হাত", "up" to "আপ", "ups" to "আপ", "raise" to "রেইজ", "raises" to "রেইজ", "bench" to "বেঞ্চ", "leg" to "পা", "legs" to "পা",
        "row" to "রো", "rows" to "রো", "rowing" to "রোয়িং", "seated" to "বসে", "one" to "এক", "stretch" to "স্ট্রেচ", "triceps" to "ট্রাইসেপস", "tricep" to "ট্রাইসেপ",
        "incline" to "ইনক্লাইন", "reverse" to "রিভার্স", "with" to "সহ", "extension" to "এক্সটেনশন", "extensions" to "এক্সটেনশন", "smith" to "স্মিথ",
        "grip" to "গ্রিপ", "deadlift" to "ডেডলিফট", "deadlifts" to "ডেডলিফট", "side" to "পাশ", "sides" to "পাশ", "overhead" to "ওভারহেড", "push" to "পুশ",
        "hip" to "হিপ", "bent" to "বেন্ট", "lunge" to "লাঞ্জ", "lunges" to "লাঞ্জ", "over" to "ওভার", "lying" to "শুয়ে", "calf" to "কাফ", "calves" to "কাফ",
        "single" to "সিঙ্গেল", "crunch" to "ক্রাঞ্চ", "crunches" to "ক্রাঞ্চ", "chest" to "চেস্ট", "fly" to "ফ্লাই", "flye" to "ফ্লাই", "flyes" to "ফ্লাই",
        "clean" to "ক্লিন", "pull" to "পুল", "front" to "ফ্রন্ট", "glute" to "গ্লুট", "wrist" to "রিস্ট", "twist" to "টুইস্ট", "twists" to "টুইস্ট",
        "close" to "ক্লোজ", "decline" to "ডিক্লাইন", "on" to "অন", "lateral" to "ল্যাটারাল", "back" to "ব্যাক", "split" to "স্প্লিট", "jump" to "জাম্প",
        "jumping" to "জাম্পিং", "bar" to "বার", "rotation" to "রোটেশন", "rotations" to "রোটেশন", "the" to "", "and" to "ও", "wide" to "ওয়াইড",
        "floor" to "ফ্লোর", "knee" to "হাঁটু", "knees" to "হাঁটু", "rear" to "রিয়ার", "pulldown" to "পুলডাউন", "pulldowns" to "পুলডাউন",
        "shoulder" to "শোল্ডার", "snatch" to "স্ন্যাচ", "ball" to "বল", "to" to "টু", "bridge" to "ব্রিজ", "delt" to "ডেল্ট", "box" to "বক্স",
        "kneeling" to "হাঁটু গেড়ে", "shrug" to "শ্রাগ", "two" to "দুই", "palms" to "তালু", "palm" to "তালু", "lat" to "ল্যাট", "plate" to "প্লেট",
        "pullover" to "পুলওভার", "sumo" to "সুমো", "down" to "ডাউন", "sit" to "সিট", "hamstring" to "হ্যামস্ট্রিং", "alternating" to "পালাক্রমে",
        "alternate" to "পালাক্রমে", "bicep" to "বাইসেপ", "biceps" to "বাইসেপস", "kickback" to "কিকব্যাক", "high" to "হাই", "neck" to "ঘাড়",
        "step" to "স্টেপ", "jerk" to "জার্ক", "straight" to "সোজা", "hammer" to "হ্যামার", "hang" to "হ্যাং", "hanging" to "ঝুলে", "backward" to "পেছনে",
        "good" to "গুড", "morning" to "মর্নিং", "mornings" to "মর্নিং", "thrust" to "থ্রাস্ট", "preacher" to "প্রিচার", "dip" to "ডিপ", "dips" to "ডিপ",
        "power" to "পাওয়ার", "stiff" to "স্টিফ", "circles" to "সার্কেল", "circle" to "সার্কেল", "rope" to "রোপ", "behind" to "পেছনে", "sled" to "স্লেড",
        "concentration" to "কনসেন্ট্রেশন", "upright" to "আপরাইট", "external" to "এক্সটার্নাল", "internal" to "ইন্টার্নাল", "plank" to "প্ল্যাঙ্ক",
        "v" to "ভি", "weighted" to "ওজনসহ", "adductor" to "অ্যাডাক্টর", "abductor" to "অ্যাবডাক্টর", "walking" to "হেঁটে", "low" to "লো",
        "chin" to "চিন", "romanian" to "রোমানিয়ান", "drag" to "ড্র্যাগ", "long" to "লং", "russian" to "রাশিয়ান", "wall" to "দেয়াল",
        "double" to "ডাবল", "carry" to "ক্যারি", "windmill" to "উইন্ডমিল", "windmills" to "উইন্ডমিল", "skull" to "স্কাল", "crusher" to "ক্রাশার",
        "hack" to "হ্যাক", "bend" to "বেন্ড", "bends" to "বেন্ড", "bodyweight" to "বডিওয়েট", "oblique" to "অবলিক", "prone" to "উপুড় হয়ে",
        "flat" to "ফ্ল্যাট", "frog" to "ফ্রগ", "lift" to "লিফট", "supine" to "চিত হয়ে", "torso" to "শরীর", "swing" to "সুইং", "swings" to "সুইং",
        "bulgarian" to "বুলগেরিয়ান", "arnold" to "আর্নল্ড", "around" to "চারপাশে", "assisted" to "সহায়তায়", "through" to "থ্রু", "body" to "বডি",
        "butt" to "বাট", "chair" to "চেয়ার", "cuban" to "কিউবান", "kick" to "কিক", "kicks" to "কিক", "face" to "ফেস", "goblet" to "গবলেট",
        "isometric" to "আইসোমেট্রিক", "pistol" to "পিস্তল", "turkish" to "টার্কিশ", "get" to "গেট", "world" to "ওয়ার্ল্ড", "twisting" to "টুইস্টিং",
        "curtsey" to "কার্টসি", "ab" to "অ্যাব", "wheel" to "হুইল", "rollout" to "রোলআউট", "full" to "ফুল", "cross" to "ক্রস", "crossbody" to "ক্রসবডি",
        "lower" to "নিচের", "upper" to "ওপরের", "donkey" to "ডংকি", "elbow" to "কনুই", "elbows" to "কনুই", "farmer's" to "ফার্মার্স", "pike" to "পাইক",
        "inverted" to "ইনভার্টেড", "wipers" to "ওয়াইপার", "thruster" to "থ্রাস্টার", "muscle" to "মাসল", "tuck" to "টাক", "pallof" to "প্যালফ",
        "rack" to "র‍্যাক", "woodchop" to "উডচপ", "bicycle" to "সাইকেল", "renegade" to "রেনেগেড", "mountain" to "মাউন্টেন", "climber" to "ক্লাইম্বার",
        "climbers" to "ক্লাইম্বার", "military" to "মিলিটারি", "pushups" to "পুশ-আপ", "superman" to "সুপারম্যান", "zercher" to "জার্চার",
        "suitcase" to "স্যুটকেস", "heel" to "গোড়ালি", "touch" to "টাচ", "touches" to "টাচ", "touchers" to "টাচার্স", "burpee" to "বার্পি",
        "burpees" to "বার্পি", "jack" to "জ্যাক", "jacks" to "জ্যাক", "skips" to "স্কিপ", "skip" to "স্কিপ", "skipping" to "স্কিপিং", "bird" to "বার্ড",
        "dog" to "ডগ", "boxer" to "বক্সার", "shuffle" to "শাফল", "fire" to "ফায়ার", "hydrant" to "হাইড্র্যান্ট", "pendlay" to "পেন্ডলে",
        "away" to "অ্যাওয়ে", "prayer" to "প্রেয়ার", "doorway" to "দরজায়", "under" to "আন্ডার", "spiderman" to "স্পাইডারম্যান", "pulse" to "পালস",
        "backhand" to "ব্যাকহ্যান্ড", "cobra" to "কোবরা", "punch" to "পাঞ্চ", "slams" to "স্ল্যাম", "slam" to "স্ল্যাম", "toes" to "পায়ের আঙুল",
        "toe" to "পায়ের আঙুল", "feet" to "পা", "foot" to "পা", "head" to "মাথা", "hands" to "হাত", "hand" to "হাত", "elevated" to "উঁচুতে",
        "diamond" to "ডায়মন্ড", "spider" to "স্পাইডার", "crawl" to "ক্রল", "star" to "স্টার", "inchworm" to "ইঞ্চওয়ার্ম", "child's" to "চাইল্ডস",
        "pose" to "পোজ", "cat" to "ক্যাট", "quad" to "কোয়াড", "groin" to "কুঁচকি", "spinal" to "মেরুদণ্ডের", "upward" to "ওপরের দিকে",
        "flutter" to "ফ্লাটার", "scissors" to "সিজার্স", "scissor" to "সিজার", "dead" to "ডেড", "bug" to "বাগ", "hug" to "জড়িয়ে ধরুন", "chest's" to "চেস্ট",
        "stability" to "স্ট্যাবিলিটি", "fast" to "দ্রুত", "of" to "", "a" to "", "in" to "ইন", "out" to "আউট", "into" to "থেকে", "from" to "থেকে",
        "gorilla" to "গরিলা", "leverage" to "লিভারেজ", "pec" to "পেক", "deck" to "ডেক", "abduction" to "অ্যাবডাকশন", "adduction" to "অ্যাডাকশন",
        "flexion" to "ফ্লেক্সন", "pass" to "পাস", "thighs" to "উরু", "trap" to "ট্র্যাপ", "mid" to "মিড", "middle" to "মাঝের", "dynamic" to "ডায়নামিক",
        "lumbar" to "কোমর", "skating" to "স্কেটিং", "run" to "দৌড়", "running" to "দৌড়", "sprint" to "স্প্রিন্ট", "walk" to "হাঁটা", "bear" to "বিয়ার",
        "rocking" to "রকিং", "jumps" to "জাম্প", "tuck" to "টাক", "sit-up" to "সিট-আপ", "push-up" to "পুশ-আপ", "pull-up" to "পুল-আপ", "step-up" to "স্টেপ-আপ"
    )

    fun of(ex: CoachExercise): String = of(ex.name)

    fun of(english: String): String {
        val cleaned = english.replace(Regex("[()]"), " ").replace(" - ", " ")
        val parts = Regex("[A-Za-z0-9']+(?:-[A-Za-z]+)?|[^A-Za-z0-9']+").findAll(cleaned).map { it.value }.toList()
        val out = parts.joinToString("") { p ->
            val key = p.lowercase()
            when {
                p.isBlank() || !p.first().isLetterOrDigit() -> p
                key in WORDS -> WORDS.getValue(key)
                key.contains('-') -> key.split('-').joinToString("-") { WORDS[it] ?: it }
                key.all { it.isDigit() } -> CoachLines.num(key.toInt())
                else -> p
            }
        }
        return out.replace(Regex("\\s+"), " ").trim()
    }
}
