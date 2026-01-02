package com.unlimited.sports.globox.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;


/**
 * Jackson 配置类，用于自定义 Jackson 序列化器的行为。
 * 该配置确保 Long 类型（包括包装类型和基本类型）在序列化时被转换为 String 格式，
 * 这有助于避免前端接收长整型数据时可能出现的精度丢失问题。
 */
@Configuration
public class JacksonConfig {


    @Autowired
    public void configureObjectMapper(ObjectMapper objectMapper) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(module);
    }
}