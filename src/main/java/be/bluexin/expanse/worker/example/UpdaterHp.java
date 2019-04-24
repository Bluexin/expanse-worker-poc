package be.bluexin.expanse.worker.example;

import be.bluexin.expanse.worker.Component;
import be.bluexin.expanse.worker.EntityStore;
import be.bluexin.expanse.worker.Health;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.ExecutionException;

public class UpdaterHp {

    static Component<Health> HEALTH_JAVA = new Component<>("HEALTH_JAVA", 2, Health.class);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Logger logger = LoggerFactory.getLogger("HP Updater");
        logger.info("Starting connection");
        EntityStore.INSTANCE.connectAeronFuture().get();
        logger.info("Entering stuff");

        Random rng = new Random();
        for (int i = 0; i < 20; i++) {
            Health hp = new Health(rng.nextFloat(), rng.nextFloat());
            EntityStore.INSTANCE.get(2).set(HEALTH_JAVA, hp);

            logger.info("Set hp to " + hp);
            Thread.sleep(1000);
        }

        Thread.sleep(3000);
        EntityStore.INSTANCE.disconnectAeronBlocking();
    }
}
