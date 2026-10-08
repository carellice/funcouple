package com.funcouple.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.random.Random

/** Carte speciali che ogni tanto escono dal mazzo di Verità o Obbligo. */
enum class CardModifier(val label: String, val icon: FcIcon) {
    NONE("", FcIcon.STAR),
    DOUBLE("DOPPIO", FcIcon.STAR),
    SWAP("SCAMBIO", FcIcon.SWAP),
    TOGETHER("INSIEME", FcIcon.HEART),
}

/** Carta scritta da uno dei due partner. */
data class CustomCard(val id: Long, val author: Int, val level: Int, val truth: Boolean, val text: String)

/** In Escalation si sale di livello ogni tante carte giocate. */
const val ESCALATION_STEP = 6

/** Stato persistente dell'app: tutto resta solo sul dispositivo. */
class AppState(context: Context) {
    private val prefs = context.getSharedPreferences("funcouple", Context.MODE_PRIVATE)

    var onboarded by mutableStateOf(prefs.getBoolean("onboarded", false))
        private set
    var name1 by mutableStateOf(prefs.getString("name1", "") ?: "")
        private set
    var name2 by mutableStateOf(prefs.getString("name2", "") ?: "")
        private set
    var level by mutableIntStateOf(prefs.getInt("level", 0))
        private set
    var favorites by mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet())
        private set
    var tried by mutableStateOf(prefs.getStringSet("tried", emptySet())?.toSet() ?: emptySet())
        private set
    var marathons by mutableIntStateOf(prefs.getInt("marathons", 0))
        private set

    fun name(player: Int) = if (player == 0) name1 else name2

    fun saveNames(first: String, second: String) {
        name1 = first.trim()
        name2 = second.trim()
        onboarded = true
        prefs.edit().putString("name1", name1).putString("name2", name2).putBoolean("onboarded", true).apply()
    }

    /** Riapre l'introduzione senza toccare i dati salvati. */
    fun replayOnboarding() {
        onboarded = false
    }

    // --- Preferenze e limiti -------------------------------------------------------------

    var limits by mutableStateOf(
        prefs.getStringSet("limits", emptySet()).orEmpty().mapNotNull { n -> Limit.entries.find { it.name == n } }.toSet(),
    )
        private set

    /** Vero se il testo non tocca nessuna delle categorie escluse dalla coppia. */
    fun allows(text: String) = limits.none { it.matches(text) }

    fun toggleLimit(limit: Limit) {
        limits = if (limit in limits) limits - limit else limits + limit
        prefs.edit().putStringSet("limits", limits.map { it.name }.toSet()).apply()
        todDecks.clear()
        neverDecks.clear()
    }

    // --- Verità o Obbligo ----------------------------------------------------------------
    // La sessione resta salvata finché non si avvia una nuova partita.

    var todScores by mutableStateOf(listOf(prefs.getInt("tod_score1", 0), prefs.getInt("tod_score2", 0)))
        private set
    var todTurn by mutableIntStateOf(prefs.getInt("tod_turn", 0))
        private set
    var todPlayed by mutableIntStateOf(prefs.getInt("tod_played", 0))
        private set
    var escalation by mutableStateOf(prefs.getBoolean("tod_escalation", false))
        private set
    var todCurrent by mutableStateOf<Challenge?>(null)
        private set
    var todIsTruth by mutableStateOf(true)
        private set
    var todModifier by mutableStateOf(CardModifier.NONE)
        private set
    var todPenalty by mutableStateOf<String?>(null)
        private set
    private val todDecks = mutableMapOf<String, ArrayDeque<Challenge>>()

    val todInProgress get() = todScores.any { it > 0 } || todPlayed > 0 || todCurrent != null

    /** Livello in gioco: quello scelto a mano, oppure quello raggiunto in Escalation. */
    val todLevel get() = if (escalation) (todPlayed / ESCALATION_STEP).coerceAtMost(levels.lastIndex) else level

    fun updateLevel(value: Int) {
        level = value
        prefs.edit().putInt("level", value).apply()
    }

    fun updateEscalation(value: Boolean) {
        escalation = value
        prefs.edit().putBoolean("tod_escalation", value).apply()
    }

    private fun cardsFor(level: Int, truth: Boolean): List<Challenge> {
        val builtIn = (if (truth) truths else dares)[level]
        val custom = customCards.filter { it.level == level && it.truth == truth }.map { Challenge(it.text, custom = true) }
        return (builtIn + custom).filter { allows(it.text) }.ifEmpty { builtIn }
    }

    fun todDraw(truth: Boolean) {
        val level = todLevel
        val deck = todDecks.getOrPut("$level-$truth") { ArrayDeque() }
        if (deck.isEmpty()) deck.addAll(cardsFor(level, truth).shuffled())
        todIsTruth = truth
        todCurrent = deck.removeFirst()
        todPenalty = null
        todModifier = if (Random.nextInt(100) < 18) {
            listOf(CardModifier.DOUBLE, CardModifier.SWAP, CardModifier.TOGETHER).random()
        } else {
            CardModifier.NONE
        }
    }

    /** Sfida completata: i punti dipendono dalla carta speciale e finiscono anche nel portafoglio. */
    fun todComplete() {
        val gains = when (todModifier) {
            CardModifier.DOUBLE -> List(2) { if (it == todTurn) 2 else 0 }
            CardModifier.SWAP -> List(2) { if (it == todTurn) 0 else 1 }
            CardModifier.TOGETHER -> listOf(1, 1)
            CardModifier.NONE -> List(2) { if (it == todTurn) 1 else 0 }
        }
        todScores = todScores.mapIndexed { i, s -> s + gains[i] }
        wallet = wallet.mapIndexed { i, s -> s + gains[i] }
        saveTodSession()
        saveWallet()
    }

    /** Chi passa pesca una penitenza. */
    fun todSkip() {
        todPenalty = penalties.filter { allows(it) }.ifEmpty { penalties }.random()
    }

    fun todEndTurn() {
        todCurrent = null
        todPenalty = null
        todModifier = CardModifier.NONE
        todPlayed++
        todTurn = 1 - todTurn
        saveTodSession()
    }

    fun todNewGame() {
        todScores = listOf(0, 0)
        todTurn = 0
        todPlayed = 0
        todCurrent = null
        todPenalty = null
        todModifier = CardModifier.NONE
        todDecks.clear()
        saveTodSession()
    }

    private fun saveTodSession() {
        prefs.edit()
            .putInt("tod_score1", todScores[0])
            .putInt("tod_score2", todScores[1])
            .putInt("tod_turn", todTurn)
            .putInt("tod_played", todPlayed)
            .apply()
    }

    // --- Buoni del piacere ---------------------------------------------------------------

    /** Punti da spendere in buoni: si guadagnano completando le sfide e non si azzerano con la partita. */
    var wallet by mutableStateOf(listOf(prefs.getInt("wallet1", 0), prefs.getInt("wallet2", 0)))
        private set

    /** Buoni comprati e non ancora usati, per giocatore. */
    var owned by mutableStateOf(List(2) { prefs.getString("owned${it + 1}", "").orEmpty().split(",").filter(String::isNotEmpty) })
        private set

    private fun saveWallet() {
        prefs.edit()
            .putInt("wallet1", wallet[0]).putInt("wallet2", wallet[1])
            .putString("owned1", owned[0].joinToString(",")).putString("owned2", owned[1].joinToString(","))
            .apply()
    }

    fun buyCoupon(player: Int, coupon: Coupon) {
        if (wallet[player] < coupon.cost) return
        wallet = wallet.mapIndexed { i, w -> if (i == player) w - coupon.cost else w }
        owned = owned.mapIndexed { i, list -> if (i == player) list + coupon.id else list }
        saveWallet()
    }

    fun useCoupon(player: Int, id: String) {
        owned = owned.mapIndexed { i, list -> if (i == player) list - id else list }
        saveWallet()
    }

    // --- Carte scritte dalla coppia ------------------------------------------------------

    var customCards by mutableStateOf(loadCustomCards())
        private set

    private fun loadCustomCards(): List<CustomCard> = runCatching {
        val array = JSONArray(prefs.getString("custom_cards", "[]"))
        List(array.length()) {
            val o = array.getJSONObject(it)
            CustomCard(o.getLong("id"), o.getInt("author"), o.getInt("level"), o.getBoolean("truth"), o.getString("text"))
        }
    }.getOrDefault(emptyList())

    private fun saveCustomCards() {
        val array = JSONArray()
        customCards.forEach {
            array.put(
                JSONObject().put("id", it.id).put("author", it.author).put("level", it.level)
                    .put("truth", it.truth).put("text", it.text),
            )
        }
        prefs.edit().putString("custom_cards", array.toString()).apply()
        todDecks.clear()
    }

    fun addCustomCard(author: Int, level: Int, truth: Boolean, text: String) {
        customCards = customCards + CustomCard(System.currentTimeMillis(), author, level, truth, text.trim())
        saveCustomCards()
    }

    fun removeCustomCard(id: Long) {
        customCards = customCards.filterNot { it.id == id }
        saveCustomCards()
    }

    // --- Altri giochi: valgono finché l'app resta aperta ---------------------------------

    var diceTurn by mutableIntStateOf(0)

    /** Il quarto dado ("come") si può togliere dal lancio. */
    var fourthDie by mutableStateOf(prefs.getBoolean("fourth_die", true))
        private set

    fun updateFourthDie(value: Boolean) {
        fourthDie = value
        prefs.edit().putBoolean("fourth_die", value).apply()
    }
    var wheelTurn by mutableIntStateOf(0)

    var neverLevel by mutableIntStateOf(0)
    var neverCurrent by mutableStateOf<String?>(null)
        private set
    var neverCounts by mutableStateOf(listOf(0, 0))
        private set
    private val neverDecks = mutableMapOf<Int, ArrayDeque<String>>()

    fun neverDraw() {
        val deck = neverDecks.getOrPut(neverLevel) { ArrayDeque() }
        if (deck.isEmpty()) deck.addAll(neverStatements[neverLevel].filter { allows(it) }.ifEmpty { neverStatements[neverLevel] }.shuffled())
        neverCurrent = deck.removeFirst()
    }

    /** Registra chi "l'ha fatto" e passa alla frase successiva. */
    fun neverNext(did: List<Boolean>) {
        neverCounts = neverCounts.mapIndexed { i, c -> if (did[i]) c + 1 else c }
        neverDraw()
    }

    fun neverReset() {
        neverCounts = listOf(0, 0)
        neverDecks.clear()
        neverDraw()
    }

    // --- Kamasutra -----------------------------------------------------------------------

    fun toggleFavorite(id: String) {
        favorites = if (id in favorites) favorites - id else favorites + id
        prefs.edit().putStringSet("favorites", favorites).apply()
    }

    fun toggleTried(id: String) {
        tried = if (id in tried) tried - id else tried + id
        prefs.edit().putStringSet("tried", tried).apply()
    }

    fun completeMarathon(ids: List<String>) {
        tried = tried + ids
        marathons++
        prefs.edit().putStringSet("tried", tried).putInt("marathons", marathons).apply()
    }

    fun resetProgress() {
        favorites = emptySet()
        tried = emptySet()
        marathons = 0
        prefs.edit().remove("favorites").remove("tried").remove("marathons").apply()
    }

    // --- Atmosfera e privacy -------------------------------------------------------------

    /** "Luci basse": schermo più scuro e caldo. */
    var dim by mutableStateOf(prefs.getBoolean("dim", false))
        private set

    fun updateDim(value: Boolean) {
        dim = value
        prefs.edit().putBoolean("dim", value).apply()
    }

    /** Effetto di profondità: l'app reagisce all'inclinazione del telefono. */
    val tilt = TiltSensor(context)
    var depth by mutableStateOf(prefs.getBoolean("depth", true))
        private set

    fun updateDepth(value: Boolean) {
        depth = value
        prefs.edit().putBoolean("depth", value).apply()
        if (value) tilt.start() else tilt.stop()
    }

    val updater = Updater(context)

    var soundOn by mutableStateOf(prefs.getBoolean("sound", true))
        private set

    fun updateSound(value: Boolean) {
        soundOn = value
        prefs.edit().putBoolean("sound", value).apply()
    }

    val sounds = Sounds(context, { soundOn }, { dim })

    /** Sblocco con impronta o volto: vale solo insieme al PIN, che resta la riserva. */
    var biometric by mutableStateOf(prefs.getBoolean("biometric", false))
        private set

    fun updateBiometric(value: Boolean) {
        biometric = value
        prefs.edit().putBoolean("biometric", value).apply()
    }

    fun unlockWithBiometric() {
        locked = false
    }

    private var pinHash = prefs.getString("pin", null)
    var hasPin by mutableStateOf(pinHash != null)
        private set
    var locked by mutableStateOf(pinHash != null)
        private set

    private fun hash(pin: String) = MessageDigest.getInstance("SHA-256")
        .digest("funcouple:$pin".toByteArray())
        .joinToString("") { "%02x".format(it) }

    fun checkPin(pin: String) = hash(pin) == pinHash

    fun setPin(pin: String?) {
        pinHash = pin?.let(::hash)
        hasPin = pinHash != null
        prefs.edit().apply { if (pinHash == null) remove("pin") else putString("pin", pinHash) }.apply()
        if (pinHash == null) updateBiometric(false)
    }

    fun unlock(pin: String): Boolean {
        if (checkPin(pin)) locked = false
        return !locked
    }

    /** Chiamato quando l'app va in secondo piano. */
    fun lock() {
        if (hasPin) locked = true
    }
}
