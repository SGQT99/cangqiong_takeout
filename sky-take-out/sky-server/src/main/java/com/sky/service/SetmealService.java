package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.mapper.SetmealMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


public interface SetmealService {

    void addSetmeal(SetmealDTO setmealDTO);

}
