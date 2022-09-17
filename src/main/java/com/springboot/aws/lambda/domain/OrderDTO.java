package com.springboot.aws.lambda.domain;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OrderDTO {

    @SerializedName("Orders")
    private List<Order> orderList;

    public List<Order> getOrderList() {
        return orderList;
    }

    public void setOrderList(List<Order> orderList) {
        this.orderList = orderList;
    }

    @Override
    public String toString() {
        return "OrderDTO{" +
                "orderList=" + orderList +
                '}';
    }
}
