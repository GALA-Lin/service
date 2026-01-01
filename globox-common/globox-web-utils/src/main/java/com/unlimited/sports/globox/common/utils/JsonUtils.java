package com.unlimited.sports.globox.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


/**
 * JSON 工具类
 *
 * @author dk
 * @since 2025/12/17 22:06
 */
@Log4j2
@Component
@AllArgsConstructor
public class JsonUtils {

    private ObjectMapper mapper;

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
