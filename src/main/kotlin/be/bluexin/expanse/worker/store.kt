package be.bluexin.expanse.worker

import io.aeron.Aeron
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.map
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.coroutineContext

object ComponentStore {
    private val entities: Int2ReferenceMap<Component<*>> = Int2ReferenceOpenHashMap()

    operator fun get(componentId: Int): Component<*>? = entities[componentId]

    operator fun plusAssign(component: Component<*>) {
        @Suppress("ReplacePutWithAssignment") // would introduce boxing
        val old = entities.put(component.id, component)
        if (old != entities.defaultReturnValue()) logger.warn { "Component $component replaced $old in the store !" }
    }
}

object EntityStore {
    private lateinit var writer: SendChannel<Update<*>>
    private lateinit var connection: Job
    private lateinit var aeron: Aeron

    private val entities: Int2ReferenceMap<Entity> = Int2ReferenceOpenHashMap()

    operator fun get(entityId: Int): Entity = entities.computeIfAbsentPartial(entityId) { Entity(it) }

    operator fun plusAssign(entity: Entity) {
        @Suppress("ReplacePutWithAssignment") // would introduce boxing
        entities.put(entity.id, entity)
    }

    operator fun plusAssign(update: Update<*>) {
        writer.offer(update)
    }

    @ExperimentalCoroutinesApi
    fun connectAeronFuture() = CompletableFuture<Unit>().also { GlobalScope.launch { connectAeron(it) } }

    @UseExperimental(ObsoleteCoroutinesApi::class)
    @ExperimentalCoroutinesApi
    suspend fun connectAeron(future: CompletableFuture<Unit>? = null) {
        this.aeron = Aeron.connect(
            Aeron.Context()
                .errorHandler { logger.warn(it) { "Aeron error" } }
                .availableImageHandler { logger.info { "Aeron is available" } }
                .unavailableImageHandler { logger.info { "Aeron went down" } }
        )
        this.connection = Job()
        with(CoroutineScope(coroutineContext + connection)) {
            val channel = Channel<Update<*>>(Channel.UNLIMITED)
            this@EntityStore.writer = channel
            logger.info { "Set writer" }
            launch(Dispatchers.IO) {
                for (update in aeronConsumer(aeron).mapUpdates()) {
                    logger.info { "Read $update" }
                    this@EntityStore[update.entityId].load(
                        update.component,
                        update.value
                    )
                }
            }
            aeronProducer(
                aeron,
                channel.map { it.apply { logger.info { "Sending $it" } } }.mapBytes().map {
                    it.apply {
                        logger.info {
                            "(serialized as ${String(it)})"
                        }
                    }
                }
            )
            Unit
        }
        future?.complete(Unit)
    }

    @ExperimentalCoroutinesApi
    fun disconnectAeronBlocking() = runBlocking { disconnectAeron() }

    suspend fun disconnectAeron() {
        logger.info { "Disconnecting..." }
        connection.cancelAndJoin()
        logger.info { "Closed connection" }
        aeron.close()
    }
}

private val logger = KotlinLogging.logger("Store")
