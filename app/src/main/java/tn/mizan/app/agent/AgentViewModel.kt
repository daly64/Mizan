package tn.mizan.app.agent

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.tool
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tn.mizan.app.MizanApplication
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executors

/** Où en est le modèle sur le téléphone. */
sealed interface ModelState {
    data object Missing : ModelState
    data class Importing(val progress: Float) : ModelState
    data object Loading : ModelState
    data object Ready : ModelState
    data class Failed(val message: String) : ModelState
}

enum class Who { USER, AGENT, ACTION }

data class ChatItem(val id: Long, val who: Who, val text: String)

data class AssistantState(
    val model: ModelState = ModelState.Missing,
    val messages: List<ChatItem> = emptyList(),
    val busy: Boolean = false,
    val gpu: Boolean = false
)

/**
 * L'assistant de Mizan : un petit modèle (Qwen3 0.6B ou Gemma 4 E2B) tourne sur le téléphone avec LiteRT-LM.
 * Rien ne sort du téléphone (Mizan n'a pas la permission internet).
 * Le moteur reste chargé tant que l'app est ouverte, pour ne pas recharger le modèle à chaque visite.
 */
class AgentViewModel(app: Application) : AndroidViewModel(app) {

    private val context get() = getApplication<Application>()
    private val dao = (app as MizanApplication).database.dao()

    /** Un seul fil pour le moteur : LiteRT-LM n'aime pas les appels concurrents. */
    private val engineExecutor = Executors.newSingleThreadExecutor()
    private val engineThread = engineExecutor.asCoroutineDispatcher()

    private val tools = MizanTools(dao) { action -> addItem(Who.ACTION, action) }
    private val quick = QuickCommands(tools)

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var sendJob: Job? = null
    private var nextId = 0L

    private val _state = MutableStateFlow(
        AssistantState(
            model = if (ModelStore.hasModel(app)) ModelState.Loading else ModelState.Missing,
            gpu = ModelStore.useGpu(app)
        )
    )
    val state: StateFlow<AssistantState> = _state.asStateFlow()

    init {
        if (ModelStore.hasModel(app)) loadEngine()
    }

    // --- Modèle ---

    fun importModel(uri: Uri) {
        val name = ModelStore.displayName(context, uri) ?: ""
        if (!name.endsWith(".litertlm")) {
            _state.update { it.copy(model = ModelState.Failed("Ce fichier n'est pas un modèle (.litertlm) : « $name ».")) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(model = ModelState.Importing(0f)) }
            runCatching {
                closeEngine()
                ModelStore.import(context, uri, name) { p -> _state.update { it.copy(model = ModelState.Importing(p)) } }
            }.onSuccess { loadEngine() }
                .onFailure { e -> _state.update { it.copy(model = ModelState.Failed("Import impossible : ${e.message}")) } }
        }
    }

    fun deleteModel() {
        viewModelScope.launch {
            closeEngine()
            ModelStore.delete(context)
            _state.update { AssistantState(model = ModelState.Missing, gpu = it.gpu) }
        }
    }

    fun setGpu(value: Boolean) {
        ModelStore.setUseGpu(context, value)
        _state.update { it.copy(gpu = value) }
        if (ModelStore.hasModel(context)) loadEngine()
    }

    fun retry() {
        if (ModelStore.hasModel(context)) loadEngine() else _state.update { it.copy(model = ModelState.Missing) }
    }

    private fun loadEngine() {
        viewModelScope.launch {
            _state.update { it.copy(model = ModelState.Loading) }
            val wantGpu = _state.value.gpu
            val result = withContext(engineThread) {
                closeEngineNow()
                runCatching { startEngine(wantGpu) }.recoverCatching { e ->
                    // Le GPU (Mali) peut refuser le modèle : on retombe sur le processeur.
                    if (!wantGpu) throw e
                    startEngine(gpu = false)
                    ModelStore.setUseGpu(context, false)
                }
            }
            result.onSuccess {
                _state.update { it.copy(model = ModelState.Ready, gpu = ModelStore.useGpu(context)) }
            }.onFailure { e ->
                _state.update { it.copy(model = ModelState.Failed("Le modèle n'a pas pu se charger : ${e.message}")) }
            }
        }
    }

    /** À appeler sur [engineThread]. */
    private fun startEngine(gpu: Boolean) {
        val cache = File(context.cacheDir, "litertlm").apply { mkdirs() }
        val e = Engine(
            EngineConfig(
                modelPath = ModelStore.modelFile(context).absolutePath,
                backend = if (gpu) Backend.GPU() else Backend.CPU(threadCount = 4),
                maxNumTokens = if (ModelStore.isGemma4(context)) 4096 else null,
                cacheDir = cache.absolutePath
            )
        )
        try {
            e.initialize()
            engine = e
            conversation = newConversation(e)
        } catch (t: Throwable) {
            runCatching { e.close() }
            throw t
        }
    }

    private fun newConversation(e: Engine): Conversation = e.createConversation(
        ConversationConfig(
            systemInstruction = Contents.of(systemPrompt()),
            tools = listOf(tool(tools)),
            samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.3),
            // Les consignes et la liste des outils sont lues dès le chargement, pas au premier message.
            prefillPrefaceOnInit = true,
            // Réponses courtes : sur un téléphone d'entrée de gamme, chaque mot compte.
            maxOutputToken = 400
        )
    )

    private fun systemPrompt(): String {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH))
        return """
            Tu es l'assistant de Mizan, une app de suivi des dépenses personnelles en dinars tunisiens (DT).
            Nous sommes le $today. Réponds en français, en phrases courtes.
            Règles :
            - Pour enregistrer une dépense, un revenu ou une note, appelle l'outil correspondant.
            - Ne calcule jamais de tête : utilise l'outil calculer pour toute opération.
            - Pour un bilan ou des conseils pour économiser, appelle d'abord bilanDuMois et appuie-toi uniquement sur ses chiffres. Donne ensuite 2 ou 3 actions concrètes.
            - N'invente jamais de montant.
        """.trimIndent() + if (ModelStore.isQwen(context)) "\n/no_think" else ""
    }

    // --- Conversation ---

    fun send(text: String) {
        val message = text.trim()
        if (message.isEmpty() || _state.value.busy) return
        addItem(Who.USER, message)
        _state.update { it.copy(busy = true) }

        // Mode rapide : réponse immédiate sans le modèle pour les demandes simples.
        viewModelScope.launch {
            val direct = withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { quick.handle(message) }.getOrNull() }
            if (direct != null) {
                if (direct.isNotEmpty()) addItem(Who.AGENT, direct)
                _state.update { it.copy(busy = false) }
            } else if (_state.value.model != ModelState.Ready) {
                addItem(Who.AGENT, "Je n'ai compris que les demandes simples sans le modèle (ex. « 20 dinars taxi », « note : … », « bilan », « 12*3 »). Pour le reste, il faut que le modèle soit chargé.")
                _state.update { it.copy(busy = false) }
            } else {
                askModel(message)
            }
        }
    }

    private fun askModel(message: String) {
        sendJob = viewModelScope.launch {
            var replyId: Long? = null
            val reply = StringBuilder()
            runCatching {
                withContext(engineThread) {
                    val conv = conversation ?: error("modèle non chargé")
                    conv.sendMessageAsync(Contents.of(message)).collect { chunk ->
                        val piece = textOf(chunk)
                        if (piece.isEmpty()) return@collect
                        reply.append(piece)
                        val visible = withoutThinking(reply.toString())
                        if (visible.isEmpty()) return@collect // il réfléchit encore : on garde « L'assistant réfléchit… »
                        val id = replyId ?: addItem(Who.AGENT, "").also { replyId = it }
                        updateItem(id, visible)
                    }
                }
            }.onFailure { e ->
                if (e !is kotlinx.coroutines.CancellationException) {
                    addItem(Who.ACTION, "Erreur : ${e.message ?: e.javaClass.simpleName}")
                }
            }
            _state.update { it.copy(busy = false) }
        }
    }

    /** Arrête la réponse en cours. */
    fun stop() {
        runCatching { conversation?.cancelProcess() }
        sendJob?.cancel()
        _state.update { it.copy(busy = false) }
    }

    /** Efface la discussion (les dépenses et notes enregistrées restent). */
    fun newChat() {
        if (_state.value.busy) stop()
        viewModelScope.launch {
            withContext(engineThread) {
                runCatching { conversation?.close() }
                conversation = engine?.let { newConversation(it) }
            }
            _state.update { it.copy(messages = emptyList()) }
        }
    }

    /** Retire les passages <think>…</think> (réflexion interne de Qwen3), même s'ils ne sont pas finis. */
    private fun withoutThinking(text: String): String {
        val closed = text.replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "")
        val open = closed.indexOf("<think>")
        return (if (open >= 0) closed.substring(0, open) else closed).trim()
    }

    private fun textOf(message: Message): String =
        message.contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }

    private fun addItem(who: Who, text: String): Long {
        val id = nextId++
        _state.update { it.copy(messages = it.messages + ChatItem(id, who, text)) }
        return id
    }

    private fun updateItem(id: Long, text: String) {
        _state.update { s -> s.copy(messages = s.messages.map { if (it.id == id) it.copy(text = text) else it }) }
    }

    private suspend fun closeEngine() = withContext(engineThread) { closeEngineNow() }

    private fun closeEngineNow() {
        runCatching { conversation?.close() }
        runCatching { engine?.close() }
        conversation = null
        engine = null
    }

    override fun onCleared() {
        runCatching { conversation?.cancelProcess() }
        engineExecutor.execute { closeEngineNow() }
        engineExecutor.shutdown()
    }
}
