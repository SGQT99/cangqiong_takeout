package com.sky.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data //来自lombok库，自动生成所有字段的getter/setter、toString、equals方法
@ApiModel(description = "员工登录时传递的数据模型")  //来自Swagger/OpenAPI的注解，用于API文档生成
public class EmployeeLoginDTO implements Serializable {
    //实现Serializable接口，表示对象可以被序列化（转换为字节流），用于网络传输、缓存存储等需要序列化的场景

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("密码")
    private String password;

}
//DTO,专用传输容器，数据格式标准化，安全保障，不包含敏感的业务逻辑，只是纯数据载体
//实际工作流程：用户输入--前端封装成DTO--HTTP请求--后端接收DTO--业务处理