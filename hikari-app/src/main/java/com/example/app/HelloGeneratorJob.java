package com.example.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@EnableScheduling
@Component
public class HelloGeneratorJob {

    private final HelloRepository helloRepository;
    private final TransactionTemplate transactionTemplate;

    public HelloGeneratorJob(HelloRepository helloRepository, PlatformTransactionManager transactionManager) {
        this.helloRepository = helloRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Scheduled(initialDelay = 1000 * 30, fixedDelay = Long.MAX_VALUE)
    public void generateData() throws InterruptedException {
        int random = new Random().nextInt(33, 100);
        log.info("Trying to generate data with thread count {}", random);
        ExecutorService executor = Executors.newFixedThreadPool(random);

        while (true) {
            for (int i = 0; i < random; i++) {
                executor.submit(() -> {
                    transactionTemplate.execute(new TransactionCallback() {
                        @Override
                        public Object doInTransaction(TransactionStatus status) {
                            String currentThread = Thread.currentThread().getName();
                            System.out.println("Transaction in thread " + currentThread);
                            try {
                                Thread.sleep(new Random().nextInt(500));
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            HelloEntity helloEntity = new HelloEntity();
                            helloEntity.setSender("Sender " + currentThread);
                            helloEntity.setMessage("Hello");
                            return helloRepository.save(helloEntity);
                        }
                    });
                });
            }
            Thread.sleep(1000);
        }
    }
}
