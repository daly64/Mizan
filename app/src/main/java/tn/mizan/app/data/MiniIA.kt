package tn.mizan.app.data

import kotlin.math.exp
import kotlin.random.Random

/**
 * Mini IA d'AL MIZAN, codée de zéro (aucune bibliothèque, aucun gros modèle, rien ne sort du téléphone).
 *
 * Elle devine la catégorie d'une dépense à partir de sa note : « taxi », « kahwa », « facture steg »…
 *
 * Comment elle marche :
 *  1. Le texte est découpé en « indices » : les mots, et des morceaux de 3 lettres
 *     (ainsi « taxis » ou « taksi » ressemblent encore à « taxi »).
 *  2. Un petit réseau de neurones à une couche (régression softmax) donne un score par catégorie
 *     pour chaque indice, puis les transforme en probabilités.
 *  3. Elle apprend avec la descente de gradient, sur des exemples de départ
 *     et surtout sur TES dépenses déjà enregistrées : quand tu corriges une catégorie,
 *     la dépense enregistrée devient un exemple, et l'IA se réentraîne (en quelques millisecondes).
 */
object MiniIA {

    private val categories = Category.entries
    private val K = categories.size

    /** Exemples de départ (français + darija en lettres latines), avant que l'IA connaisse tes habitudes. */
    private val seed: Map<Category, List<String>> = mapOf(
        Category.COURSES to listOf(
            "courses", "carrefour", "monoprix", "aziza", "magasin general", "geant", "promogros", "epicerie",
            "superette", "supermarche", "hyper", "marche", "souk", "boucherie", "poulet", "viande", "poisson",
            "legumes", "fruits", "primeur", "pain", "khobz", "baguette", "boulangerie", "patisserie", "lait", "lben",
            "oeufs", "huile", "sucre", "farine", "cafe en grains", "eau", "pharmacie", "medicament", "parapharmacie",
            "savon", "shampoing", "couches", "hamout", "attar", "fromage", "yaourt", "pates", "riz", "harissa"
        ),
        Category.TRANSPORT to listOf(
            "taxi", "taksi", "louage", "bus", "metro", "train", "sncft", "transtu", "essence", "carburant", "gasoil",
            "sans plomb", "station", "agil", "shell", "total", "ola energy", "star oil", "peage", "autoroute", "parking",
            "garage", "mecanicien", "vidange", "pneu", "lavage voiture", "uber", "bolt", "indrive", "ticket bus", "moto",
            "velo", "assurance voiture", "vignette", "visite technique"
        ),
        Category.LOISIRS to listOf(
            "cafe", "kahwa", "express", "capucin", "the", "salon de the", "restaurant", "resto", "pizza", "sandwich",
            "kaskrout", "mlawi", "chapati", "makloub", "fast food", "snack", "glace", "creperie", "cinema", "concert",
            "sortie", "cadeau", "anniversaire", "sport", "salle de sport", "foot", "jeu", "playstation", "netflix",
            "livre", "librairie", "voyage", "hotel", "plage", "chicha", "coiffeur", "vetements", "chaussures"
        ),
        Category.FACTURES to listOf(
            "facture", "steg", "electricite", "dhaw", "gaz", "sonede", "eau facture", "loyer", "kra", "internet",
            "topnet", "globalnet", "hexabyte", "ooredoo", "orange", "tunisie telecom", "telephone", "recharge",
            "abonnement", "assurance", "banque", "frais bancaires", "credit", "impots", "syndic", "ecole",
            "inscription", "creche", "mutuelle"
        )
    )

    /** Mots qui ne disent rien sur la catégorie. */
    private val stopWords = setOf(
        "dinar", "dinars", "dt", "tnd", "d", "millimes", "millime", "mill", "de", "du", "des", "le", "la", "les",
        "un", "une", "et", "pour", "au", "aux", "a", "en", "avec", "chez", "mon", "ma", "mes", "l", "fi", "w"
    )

    /** Un modèle entraîné : un poids par (indice, catégorie), et un biais par catégorie. */
    class Model internal constructor(
        private val weights: Map<String, FloatArray>,
        private val bias: FloatArray,
        /** Nombre de tes dépenses avec note qui ont servi à l'entraînement. */
        val personalExamples: Int
    ) {
        /** Probabilité de chaque catégorie, ou null si l'IA ne reconnaît aucun indice dans le texte. */
        fun probabilities(text: String): Map<Category, Float>? {
            val known = features(text).filter { it in weights }
            if (known.isEmpty()) return null
            val scores = FloatArray(K) { bias[it] }
            known.forEach { f -> val w = weights.getValue(f); for (k in 0 until K) scores[k] += w[k] }
            val p = softmax(scores)
            return categories.withIndex().associate { (i, c) -> c to p[i] }
        }

        /** La catégorie la plus probable, si l'IA est assez sûre d'elle. */
        fun guess(text: String, minConfidence: Float = 0.45f): Guess? {
            val p = probabilities(text) ?: return null
            val best = p.maxByOrNull { it.value } ?: return null
            return if (best.value >= minConfidence) Guess(best.key, best.value) else null
        }
    }

    data class Guess(val category: Category, val confidence: Float) {
        val percent: Int get() = (confidence * 100).toInt()
    }

    /** Modèle de départ (sans tes données), prêt immédiatement. */
    val starter: Model by lazy { train(emptyList()) }

    /**
     * Entraîne la mini IA. Tes dépenses (avec une note) comptent 3 fois plus que les exemples de départ,
     * et les plus récentes passent en premier (500 au maximum).
     */
    fun train(expenses: List<Expense>, epochs: Int = 25, learningRate: Float = 0.3f): Model {
        val examples = ArrayList<Pair<List<String>, Int>>()
        seed.forEach { (category, words) ->
            words.forEach { w -> examples += features(w) to category.ordinal }
        }
        val personal = expenses.asSequence()
            .filter { !it.isIncome && it.note.isNotBlank() }
            .sortedByDescending { it.dateMillis }
            .take(500)
            .map { features(it.note) to it.category.ordinal }
            .filter { it.first.isNotEmpty() }
            .toList()
        repeat(3) { examples += personal }

        val weights = HashMap<String, FloatArray>()
        examples.forEach { (fs, _) -> fs.forEach { f -> weights.getOrPut(f) { FloatArray(K) } } }
        val bias = FloatArray(K)
        val random = Random(42) // même résultat à chaque entraînement
        val decay = 0.0005f     // petite régularisation : évite que l'IA soit trop sûre d'elle

        repeat(epochs) { epoch ->
            val lr = learningRate / (1 + epoch * 0.1f)
            examples.shuffle(random)
            examples.forEach { (fs, target) ->
                val scores = FloatArray(K) { bias[it] }
                fs.forEach { f -> val w = weights.getValue(f); for (k in 0 until K) scores[k] += w[k] }
                val p = softmax(scores)
                // Gradient de l'erreur (cross-entropy) : probabilité prédite - bonne réponse.
                for (k in 0 until K) {
                    val g = p[k] - if (k == target) 1f else 0f
                    bias[k] -= lr * g * 0.1f
                    fs.forEach { f -> val w = weights.getValue(f); w[k] -= lr * (g + decay * w[k]) }
                }
            }
        }
        return Model(weights, bias, personal.size)
    }

    /** Les indices d'un texte : ses mots, et pour chaque mot ses morceaux de 3 lettres. */
    fun features(text: String): List<String> {
        val words = TicketParser.normalize(text).lowercase()
            .split(Regex("[^a-z]+"))
            .filter { it.length >= 2 && it !in stopWords }
        val out = ArrayList<String>()
        words.forEach { w ->
            out += "w:$w"
            val padded = "<$w>"
            for (i in 0..padded.length - 3) out += "g:" + padded.substring(i, i + 3)
        }
        words.zipWithNext().forEach { (a, b) -> out += "b:$a $b" } // paires de mots : « salon de the »
        return out
    }

    private fun softmax(scores: FloatArray): FloatArray {
        val max = scores.max()
        val e = FloatArray(scores.size) { exp((scores[it] - max).toDouble()).toFloat() }
        val sum = e.sum()
        for (i in e.indices) e[i] /= sum
        return e
    }

    // ------------------------------------------------------------------ Saisie rapide

    /** Ce que la saisie rapide a compris : « taxi 7.5 » → 7,500 dinars, note « Taxi », Transport. */
    data class QuickEntry(val amountMillimes: Long?, val note: String, val guess: Guess?)

    private val millimesPattern = Regex("(\\d+)\\s*(?:millimes?|mill|mil|m)\\b", RegexOption.IGNORE_CASE)
    private val amountPattern = Regex("(?<![\\d])(\\d{1,3}(?:[ \\u00A0]\\d{3})+|\\d+)(?:[.,](\\d{1,3}))?(?![\\d])")
    private val currencyWords = Regex("\\b(dinars?|dt|tnd|d)\\b\\.?", RegexOption.IGNORE_CASE)

    /** Comprend une phrase libre : « 20 dinars taxi », « kahwa 2.8 », « steg 85,500 », « pain 600 millimes ». */
    fun quick(text: String, model: Model): QuickEntry {
        var rest = text.trim()
        var amount: Long? = null

        millimesPattern.find(rest)?.let { m ->
            amount = m.groupValues[1].toLongOrNull()
            rest = rest.removeRange(m.range)
        }
        if (amount == null) {
            amountPattern.find(rest)?.let { m ->
                val dinars = m.groupValues[1].replace(Regex("[ \\u00A0]"), "").toLongOrNull()
                if (dinars != null) {
                    val decimals = m.groupValues[2]
                    val millimes = if (decimals.isEmpty()) 0L else decimals.padEnd(3, '0').toLong()
                    amount = dinars * 1000 + millimes
                    rest = rest.removeRange(m.range)
                }
            }
        }
        val note = rest.replace(currencyWords, " ").replace(Regex("\\s+"), " ").trim()
            .replaceFirstChar { it.uppercase() }
        return QuickEntry(amount?.takeIf { it > 0 }, note, if (note.isBlank()) null else model.guess(note))
    }

    /** Pour afficher un pourcentage d'assurance lisible (évite « 100 % » trop affirmatif). */
    fun confidenceLabel(guess: Guess): String = "${guess.percent.coerceAtMost(99)} %"
}
