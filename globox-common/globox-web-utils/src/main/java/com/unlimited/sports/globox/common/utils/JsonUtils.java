package com.unlimited.sports.globox.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;


/**
 * JSON 工具类
 */
@Log4j2
@Component
public class JsonUtils {

    private final ObjectMapper mapper;

    public JsonUtils() {
        mapper = new ObjectMapper();

        // === 1. Java 8 时间支持（核心）===
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // LocalDateTime 序列化
        javaTimeModule.addSerializer(
                LocalDateTime.class,
                new LocalDateTimeSerializer(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                )
        );

        // LocalDateTime 反序列化
        javaTimeModule.addDeserializer(
                LocalDateTime.class,
                new LocalDateTimeDeserializer(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                )
        );

        mapper.registerModule(javaTimeModule);

        // === 2. 禁用时间戳（否则会变成数组）===
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // === 3. 容错配置（推荐）===
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * 将对象转换成json字符串。
     *
     * @param data 数据
     * @return JSON 字符串
     */
    public String objectToJson(Object data) {
        try {
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(),e);
            throw new GloboxApplicationException(e);
        }
    }

    /**
     * 将json结果集转化为对象
     *
     * @param jsonData json数据
     * @param beanType 对象中的object类型
     * @return T
     */
    public <T> T jsonToPojo(String jsonData, Class<T> beanType) {
        try {
            return mapper.readValue(jsonData, beanType);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            throw new GloboxApplicationException(e);
        }
    }

    /**
     * 将json数据转换成pojo对象list
     *
     * @param jsonData json数据
     * @param beanType 对象中的object类型
     * @return List<T>
     */
    public <T> List<T> jsonToList(String jsonData, Class<T> beanType) {
        JavaType javaType = mapper.getTypeFactory().constructParametricType(List.class, beanType);
        try {
            return mapper.readValue(jsonData, javaType);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            throw new GloboxApplicationException(e);
        }
    }


    /**
     * 将 json 数据转换为 map
     *
     * @param jsonData json数据
     * @return java.util.Map
     */
    public Map<String, Object> jsonToMap(String jsonData) {
        try {
            return mapper.readValue(jsonData, new TypeReference<>() {});
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            // 这里可以根据需要返回 null 或者抛出自定义异常
            return null;
        }
    }

}
