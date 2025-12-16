package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


public interface SetmealService {

    void addSetmeal(SetmealDTO setmealDTO);

    /**
     * 分页查询套餐
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult page(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 删除套餐
     * @param ids
     */
    void deleteSetmeal(List<Long> ids);

    SetmealVO getSetmealById(Long id);

    void update(SetmealDTO setmealDTO);

    /**
     * 修改起售或停售状态
     * @param status
     * @param id
     */
    void startOrStop(Integer status,Long id);

}
