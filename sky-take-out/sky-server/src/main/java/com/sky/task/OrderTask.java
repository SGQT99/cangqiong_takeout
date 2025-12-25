package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Component//实例化并放进Spring容器中
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;

    //处理支付超时订单
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder(){
        log.info("定时处理超时订单：{}", new Date());
        //首先根据订单状态和下单时间查询订单，查询到可能超时的订单然后更改订单的状态
        LocalDateTime time = LocalDateTime.now().minusMinutes(15);
        List<Orders> orders = orderMapper.getByStatusAndOrdertimeLT(Orders.PENDING_PAYMENT,time);
        if(orders!=null&&orders.size()>0){
            for(Orders order:orders){
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("支付超时，自动取消");
                order.setOrderTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }

    //处理长时间未派送的订单
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder() {
        log.info("定时处理处于派送中的订单");
        //LocalDateTime time = LocalDateTime.now().minusMinutes(60);
        LocalDateTime time = LocalDateTime.now().minusSeconds(5);
        //每天凌晨一点处理前一天还在派送中的订单，可能是骑手没有点击已完成
        List<Orders> ordersList = orderMapper.getByStatusAndOrdertimeLT(Orders.DELIVERY_IN_PROGRESS, time);

        //如果有必要可以加上一个筛选，不处理当前下单时间不超过一段时间的，防止用户下单被自动完成订单
        if (ordersList != null && ordersList.size() > 0) {
            ordersList.forEach(order -> {
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            });
        }
    }
}
