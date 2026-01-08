package com.unlimited.sports.globox.demo.mapper;

import com.unlimited.sports.globox.model.demo.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author dk
* @description 针对表【user(用户表)】的数据库操作Mapper
* @createDate 2025-12-17 18:26:24
* @Entity com.unlimited.sports.globox.demo.User
*/
@Mapper
public interface UserMapper extends BaseMapper<User> {

}




