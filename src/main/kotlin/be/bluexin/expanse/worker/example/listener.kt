package be.bluexin.expanse.worker.example

import be.bluexin.expanse.worker.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

@UseExperimental(ExperimentalCoroutinesApi::class)
fun main() {
    runBlocking {
        val logger = KotlinLogging.logger("main")
        logger.info { "Starting connection" }
        EntityStore.connectAeron()
        logger.info { "Entering stuff" }

        Component.POSITION.listen()
        Component.HEALTH.listen()
        UpdaterHp.HEALTH_JAVA.listen()

        for (i in 0 until 10) {
            // We retrieve the entity with ID 2
            val entity: Entity = EntityStore[2]

            // We retrieve the position for our entity
            val pos: Position? = entity[Component.POSITION]
            val hp: Health? = entity[Component.HEALTH]
            val hpJava: Health? = entity[UpdaterHp.HEALTH_JAVA]

            logger.info { "Found health: $hp, health_java: $hpJava and pos: $pos" }
            delay(2500)
        }

        delay(3000)
        EntityStore.disconnectAeron()
    }
}
