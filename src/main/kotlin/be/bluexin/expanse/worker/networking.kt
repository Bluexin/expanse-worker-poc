package be.bluexin.expanse.worker

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.channels.mapNotNull
import kotlin.random.Random

private val WORKER_ID = Random.nextInt()

@UseExperimental(ObsoleteCoroutinesApi::class)
fun ReceiveChannel<ByteArray>.mapUpdates() : ReceiveChannel<Update<*>> = this.mapNotNull { JSON.readValue<Update<*>?>(it) }
fun ReceiveChannel<Update<*>>.mapBytes() : ReceiveChannel<ByteArray> = this.map { JSON.writeValueAsBytes(it) }

@JsonSerialize(using = UpdateSerializer::class)
@JsonDeserialize(using = UpdateDeserializer::class)
data class Update<T: Value>(
    val entityId: Int,
    val component: Component<T>,
    val value: T?,
    val workerId: Int = WORKER_ID
)

class UpdateSerializer: JsonSerializer<Update<*>>() {
    override fun serialize(value: Update<*>, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeNumberField("workerId", value.workerId)
        gen.writeNumberField("entityId", value.entityId)
        gen.writeNumberField("componentId", value.component.id)
        gen.writeObjectField("value", value.value)
        gen.writeEndObject()
    }
}

class UpdateDeserializer: JsonDeserializer<Update<*>?>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Update<*>? {
        val root = p.codec.readTree<JsonNode>(p)
        val workerId = root.get("workerId").asInt()
        if (workerId == WORKER_ID) return null
        @Suppress("UNCHECKED_CAST")
        val component = ComponentStore[root.get("componentId").asInt()] as? Component<Value> ?: return null
        val entityId = root.get("entityId").asInt()
        val value = JSON.treeToValue(root.get("value"), component.clazz)
        return Update(entityId, component, value, workerId)
    }
}
