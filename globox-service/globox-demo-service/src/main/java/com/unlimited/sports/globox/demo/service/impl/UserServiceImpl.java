package com.unlimited.sports.globox.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.unlimited.sports.globox.common.result.DemoCode;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.demo.mapper.UserMapper;
import com.unlimited.sports.globox.demo.service.UserService;
import com.unlimited.sports.globox.model.demo.entity.User;
import com.unlimited.sports.globox.model.demo.vo.UserRequestVO;
import com.unlimited.sports.globox.model.demo.vo.UserResponseVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * UserServiceImpl
 *
 * @author dk
 * @since 2025/12/17 19:09
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    @Override
    public R<UserResponseVO> getUserById(Long id) {
        User user = this.getById(id);
        Assert.isNotEmpty(user, DemoCode.USER_NOT_EXIST);

        UserResponseVO userResponseVO = new UserResponseVO();
        BeanUtils.copyProperties(user, userResponseVO);

        return R.ok(userResponseVO);
    }

    @Override
    public R<List<UserResponseVO>> getAll() {
        List<User> list = list();
        Assert.isNotEmpty(list, DemoCode.USER_NOT_EXIST);

        List<UserResponseVO> listVO = list.stream().map(user -> {
            UserResponseVO userResponseVO = new UserResponseVO();
            BeanUtils.copyProperties(user, userResponseVO);
            return userResponseVO;
        }).toList();

        return R.ok(listVO);
    }

    @Override
    public R<UserResponseVO> addUser(UserRequestVO userRequestVO) {
        User user = new User();
        BeanUtils.copyProperties(userRequestVO, user);

        boolean save = save(user);

        Assert.isTrue(save, DemoCode.USER_INSERT_ERROR);

        UserResponseVO userResponseVO = new UserResponseVO();
        BeanUtils.copyProperties(user, userResponseVO);

        return R.ok(userResponseVO);
    }

    @Override
    public R<UserResponseVO> updateUser(Long id, UserRequestVO userRequestVO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getId, id);

        User user = getOne(queryWrapper);

        Assert.isNotEmpty(user, DemoCode.USER_UPDATE_ERROR);

        UserResponseVO userResponseVO = new UserResponseVO();
        BeanUtils.copyProperties(user, userResponseVO);
        return R.ok(userResponseVO);
    }

    @Override
    public R<Void> deleteUser(Long id) {
        boolean success = removeById(id);
        Assert.isTrue(success, DemoCode.USER_DELETE_ERROR);
        return R.ok();
    }
}




