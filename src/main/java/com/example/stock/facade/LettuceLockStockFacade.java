package com.example.stock.facade;

import com.example.stock.repository.RedisLockRepository;
import com.example.stock.service.LettuceStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LettuceLockStockFacade {

    private final RedisLockRepository redisLockRepository;
    private final LettuceStockService lettuceStockService;

    public void decrease(Long productId, Long quantity) throws InterruptedException {
        while (!redisLockRepository.lock(productId)) {
            Thread.sleep(100);
        }

        try {
            lettuceStockService.decrease(productId, quantity);
        } finally {
            redisLockRepository.unlock(productId);
        }
    }
}
