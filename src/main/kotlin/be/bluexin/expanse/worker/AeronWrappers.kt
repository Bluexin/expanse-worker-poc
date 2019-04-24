package be.bluexin.expanse.worker

import io.aeron.Aeron
import io.aeron.FragmentAssembler
import io.aeron.Publication
import io.aeron.logbuffer.FragmentHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import mu.KotlinLogging.logger
import org.agrona.concurrent.BackoffIdleStrategy
import org.agrona.concurrent.UnsafeBuffer
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

private const val aeronUrl = "aeron:udp?endpoint=localhost:40123"
private const val aeronStream = 10

// TODO: investigate the use of Exclusive* stuff (https://github.com/real-logic/aeron/wiki/Client-Concurrency-Model)

/**
 * Creates an Aeron producer, relaying everything from [input] to the Aeron bus.
 */
@ExperimentalCoroutinesApi
fun CoroutineScope.aeronProducer(aeron: Aeron, input: ReceiveChannel<ByteArray>): Job = launch(Dispatchers.IO) {
    val logger = logger("Aeron producer")
    logger.info { "Booting up the Aeron producer" }

    val pub = aeron.addPublication(aeronUrl, aeronStream)
    val buff = UnsafeBuffer(ByteBuffer.allocateDirect(256))
    val idleStrategy = idleStrategy

    logger.info { "Starting to send to Aeron" }

    for (i in input) {
        buff.putBytes(0, i)
        var res = pub.offer(buff, 0, i.size)
        while (isActive && res <= 0) {
            when (res) {
                Publication.CLOSED -> {
                    input.cancel()
                    return@launch
                }
                else -> {
                    idleStrategy.idle()
                    res = pub.offer(buff, 0, i.size)
                }
            }
        }
        if (!isActive) break
        idleStrategy.reset()
    }

    pub.close()

    logger.info { "Stopping to send to Aeron (canceled: ${!isActive}, input closed: ${input.isClosedForReceive})" }
}

/**
 * Creates an Aeron consumer, relaying everything from the Aeron bus to the returned Channel.
 */
@ExperimentalCoroutinesApi
fun CoroutineScope.aeronConsumer(aeron: Aeron): ReceiveChannel<ByteArray> = produce(Dispatchers.IO, capacity = Channel.UNLIMITED) {
    val logger = logger("Aeron consumer")
    logger.info { "Booting up the Aeron consumer" }

    val idleStrategy = idleStrategy
    val sub = aeron.addSubscription(aeronUrl, aeronStream)
    val fragmentHandler = FragmentAssembler(FragmentHandler { buffer, offset, length, _ ->
        val data = ByteArray(length)
        buffer.getBytes(offset, data)
        offer(data)
    })

    logger.info { "Starting to consume from Aeron" }

    while (isActive) {
        val fragmentsRead = sub.poll(fragmentHandler, 10)
        if (isActive) idleStrategy.idle(fragmentsRead)
    }

    sub.close()

    logger.info { "Stopping to consume from Aeron (canceled: ${!isActive}, output closed: ${channel.isClosedForSend})" }
}

private val idleStrategy
    get() = BackoffIdleStrategy(
        100,
        10,
        TimeUnit.MICROSECONDS.toNanos(1),
        TimeUnit.MICROSECONDS.toNanos(100)
    )
