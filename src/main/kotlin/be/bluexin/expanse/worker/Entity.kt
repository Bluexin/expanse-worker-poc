package be.bluexin.expanse.worker

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap

data class Entity internal constructor(
    val id: Int,
    private val components: Int2ReferenceMap<Value> = Int2ReferenceOpenHashMap()
) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Value> get(component: Component<T>): T? = components[component.id] as T?

    operator fun <T : Value> set(component: Component<T>, value: T?) {
        this.load(component, value)
        EntityStore += Update(this.id, component, value)
    }

    internal fun <T : Value> load(component: Component<out T>, value: T?) {@Suppress("ReplacePutWithAssignment") // would introduce boxing
        if (value != null) components.put(component.id, value)
        else components.remove(component.id)
    }
}
