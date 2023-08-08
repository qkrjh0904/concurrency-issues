# concurrency-issues

### 1. 동시성 이슈 발생
하나의 상품에 100개의 재고가 있다고 가정했을 때, 동시에 여러 요청이 들어온다면 어떻게 될까?

아래 소스코드에서 100개의 재고 감소 요청을 멀티스레드로 동시에 요청하면 어떻게 될까? 
```java
@Test
@DisplayName("동시에 100개의 재고 감소 요청")
public void decreaseAtTheSameTime() throws InterruptedException {
    int threadCount = 100;
    ExecutorService executorService = Executors.newFixedThreadPool(32);
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
```

우리가 기대하던 상황은 1번 스레드가 재고를 가져와 감소시키면 그 후에 2번 스레드가 재고를 가져와 감소시키기를 기대한다.  
하지만 실제로는 1번 스레드가 가져와 감소시킨 후 db를 업데이트 시키기 전에 2번 스레드가 가져와 감소시켜 2번이 누락되게 된다.

### 2. synchronized 이용해보기
`@Transactional`을 사용하면 매핑하고 있는 클래스를 새로 만들어 실행하게 되어 race condition 을 막을 수 없다.  
그래서 기존의 문제를 해결할 수 없다.  
일단 `@Transactional`을 주석처리하고 synchronized 를 붙여주면 하나의 스레드만 접근가능하다. 
```java
// @Transactional
public synchronized void decrease(Long productId, Long quantity) {
    // stock 조회
    Stock stock = stockRepository.findByProductId(productId).orElseThrow();

    // 재고 감소
    stock.decrease(quantity);
    stockRepository.saveAndFlush(stock);
}
```