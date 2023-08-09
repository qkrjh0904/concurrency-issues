package com.example.stock.service;

import com.example.stock.domain.entity.Stock;
import com.example.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LettuceStockService {

    private final StockRepository stockRepository;

    @Transactional
    public void decrease(Long productId, Long quantity) {
        Stock stock = stockRepository.findByProductId(productId).orElseThrow();
        stock.decrease(quantity);
    }
}
