package com.unlimited.sports.globox.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON工具类
 * 提供:
 * 1. 对象转JSON
 * 2. JSON转对象
 */
@Slf4j
public class RedisJsonUtil {

    /**
     * 对象转换器
     */
    private static ObjectMapper OBJECT_MAPPER;


    public static final String DATE_STANDARD_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * init...
     */
    static {
        OBJECT_MAPPER = JsonMapper.builder().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false)
                // 不适用默认的dateTime进行序列化,使用JSR310的LocalDateTimeSerializer
                .configure(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS, false)
                .configure(MapperFeature.USE_ANNOTATIONS, true)
                // 这是序列化LocalDateTIme和LocalDate的必要配置
                .addModule(new JavaTimeModule())
                .addModule(new SimpleModule()
                        .addSerializer(LocalDateTime.class,
                                new LocalDateTimeSerializer(
                                        DateTimeFormatter.ofPattern(DATE_STANDARD_FORMAT)))
                        .addDeserializer(LocalDateTime.class,
                                new LocalDateTimeDeserializer(
                                        DateTimeFormatter.ofPattern(DATE_STANDARD_FORMAT))))
                // 所有的日期格式都统一为以下的样式，即yyyy-MM-dd HH:mm:ss
                .defaultDateFormat(new SimpleDateFormat(DATE_STANDARD_FORMAT))
                // 只针对非空的值进行序列化
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();
    }

    /**
     * 禁用构造
     */
    private RedisJsonUtil() {
    }

    /**
     * 对象转Json格式字符串
     * 
     * @param obj 对象
     * @return Json格式字符串
     * @param <T> 对象类型
     */
    public static <T> String obj2String(T obj) {
        if (obj == null) {
            return null;
        }
        try {
            return obj instanceof String ? (String) obj : OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Parse Object to String error : {}", e.getMessage());
            return null;
        }
    }

    /**
     * 字符串转换为自定义对象
     * 
     * @param str   要转换的字符串
     * @param clazz 自定义对象的class对象
     * @return 自定义对象
     * @param <T> 对象类型
     */
    public static <T> T string2Obj(String str, Class<T> clazz) {
        if (str == null || str.isEmpty() || clazz == null) {
            return null;
        }
        try {
            return clazz.equals(String.class) ? (T) str : OBJECT_MAPPER.readValue(str, clazz);
        } catch (Exception e) {
            log.warn("Parse String to Object error : {}", e.getMessage());
            return null;
        }
    }

    /**
     * 字符串转换为自定义对象,支持复杂的泛型嵌套
     *
     * @param str          json字符串
     * @param valueTypeRef 对象模板信息
     *                     使用如 new TypeReference<List<Map<String,TestObj>>>() {}
     * @return 对象类对应的对象
     * @param <T> 对象类
     */
    public static <T> T string2Obj(String str, TypeReference<T> valueTypeRef) {
        if (StringUtils.isEmpty(str) || valueTypeRef == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(str, valueTypeRef);
        } catch (Exception e) {
            log.warn("Parse String to Object error : {}", e.getMessage());
            return null;
        }
    }

    /**
     * 字符串转换为自定义字段转为list,支持List嵌套简单对象
     * 
     * @param str   json字符串
     * @param clazz 对象类
     * @return 对象列表
     * @param <T> 对象类型
     */
    public static <T> List<T> string2List(String str, Class<T> clazz) {
        if (str == null || str.isEmpty() || clazz == null) {
            return null;
        }
        JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructParametricType(List.class, clazz);
        try {
            return OBJECT_MAPPER.readValue(str, javaType);
        } catch (IOException e) {
            log.warn("Parse String to List error : {}", e.getMessage());
            return null;
        }
    }

    /**
     * 字符串转换为自定义字段转为map,支持Map嵌套简单对象
     * 
     * @param str        str 字符串信息
     * @param valueClass valueClass value的类别
     * @return map对象
     * @param <T> value 的类型
     */
    public static <T> Map<String, T> string2Map(String str, Class<T> valueClass) {
        if (str == null || str.isEmpty() || valueClass == null) {
            return null;
        }
        JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructMapType(LinkedHashMap.class,
                String.class, valueClass);
        try {
            return OBJECT_MAPPER.readValue(str, javaType);
        } catch (JsonProcessingException e) {
            log.warn("Parse String to Map error : {}", e.getMessage());
            return null;
        }
    }
}
