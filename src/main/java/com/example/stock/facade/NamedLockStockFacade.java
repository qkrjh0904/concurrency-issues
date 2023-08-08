package com.example.stock.facade;

import com.example.stock.repository.LockRepository;
import com.example.stock.service.NamedLockStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NamedLockStockFacade {

    private final LockRepository lockRepository;
    private final NamedLockStockService namedLockStockService;

    @Transactional
    public void decrease(Long productId, Long quantity) {
        try {
            lockRepository.getLock(productId.toString());
            namedLockStockService.decrease(productId, quantity);
        } finally {
            lockRepository.releaseLock(productId.toString());
        }

    }
}
