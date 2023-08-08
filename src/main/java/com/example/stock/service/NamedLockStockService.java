package com.example.stock.service;

import com.example.stock.domain.entity.Stock;
import com.example.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NamedLockStockService {

    private final StockRepository stockRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decrease(Long productId, Long quantity) {
        // stock 조회
        Stock stock = stockRepository.findByProductId(productId).orElseThrow();

        // 재고 감소
        stock.decrease(quantity);
        stockRepository.saveAndFlush(stock);
    }
}
