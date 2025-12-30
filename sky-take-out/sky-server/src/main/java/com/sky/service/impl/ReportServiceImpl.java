package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;


    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnover(LocalDate begin, LocalDate end) {
        List<LocalDate> datelist = new ArrayList<>();//用于存放从开始到结束每一天的日期
        datelist.add(begin);

        while(!begin.equals(end)){
            begin = begin.plusDays(1);//日期计算，获得指定日期后1天的日期
            datelist.add(begin);
        }

        List<Double> doublelist = new ArrayList<>();
        for (LocalDate date : datelist) {
            //LocalTime只有年月日，所以把它转换为LocalDateTime有时分秒的
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//通过of这个方法，获取当天时间的开始（包含时分秒）
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);//通过of这个方法，获取当天时间的结束（包含时分秒）
            Map map = new HashMap();//查询条件封装进一个Map
            map.put("status", Orders.COMPLETED);
            map.put("begin", beginTime);
            map.put("end", endTime);
            //查询date日期对应的营业额
            //选择日期在这一天且状态为已完成的订单，并返回它们的总和
            Double turnover = orderMapper.sumByMap(map);
            //处理营业额为空的特殊情况
            if(turnover==null){
                turnover = 0.0;
            }
            doublelist.add(turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(datelist,','))
                .turnoverList(StringUtils.join(doublelist,','))
                .build();
    }

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUsers(LocalDate begin,LocalDate end) {
        //同样先获取日期列表
        List<LocalDate> datelist = new ArrayList<>();
        datelist.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            datelist.add(begin);
        }
        List<Integer> userNumList = new ArrayList<>();
        List<Integer> newUserNumList = new ArrayList<>();
        for (LocalDate date : datelist) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Integer userNum = getUserCount(null,endTime);
            Integer newUserNum = getUserCount(beginTime,endTime);

//            Map map = new HashMap();
//            map.put("createTime", beginTime);
//            map.put("endTime", endTime);
//            Integer userNum = userMapper.getUserByTime(endTime);//写两个方法还是太麻烦了，可以封装成一个函数
            userNumList.add(userNum);
//            Integer newUserNum = userMapper.getNewUserByMap(map);
            newUserNumList.add(newUserNum);
        }
        return UserReportVO.builder().dateList(StringUtils.join(datelist,','))
                .totalUserList(StringUtils.join(userNumList,','))
                .newUserList(StringUtils.join(newUserNumList,',')).build();
    }

    /**
     * 用户统计的工具函数
     * @param beginTime
     * @param endTime
     * @return
     */
    public Integer getUserCount(LocalDateTime beginTime, LocalDateTime endTime){
        Map map = new HashMap();
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        Integer Num = userMapper.countByMap(map);
        return Num;
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrders(LocalDate begin, LocalDate end) {
        //首先，也是包含时间列表的
        List<LocalDate> datelist = new ArrayList<>();
        datelist.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            datelist.add(begin);
        }

        //需要另外返回两个Integer,分别是订单总数和有效订单数
        //订单总数和有效订单数的区别在于完成状态，有效订单数的完成状态是已完成
        //需要返回两个列表，分别是订单数列表和有效订单列表，以逗号分隔
        //总数和每一天的订单数的区别是，总数只需要传入endTime
        List<Integer> totalOrdersList = new ArrayList<>();
        List<Integer> validateOrdersList = new ArrayList<>();
        for(LocalDate date :datelist){
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Integer totalOrder = getOrdersCounts(null,beginTime,endTime);
            Integer validateOrder = getOrdersCounts(Orders.COMPLETED,beginTime,endTime);

            totalOrdersList.add(totalOrder);
            validateOrdersList.add(validateOrder);

//            totalOrders = totalOrders + totalOrder;
//            validateOrders = validateOrders + validateOrder;
        }

        //返回订单完成率，double类型，就是有效订单数/订单总数
//        Double completeScore = validateOrders*1.0/totalOrders;//强转
        //自己的方法没有考虑到订单数为0的情况，可能会造成除0的错误
        //以及这里使用了流的方式
        //时间区间内的总订单数
        Integer totalOrderCount = totalOrdersList.stream().reduce(Integer::sum).get();
        //时间区间内的总有效订单数
        Integer validOrderCount = validateOrdersList.stream().reduce(Integer::sum).get();
        //订单完成率
        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0){
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(datelist,','))
                .orderCountList(StringUtils.join(totalOrdersList,','))
                .validOrderCountList(StringUtils.join(validateOrdersList,','))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 订单统计子函数
     * @param status
     * @param begin
     * @param end
     * @return
     */
    public Integer getOrdersCounts(Integer status, LocalDateTime begin, LocalDateTime end) {
        Map map = new HashMap();
        map.put("status", status);
        map.put("begin", begin);
        map.put("end", end);
        return orderMapper.countOrdersByMap(map);
    }


    /**
     * Top10菜品
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO SalesTopReport(LocalDate begin, LocalDate end) {
        //根据时间区间，展示销量前10的商品（包括菜品和套餐）
        //销量为商品销售的份数
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);//找半天错误在哪，我真服了。。。。。这里是end啊啊啊啊
        List<GoodsSalesDTO> goodsSalesList = orderMapper.getSalesTop10(beginTime,endTime);
        //List<GoodsSalesDTO> goodsSalesList = orderMapper.getSalesTop10(LocalDateTime.parse("2025-12-22T00:00:00"),
                //LocalDateTime.parse("2025-12-28T23:59:59"));

        String nameList = StringUtils.join(goodsSalesList.stream()
                 .map(GoodsSalesDTO::getName)
                 .collect(Collectors.toList()),",");

        String numberList = StringUtils.join(goodsSalesList.stream()
                .map(GoodsSalesDTO::getNumber)
                .collect(Collectors.toList()),",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

}
