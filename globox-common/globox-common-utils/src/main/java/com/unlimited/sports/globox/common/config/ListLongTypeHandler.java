package com.unlimited.sports.globox.common.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Mybatis 类型处理器：List<Long> 与 VARCHAR 之间的转换
 */

@MappedTypes({List.class})
@MappedJdbcTypes({JdbcType.VARCHAR})
public class ListLongTypeHandler extends BaseTypeHandler<List<Long>> {


    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 从数据库读取JSON字符串 → 转换为List<Long>
    @Override
    public List<Long> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String jsonStr = rs.getString(columnName);
        return parseJsonToList(jsonStr);
    }

    @Override
    public List<Long> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String jsonStr = rs.getString(columnIndex);
        return parseJsonToList(jsonStr);
    }

    @Override
    public List<Long> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String jsonStr = cs.getString(columnIndex);
        return parseJsonToList(jsonStr);
    }

    // 从Java的List<Long> → 序列化为JSON字符串存入数据库
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Long> parameter, JdbcType jdbcType) throws SQLException {
        try {
            String jsonStr = OBJECT_MAPPER.writeValueAsString(parameter);
            ps.setString(i, jsonStr);
        } catch (Exception e) {
            throw new SQLException("转换List<Long>为JSON字符串失败", e);
        }
    }

    // 解析JSON字符串为List<Long>
    private List<Long> parseJsonToList(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(jsonStr, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("解析JSON字符串为List<Long>失败", e);
        }
    }
}
