package be.bluexin.expanse.worker.example

import be.bluexin.expanse.worker.Component
import be.bluexin.expanse.worker.EntityStore
import be.bluexin.expanse.worker.Position
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import kotlin.random.Random

@UseExperimental(ExperimentalCoroutinesApi::class)
fun main() {
    runBlocking {
        val logger = KotlinLogging.logger("main")
        logger.info { "Starting connection" }
        EntityStore.connectAeron()
        logger.info { "Entering stuff" }

        val rng = Random(Random.nextLong())
        for (i in 0 until 20) {
            val pos = Position(rng.nextDouble(), rng.nextDouble(), rng.nextDouble())
            // We store a position in entity with ID 2
            EntityStore[2][Component.POSITION] = pos

            logger.info { "Set pos to: $pos" }
            delay(1000)
        }

        delay(3000)
        EntityStore.disconnectAeron()
    }

}
