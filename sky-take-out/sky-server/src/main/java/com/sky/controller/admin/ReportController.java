package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/report")
@Slf4j
@Api("统计相关接口")
public class ReportController {
    @Autowired
    private ReportService reportService;

    @GetMapping("/turnoverStatistics")
    @ApiOperation("营业额统计")
    //日期的参数需要设置日期的固定模式
    public Result<TurnoverReportVO> turnoverStatistic(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin
            ,@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end){
        log.info("营业额统计：{}{}",begin,end);
        TurnoverReportVO turnoverReportVO = reportService.getTurnover(begin,end);
        return Result.success(turnoverReportVO);
    }

    @GetMapping("/userStatistics")
    @ApiOperation("用户统计")
    public Result<UserReportVO> userReport(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                           @DateTimeFormat(pattern = "yyyy-MM-dd")LocalDate end){
        log.info("用户统计：{}{}",begin,end);
        UserReportVO userReportVO = reportService.getUsers(begin,end);
        return Result.success(userReportVO);
    }

    @GetMapping("/ordersStatistics")
    @ApiOperation("订单统计")
    public Result<OrderReportVO> orderReport(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                             @DateTimeFormat(pattern = "yyyy-MM-dd")LocalDate end){
        log.info("订单统计：{}{}",begin,end);
        OrderReportVO orderReportVO = reportService.getOrders(begin,end);
        return Result.success(orderReportVO);
    }

    @GetMapping("/top10")
    @ApiOperation("Top10菜品")
    public Result<SalesTop10ReportVO> SalesTopReport(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
                                                     @DateTimeFormat(pattern = "yyyy-MM-dd")LocalDate end){
        log.info("Top10菜品：{}{}",begin,end);
        SalesTop10ReportVO salesTop10ReportVO = reportService.SalesTopReport(begin,end);
        return Result.success(salesTop10ReportVO);
    }
}
