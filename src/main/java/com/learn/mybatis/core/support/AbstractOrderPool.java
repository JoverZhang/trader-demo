package com.learn.mybatis.core.support;

import com.learn.mybatis.core.lua.OrderPoolLuaHelper;
import com.learn.mybatis.domain.Order;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * @author Jover Zhang
 */
public abstract class AbstractOrderPool {

    @Getter(value = AccessLevel.PROTECTED)
    @Setter(value = AccessLevel.PROTECTED, onMethod_ = @Autowired)
    private OrderPoolLuaHelper helper;

    /**
     * 获取队列命名空间
     */
    abstract String getNamespace();

    /**
     * 获取挂单池挂单价格的排序顺序
     * <p>
     * TRUE: 按价格正序 <br/>
     * FALSE: 按价格倒序
     */
    abstract boolean isAscending();

    public void addOrder(Order order) {
        assert order.getId() != null;
        assert order.getPrice() != null;
        assert order.getAmount() != null;
        helper.addOrder(getNamespace(), order.getId(),
                order.getPrice().toPlainString(), order.getAmount().toPlainString());
    }

    public void delOrder(Order order) {
        assert order.getId() != null;
        assert order.getPrice() != null;
        helper.delOrder(getNamespace(), order.getId(), order.getPrice().toPlainString());
    }

    public List<Order> match(Order externalOrder) {
        assert externalOrder.getPrice() != null;
        assert externalOrder.getAmount() != null;
        return helper.match(getNamespace(),
                externalOrder.getPrice().toPlainString(),
                externalOrder.getAmount().toPlainString(),
                isAscending());
    }

    @Deprecated
    ConcurrentSkipListMap<BigDecimal, LinkedList<Order>> fetchAllOrders() {
        return helper.fetchOrderPool(getNamespace());
    }

    @Deprecated
    void print() {
        helper.print(getNamespace());
    }

}
