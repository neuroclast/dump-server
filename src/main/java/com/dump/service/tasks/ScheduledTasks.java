package com.dump.service.tasks;

import com.dump.service.objects.Dump;
import com.dump.service.repositories.DumpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Spring Component to handle scheduled tasks
 */
@Component
public class ScheduledTasks {

    @Autowired
    DumpRepository dumpRepository;

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");


    /**
     * Task to clear expired Dumps. Runs one hour after completion of previous run.
     */
    @Scheduled(fixedDelay = 3600000) // every hour
    public void clearExpired() {
        logger.info("Running expired cleanup task... - {}", dateFormat.format(new Date()));

        Dump[] dumps = dumpRepository.findByExpirationIsAfterAndExpirationIsBefore(new Date(3600), new Date());

        if(dumps.length > 0) {
            logger.info("Purging {} expired dumps...", dumps.length);

            for (Dump dump : dumps) {
                // TODO: uncomment for production
                // dumpRepository.delete(dump);
            }

            logger.info("Purged {} dumps!", dumps.length);

            return;
        }

        logger.info("No dumps to purge right now.");
    }
}
