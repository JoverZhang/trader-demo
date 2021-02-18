package com.learn.mybatis.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import java.math.BigDecimal;

/**
 * @author Jover Zhang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Nonnull
    String id;

    @Nonnull
    BigDecimal price;

    @Nonnull
    BigDecimal amount;

    public Order(@Nonnull String id, @Nonnull String price, @Nonnull String amount) {
        this.id = id;
        this.price = new BigDecimal(price);
        this.amount = new BigDecimal(amount);
    }

    public static Order decode(@Nonnull String str) {
        String[] orderInfo = str.split(",");
        return Order.builder()
                .id(orderInfo[0])
                .price(new BigDecimal(orderInfo[1]))
                .amount(new BigDecimal(orderInfo[2]))
                .build();
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            return true;
        }
        if (obj instanceof Order) {
            Order o = (Order) obj;
            if (!this.toString().equals(o.toString())) {
                if (!this.getId().equals(o.getId())) {
                    return false;
                }
                if (this.getPrice().compareTo(o.getPrice()) != 0) {
                    return false;
                }
                return this.getAmount().compareTo(o.getAmount()) == 0;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", price=" + price.toPlainString() +
                ", amount=" + amount.toPlainString() +
                '}';
    }

}
