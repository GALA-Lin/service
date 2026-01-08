package com.unlimited.sports.globox.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 元信息处理器
 *
 * @author dk
 * @since 2025/12/17 21:59
 */
@Component
public class MyBatisPlusMetaObjectHandler implements MetaObjectHandler {
    /**
     * 新增时自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        //参数1:元数据对象
        //参数2:类属性名称
        //参数3:类对象
        //参数4:当前系统时间
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 修改时自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
