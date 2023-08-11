package com.example.stock.service;

import com.example.stock.domain.entity.Stock;
import com.example.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    @Transactional
    public void decrease(Long productId, Long quantity) {
        Stock stock = stockRepository.findByProductId(productId).orElseThrow();

        // 재고 감소
        stock.decrease(quantity);
        stockRepository.saveAndFlush(stock);
    }
}
