package com.example.stock.service;

import com.example.stock.domain.entity.Stock;
import com.example.stock.facade.LettuceLockStockFacade;
import com.example.stock.facade.NamedLockStockFacade;
import com.example.stock.facade.OptimisticLockStockFacade;
import com.example.stock.facade.RedissonLockStockFacade;
import com.example.stock.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StockServiceTest {

    @Autowired
    private StockService stockService;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private NamedLockStockFacade namedLockStockFacade;
    @Autowired
    private OptimisticLockStockFacade optimisticLockStockFacade;
    @Autowired
    private LettuceLockStockFacade lettuceLockStockFacade;
    @Autowired
    private PessimisticLockStockService pessimisticLockStockService;
    @Autowired
    private RedissonLockStockFacade redissonLockStockFacade;

    @BeforeEach
    void setUp() {
        stockRepository.saveAndFlush(Stock.create(1L, 100L));
    }

    @AfterEach
    void tearDown() {
        stockRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("하나의 재고 감소 요청")
    public void decrease() {
        // when
        stockService.decrease(1L, 1L);

        // then
        Stock stock = stockRepository.findByProductId(1L).orElseThrow();
        assertThat(stock.getQuantity()).isEqualTo(99);
    }

    @Test
    @DisplayName("동시에 100개의 재고 감소 요청")
    public void decreaseAtTheSameTime() throws InterruptedException {
        int threadCount = 100;
        // thread pool 을 32개로 한 이유는?
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        // count down latch 는 설정한 count 만큼 스레드가 종료되길 기다린다.
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < 100; ++i) {
            executorService.submit(() -> {
                try {
                    stockService.decrease(1L, 1L);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        Stock stock = stockRepository.findByProductId(1L).orElseThrow();
        assertThat(stock.getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("Pessimistic Lock 을 활용한 동시에 100개의 재고 감소 요청")
    public void decreaseAtTheSameTimeWithPessimisticLock() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < 100; ++i) {
            executorService.submit(() -> {
                try {
                    pessimisticLockStockService.decrease(1L, 1L);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        Stock stock = stockRepository.findByProductId(1L).orElseThrow();
        assertThat(stock.getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("Optimistic Lock 을 활용한 동시에 100개의 재고 감소 요청")
    public void decreaseAtTheSameTimeWithOptimisticLock() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < 100; ++i) {
            executorService.submit(() -> {
                try {
                    optimisticLockStockFacade.decrease(1L, 1L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        Stock stock = stockRepository.findByProductId(1L).orElseThrow();
        assertThat(stock.getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("Named Lock 을 활용한 동시에 100개의 재고 감소 요청")
    public void decreaseAtTheSameTimeWithNamedLock() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < 100; ++i) {
            executorService.submit(() -> {
                try {
                    namedLockStockFacade.decrease(1L, 1L);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        Stock stock = stockRepository.findByProductId(1L).orElseThrow();
        assertThat(stock.getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("Lettuce 를 활용한 동시에 100개의 재고 감소 요청")
    public void decreaseAtTheSameTimeWithLettuce() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < 100; ++i) {
            executorService.submit(() -> {
                try {
                    lettuceLockStockFacade.decrease(1L, 1L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        Stock stock = stockRepository.findByProductId(1L).orElseThrow();
        assertThat(stock.getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("Redisson 를 활용한 동시에 100개의 재고 감소 요청")
    public void decreaseAtTheSameTimeWithRedisson() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < 100; ++i) {
            executorService.submit(() -> {
                try {
                    redissonLockStockFacade.decrease(1L, 1L);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        Stock stock = stockRepository.findByProductId(1L).orElseThrow();
        assertThat(stock.getQuantity()).isEqualTo(0);
    }
}