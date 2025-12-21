package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        //首先需要查询当前购物车内的菜品或套餐是否已经添加，如果已经添加，那就只需要更新个数
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        //只能查询自己购物车的数据，通过从线程里获取用户id
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        if (list != null && list.size() == 1) {
            shoppingCart = list.get(0);
            shoppingCart.setNumber(shoppingCart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(shoppingCart);
        }else {

            //如果没有添加，则需要插入新的菜品
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {
                //添加到购物车的是菜品
                Dish dish = dishMapper.getById(dishId);//先查询到菜品信息
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());//设置菜品相关的信息
            } else {
                //添加到购物车的是套餐
                Setmeal setmeal = setmealMapper.getById(shoppingCartDTO.getSetmealId());//查询套餐
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());//设置套餐相关信息
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());//设置购物车内菜品相关信息
            shoppingCartMapper.insert(shoppingCart);//插入数据
        }
    }

    /**
     * 查看购物车
     * @return
     */
    public List<ShoppingCart> showShoppingCart() {
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(BaseContext.getCurrentId());
        //查询
        List<ShoppingCart> shoppingCartList  =  shoppingCartMapper.list(shoppingCart);
        return shoppingCartList;
    }

    public void cleanShoppingCart(){
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.delete(userId);
    }

    @Override
    public void deleteSig(ShoppingCartDTO shoppingCartDTO) {
        //先查询当前菜品是否只有一个
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list != null && list.size() == 1) {
            ShoppingCart shoppingCart1 = list.get(0);
            Integer number = shoppingCart1.getNumber();
            //如果只有一个，根据菜品id删除掉，如果不是只有一个，只需要更新number为减一即可
            if(number==1){
                //shoppingCartMapper.deleteByDish(shoppingCart1);//这样写还要根据菜品id和套餐id来删除，直接根据购物车中的id删除即可
                shoppingCartMapper.deleteById(shoppingCart1.getId());
            }else{
                shoppingCart1.setNumber(number-1);
                shoppingCartMapper.updateNumberById(shoppingCart1);
            }
        }
    }
}
