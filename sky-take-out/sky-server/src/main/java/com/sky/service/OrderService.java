package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 历史订单查询
     * @param
     * @return
     */
    PageResult pageQuery(int page, int pageSize,Integer status);

    OrderVO getOrderById(Long id);

    /**
     * 取消订单
     * @param id
     */
    void cancelOrder(Long id);

    /**
     * 商家取消订单
     * @param ordersCancelDTO
     */
    void cancelOrder(OrdersCancelDTO ordersCancelDTO);

    void repetitionOrder(Long id);

    /**
     * 管理端订单查询
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult search(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 各个状态的订单数量查询
     * @return
     */
    OrderStatisticsVO statistic();

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 拒单
     * @param
     */
    void reject(OrdersRejectionDTO ordersRejectionDTO);

    /**
     * 派送
     * @param id
     */
    void delivery(Long id);

    void complete(Long id);
}
