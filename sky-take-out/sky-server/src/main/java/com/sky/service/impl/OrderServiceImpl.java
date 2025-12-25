package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //首先要确认地址、购物车的异常情况
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook==null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> listShoppingCart = shoppingCartMapper.list(shoppingCart);

        if( listShoppingCart == null || listShoppingCart.size()==0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //地址和购物车数据正常后可以下单，要向两个表中写入数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setPhone(addressBook.getPhone());
        orders.setUserId(BaseContext.getCurrentId());
        orders.setAddressBookId(addressBook.getId());
        orders.setAddress(addressBook.getDetail());
        orders.setConsignee(addressBook.getConsignee());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));//设置订单号
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setOrderTime(LocalDateTime.now());
        //向订单表中插入一条数据
        orderMapper.insert(orders);

        List<OrderDetail> listOrderDetail = new ArrayList<>();
        //向订单详细说明表中插入多条数据
        for(ShoppingCart shopping:listShoppingCart){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shopping,orderDetail);
            orderDetail.setOrderId(orders.getId());
            listOrderDetail.add(orderDetail);
        }

        orderDetailMapper.insertBatch(listOrderDetail);

        //构造返回值
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(shoppingCart.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
//        // 当前登录用户id
//        Long userId = BaseContext.getCurrentId();
//        User user = userMapper.getById(userId);
//
//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
//
//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));

        //跳过微信支付
        log.info("跳过微信支付，支付成功！");
        paySuccess(ordersPaymentDTO.getOrderNumber());

        return new OrderPaymentVO();
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        //更新了订单状态之后是不是得清空购物车？试一下
        shoppingCartMapper.delete(BaseContext.getCurrentId());

        //通过Websocket推送消息type orderId content
        Map map = new HashMap();
        map.put("type",1);
        map.put("orderId",orders.getId());
        map.put("content","订单号"+outTradeNo);

        String json = JSONObject.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 历史订单查询
     * @param
     * @return
     */
    public PageResult pageQuery(int page,int pageSize,Integer status) {
        PageHelper.startPage(page,pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        //分页条件查询
        Page<Orders> ordersPage = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();
        if(ordersPage !=null&&ordersPage.getTotal()>0){
            for(Orders orders:ordersPage){
                Long orderId = orders.getId();

                //查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }

        return new PageResult(ordersPage.getTotal(),list);
    }

    /**
     *查询订单详情
     * @param id
     * @return
     */
    public OrderVO getOrderById(Long id){
        Orders orders = orderMapper.getById(id);

        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        orderVO.setOrderDetailList(orderDetails);
        return orderVO;
    }

    public void cancelOrder(Long id){
        Orders orders = orderMapper.getById(id);
        if(orders==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Integer status = orders.getStatus();
        if(status>2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders ordersDB = new Orders();//重新构建一个新的对象是为了具体修改值，update操作会修改不是非空的列表值
        // 所以我们只需要把需要修改的值单独摘出来就好了，其他的没有必要重复覆盖
        ordersDB.setId(orders.getId());//Id是一定需要的，需要根据这个具体的值找到需要修改的条次

        //订单处于待接单状态，需要进行退款
        if(status.equals(Orders.TO_BE_CONFIRMED)){
            //调用微信支付退款接口
            //  weChatPayUtil.refund(
            //          ordersDB.getNumber(), //商户订单号
            //          ordersDB.getNumber(), //商户退款单号
            //          new BigDecimal(0.01),//退款金额，单位 元
            //          new BigDecimal(0.01));//原订单金额

            //支付状态修改为 退款
            ordersDB.setPayStatus(Orders.REFUND);
        }

        //更新订单状态、取消原因、取消时间
        ordersDB.setStatus(Orders.CANCELLED);
        ordersDB.setCancelReason("用户取消");
        ordersDB.setCancelTime(LocalDateTime.now());
        orderMapper.update(ordersDB);
    }

    public void repetitionOrder(Long id){
        //查询当前的用户id
        Long userId = BaseContext.getCurrentId();
        //根据订单id查询订单详情
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        //将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetails.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        //将购物车数据批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 管理端订单查询
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult search(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());
        Page<Orders> orders = orderMapper.pageQuery(ordersPageQueryDTO);

        //部分订单的状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> list = new ArrayList<>();
        if(orders.getTotal()>0){
            for(Orders orders1: orders){
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders1,orderVO);
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders1.getId());
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO);
            }
        }
        return new PageResult(orders.getTotal(),list);
    }

    /**
     * 各个状态的订单数量查询
     * @return
     */
    public OrderStatisticsVO statistic() {
        List<Orders> confirm = orderMapper.getByStatistic(Orders.CONFIRMED);
        List<Orders> delivery = orderMapper.getByStatistic(Orders.DELIVERY_IN_PROGRESS);
        List<Orders> toBeConfirmed = orderMapper.getByStatistic(Orders.TO_BE_CONFIRMED);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setConfirmed(confirm.size());
        orderStatisticsVO.setToBeConfirmed(delivery.size());
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed.size());
        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO){
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 拒单
     * @param
     */
    public void reject(OrdersRejectionDTO ordersRejectionDTO){
        //拒单就是将订单状态修改为已取消
        //只有订单处于“待接单”的状态可以执行拒单操作，一般我们处理异常情况
        Orders orders = orderMapper.getById(ordersRejectionDTO.getId());
        if(orders==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if(orders.getStatus()!=Orders.TO_BE_CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //商家拒单时，如果用户已经完成支付，需要为用户退款
        /*
        //支付状态
        Integer payStatus = orders.getPayStatus();
        if (payStatus == Orders.PAID) {
            //用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("申请退款：{}", refund);
        }
        */

        //商家拒单时需要指定拒单原因
        Orders ordersDB = new Orders();
        ordersDB.setId(orders.getId());
        ordersDB.setStatus(Orders.CANCELLED);
        ordersDB.setCancelTime(LocalDateTime.now());
        ordersDB.setCancelReason(ordersRejectionDTO.getRejectionReason());

        orderMapper.update(ordersDB);
    }

    /**
     * 取消订单
     * @param ordersCancelDTO
     */
    public void cancelOrder(OrdersCancelDTO ordersCancelDTO){
        Orders orders = new Orders();
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());
        if(ordersDB==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
//        if(ordersDB.getPayStatus()==Orders.PAID){
//            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);
//        }
        orders.setStatus(Orders.CANCELLED);
        orders.setId(ordersCancelDTO.getId());
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orderMapper.update(orders);
    }

    /**
     * 派送
     * @param id
     */
    public void delivery(Long id){
        //只有状态为待派送才可以执行派送订单操作
        Orders ordersDB = orderMapper.getById(id);
        if(ordersDB==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if(ordersDB.getStatus()!=Orders.CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        orderMapper.update(orders);
    }

    public void complete(Long id){
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 客户催单
     * @param id
     */
    public void reminder(Long id){
        //查询订单是否存在
        Orders orders = orderMapper.getById(id);
        if(orders==null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //基于WebSocket实现催单
        Map map = new HashMap();
        map.put("type",2);
        map.put("orderId",id);
        map.put("content","订单号"+orders.getNumber());
        String json = JSONObject.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

}
