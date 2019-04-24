package be.bluexin.expanse.worker

import kotlin.reflect.KProperty

// TODO: centralized way to define component IDs
//  These could be held in a master controller and queries by workers, or whatever

@Suppress("unused") // generic type is used for set/get safety
open class Component<T : Value>(val name: String, val id: Int, val clazz: Class<T>) {

    /**
     * Register this component in the [ComponentStore].
     * This is needed only if you need updates for this component.
     */
    fun listen() {
        ComponentStore += this
    }

    companion object {
        @JvmStatic
        val POSITION by component<Position>(0)
        @JvmStatic
        val HEALTH by component<Health>(1)

        /**
         * Shorthand for creating component delegates
         */
        inline fun <reified T : Value> component(id: Int) = ComponentDelegate(id, T::class.java)

        /**
         * Holder for automatic component delegates
         */
        class ComponentDelegate<T : Value>(private val id: Int, private val clazz: Class<T>) {
            private lateinit var component: Component<T>

            operator fun getValue(receiver: Any, property: KProperty<*>) = component

            operator fun provideDelegate(receiver: Any, property: KProperty<*>) = this.apply {
                component = Component(property.name, this.id, this.clazz)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Component<*>) return false

        if (name != other.name) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + id
        return result
    }

    override fun toString() = "Component(name='$name', id=$id)"
}
