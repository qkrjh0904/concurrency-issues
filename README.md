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
하지만 synchronized 는 하나의 프로세스에서만 보장이된다.  
따라서 서버가 2대 이상인 경우 DB 접근을 여러군데에서 할 수 있다는 문제가 있다.  

실제 운영중인 서비스는 대부분 서버가 2대 이상이기 때문에 synchronized 를 거의 사용하지 않는다.  

### 3. MySQL 을 활용한 방법
1. Pessmistic Lock  
데이터에 Lock을 걸어 정합성을 맞추는 방법이다.  
exclusive lock을 걸게되면 다른 트랜잭션에서는 lock이 해제되기 전에 데이터를 가져갈 수 없게된다.  
데드락이 걸릴 수 있기 때문에 주의해서 사용해야한다.  
충돌이 빈번하게 일어난다면 Optimistic Lock보다 성능이 좋을 수 있다.  
  

2. Optimistic Lock  
실제로 Lock을 이용하지 않고 버전을 이용함으로써 정합성을 맞추는 방법이다.  
먼저 데이터를 읽은 후에 update를 수행할 때 내가 읽은 버전이 맞는지 확인하며 업데이트한다.  
내가 읽은 버전에서 수정사항이 생겼을 때 application에서 다시 읽은 후에 작업을 수행해야 한다.  
DB에 직접 Lock을 잡지 않아 Pessimistic Lock보다 성능은 좋지만, update가 실패했을 때 재시도 로직을 개발자가 직접 작성해야한다.  
충돌이 빈번하게 일어난다면 Pessimistic Lock을 사용하는 것이 좋다.  

  
3. Named Lock  
이름을 가진 metadata lock 이다.  
이름을 가진 lock을 획득한 후 해제할때까지 다른 세션은 이 lock을 획득할 수 없도록 한다.  
주의할점으로는 트랜잭션이 종료될 때 lock이 자동으로 해제되지 않는다.  
별도의 명령어로 해제를 수행해주거나 선점시간이 끝나야 해제된다.  
별도의 데이터소스를 사용해야한다.  

### 4. Redis 이용해보기
1. Lettuce
- setnx 명령어를 활용하여 분산락 구현  
- spin lock 방식 : lock을 획득하려는 스레드가 락을 사용할 수 있는지 반복적으로 확인하며 lock획득을 시도하는 방식


2. Redisson
- pub-sub 기반으로 lock 구현 제공 