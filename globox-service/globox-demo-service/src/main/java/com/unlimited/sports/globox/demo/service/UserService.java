package com.unlimited.sports.globox.demo.service;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.demo.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.unlimited.sports.globox.model.demo.vo.UserRequestVO;
import com.unlimited.sports.globox.model.demo.vo.UserResponseVO;

import java.util.List;

/**
* @author dk
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2025-12-17 18:26:24
*/
public interface UserService extends IService<User> {


    R<UserResponseVO> getUserById(Long id);

    R<List<UserResponseVO>> getAll();

    R<UserResponseVO> addUser(UserRequestVO userRequestVO);

    R<UserResponseVO> updateUser(Long id, UserRequestVO userRequestVO);

    R<Void> deleteUser(Long id);
}
