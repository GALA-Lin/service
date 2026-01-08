package com.unlimited.sports.globox.demo.controller;


import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.demo.service.OrderService;
import com.unlimited.sports.globox.demo.service.UserService;
import com.unlimited.sports.globox.dubbo.demo.ProducerDubboService;
import com.unlimited.sports.globox.model.demo.vo.CreateVenueOrderRequestVO;
import com.unlimited.sports.globox.model.demo.vo.UserRequestVO;
import com.unlimited.sports.globox.model.demo.vo.UserResponseVO;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 控制层
 *
 * @author dk
 * @since 2025/12/17 18:27
 */
@RestController
@RequestMapping("/demo")
public class DemoController {

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @DubboReference(group = "rpc")
    private ProducerDubboService producerDubboService;

    @GetMapping("user/{id}")
    public R<UserResponseVO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("user")
    public R<List<UserResponseVO>> getAll() {
        return userService.getAll();
    }

    @PostMapping("user")
    public R<UserResponseVO> addUser(@RequestBody UserRequestVO userRequestVO) {
        return userService.addUser(userRequestVO);
    }

    @PutMapping("user/{id}")
    public R<UserResponseVO> updateUser(@PathVariable Long id, @RequestBody UserRequestVO userRequestVO) {
        return userService.updateUser(id, userRequestVO);
    }

    @DeleteMapping("user/{id}")
    public R<Void> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id);
    }


    @GetMapping("/rpc/{name}")
    public R<String> sayHello(@PathVariable  String name) {
        return R.ok(producerDubboService.sayHello(name));
    }


    @PostMapping("/create")
    public R<String> create(@RequestBody CreateVenueOrderRequestVO createVenueOrderRequestVO) {
        return R.ok(orderService.create(createVenueOrderRequestVO));

    }



}
