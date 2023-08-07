package com.example.stock.domain.entity;

import lombok.Getter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@Getter
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private Long quantity;

    public static Stock create(Long productId, Long quantity) {
        Stock stock = new Stock();
        stock.productId = productId;
        stock.quantity = quantity;
        return stock;
    }

    public void decrease(Long quantity) {
        if (this.quantity < quantity) {
            throw new IllegalArgumentException("재고는 0개 미만이 될 수 없습니다.");
        }
        this.quantity -= quantity;
    }
}
