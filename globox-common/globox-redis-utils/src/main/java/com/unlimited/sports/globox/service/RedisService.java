package com.unlimited.sports.globox.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.unlimited.sports.globox.utils.RedisJsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisService {

    @Qualifier("customRedisTemplate")
    @Autowired
    private RedisTemplate redisTemplate;


    

    //     基本操作

    /**
     * 设置有效时间 (时间单位默认秒)
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间 （可指定时间单位）
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取有效时间
     *
     * @param key Redis键
     * @return 有效时间
     */
    public long getExpire(final String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 判断 key是否存在
     *
     * @param key 键
     * @return true=存在；false=不存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }



    /**
     * 根据提供的键模式查找 Redis 中匹配的键
     *
     * @param pattern 要查找的键的模式
     * @return 键列表
     */
    public Collection<String> keys(final String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * 重命名key
     *
     * @param oldKey 原来key
     * @param newKey 新key
     */
    public void renameKey(String oldKey, String newKey) {
        redisTemplate.rename(oldKey, newKey);
    }

    /**
     * 删除单个数据
     *
     * @param key 缓存的键值
     * @return 是否成功 true=删除成功；false=删除失败
     */
    public boolean deleteObject(final String key) {
        return redisTemplate.delete(key);
    }



    /**
     * 删除多个数据
     *
     * @param keys 多个数据对应的缓存的键值列表
     * @return 删除的数量
     */
    public long deleteObjects(final List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        return redisTemplate.delete(keys);
    }

    //   操作String


    /**
     * 缓存存储数据,值存储JSON数据
     * 
     * @param key   键
     * @param value 值
     */
    public <T> void setCacheObject(final String key, final T value) {
        redisTemplate.opsForValue().set(key, value);
    }


    /**
     * 缓存存储数据,值存储JSON数据,设置过期时间
     * 
     * @param key      键
     * @param value    值
     * @param timeout  过期时间
     * @param timeUnit 过期时间单位
     */
    public <T> void setCacheObject(final String key, final T value, final Long timeout, final TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 缓存存储数据,值存储JSON数据,设置过期时间, 如果键存在则不存储
     * 
     * @param key      键
     * @param value    值
     * @param timeout  过期时间
     * @param timeUnit 过期时间单位
     * @return 设置成功 or 失败
     */
    public <T> Boolean setCacheObjectIfAbsent(final String key, final T value, final Long timeout,
            final TimeUnit timeUnit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout,
                timeUnit);
    }

    /**
     * 获得缓存的数据（将缓存的数据反序列化为指定类型返回）
     * 
     * @param key   键
     * @param clazz 类型
     * @return 对象
     */
    public <T> T getCacheObject(final String key, Class<T> clazz) {
        ValueOperations<String, T> valueOperations = redisTemplate.opsForValue();
        T t = valueOperations.get(key);
        if (t == null) {
            return null;
        }
        
        return RedisJsonUtil.string2Obj(RedisJsonUtil.obj2String(t), clazz);
    }

    /**
     * 获得缓存的数据 （将缓存的数据反序列化为指定类型返回，支持复杂的泛型）
     * 
     * @param key           键
     * @param typeReference 类型模版
     * @return 对象
     */
    public <T> T getCacheObject(final String key, TypeReference<T> typeReference) {
        ValueOperations<String, T> valueOperations = redisTemplate.opsForValue();
        T t = valueOperations.get(key);
        if (t == null) {
            return null;
        }
        return RedisJsonUtil.string2Obj(RedisJsonUtil.obj2String(t), typeReference);
    }


    /**
     * 对缓存中的数值进行自增操作
     *
     * @param key   键
     * @param delta 自增步长(可以为负数表示自减)
     * @return 自增后的结果
     */
    public Long incr(final String key, final long delta) {
        Long result = redisTemplate.opsForValue().increment(key, delta);
        return result;
    }
    //   操作List

    /**
     * 缓存list数据
     * 
     * @param key      键
     * @param dataList list数据
     * @return 添加redis列表的长度
     * @param <T> 对象类型
     */
    public <T> long setCacheList(final String key, final List<T> dataList) {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count == null ? 0 : count;
    }

    /**
     * 缓存list数据
     * 
     * @param key      键
     * @param dataList list数据
     * @return 添加redis列表的长度
     * @param <T> 对象类型
     */
    public <T> long setCacheList(final String key, final List<T> dataList, Long exp, TimeUnit timeUnit) {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        redisTemplate.expire(key, exp, timeUnit);
        return count == null ? 0 : count;
    }

    /**
     * list头插
     * 
     * @param key   键
     * @param value 值
     * @param <T>   值的类型
     */
    public <T> void leftPushForList(final String key, final T value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * list尾插
     * 
     * @param key   键
     * @param value 值
     * @param <T>   值的类型
     */
    public <T> void rightPushForList(final String key, final T value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 删除左侧第一个元素(头删)
     * 
     * @param key 键
     */
    public void leftPopForList(String key) {
        redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 删除右侧第一个元素(尾删)
     * 
     * @param key 键
     */
    public void rightPopForList(String key) {
        redisTemplate.opsForList().rightPop(key);
    }

    /**
     * 移除redis List第一个匹配的元素
     * 
     * @param key   键
     * @param value 值
     * @param <T>   类型信息
     */
    public <T> void removeForList(final String key, T value) {
        redisTemplate.opsForList().remove(key, 1L, value);
    }

    /**
     * 移除redis List 所有匹配的元素
     * 
     * @param key   键
     * @param value 值
     * @param <T>   类型信息
     */
    public <T> void removeAllForList(final String key, T value) {
        redisTemplate.opsForList().remove(key, 0, value);
    }

    /**
     * 移除redis List的所有元素
     * 
     * @param key 键
     * @param <T> 类型信息
     */
    public <T> void removeForAllList(final String key) {
        redisTemplate.opsForList().trim(key, -1, 0);
    }

    /**
     * 修改指定下标的元素
     * 
     * @param key   键
     * @param index 下标
     * @param value 新值
     * @param <T>   类型信息
     */
    public <T> void setElementAtIndex(final String key, final int index, final T value) {
        redisTemplate.opsForList().set(key, index, value);
    }

    /**
     * 获取缓存的List对象
     * 
     * @param key   键
     * @param clazz 类型
     * @return List
     * @param <T> 类型信息
     */
    public <T> List<T> getCacheList(final String key, Class<T> clazz) {
        List list = redisTemplate.opsForList().range(key, 0, -1);
        return RedisJsonUtil.string2List(RedisJsonUtil.obj2String(list), clazz);
    }

    /**
     * 获取缓存的List对象,支持复杂的泛型嵌套
     * 
     * @param key           键
     * @param typeReference 类型模版
     * @return List
     * @param <T> 类型信息
     */
    public <T> List<T> getCacheList(final String key, TypeReference<List<T>> typeReference) {
        List list = redisTemplate.opsForList().range(key, 0, -1);
        List<T> res = RedisJsonUtil.string2Obj(RedisJsonUtil.obj2String(list),
                typeReference);
        return res;
    }

    /**
     * 根据范围获取List
     *
     * @param key   key
     * @param start 开始位置
     * @param end   结束位置
     * @param clazz 类信息
     * @return List列表
     * @param <T> 类型
     */
    public <T> List<T> getCacheListByRange(final String key, long start, long end, Class<T> clazz) {
        List range = redisTemplate.opsForList().range(key, start, end);
        return RedisJsonUtil.string2List(RedisJsonUtil.obj2String(range), clazz);
    }

    /**
     * 根据范围获取List（支持复杂的泛型嵌套 ）
     *
     * @param key           key
     * @param start         开始
     * @param end           结果
     * @param typeReference 类型模板
     * @return list列表
     * @param <T> 类型信息
     */
    public <T> List<T> getCacheListByRange(final String key, long start, long end,
            TypeReference<List<T>> typeReference) {
        List range = redisTemplate.opsForList().range(key, start, end);
        return RedisJsonUtil.string2Obj(RedisJsonUtil.obj2String(range), typeReference);
    }

    /**
     * 获取指定列表长度
     * 
     * @param key key信息
     * @return 列表长度
     */
    public long getCacheListSize(final String key) {
        Long size = redisTemplate.opsForList().size(key);
        return size == null ? 0L : size;
    }

    /**
     * 获取元素在列表中的位置
     * 
     * @param key   键
     * @param value 值
     * @return 位置
     * @param <T> 类型信息
     */
    public <T> Long indexOfForList(final String key, T value) {
        return redisTemplate.opsForList().indexOf(key, value);
    }

    //  操作Set

    /**
     * set添加元素
     * 
     * @param key    键
     * @param member 元素信息
     */
    public void addMember(final String key, Object... member) {
        redisTemplate.opsForSet().add(key, member);
    }

    /**
     * 删除元素
     * 
     * @param key    键
     * @param member 元素信息
     */
    public void deleteMember(final String key, Object... member) {
        redisTemplate.opsForSet().remove(key, member);
    }

    /**
     * 获取set数据(所有成员)
     * 
     * @param key 键
     * @return set数据
     */
    public Set<String> getSetMembers(final String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 设置 set 数据
     * 
     * @param key    键
     * @param values 值集合
     */
    public void setSetMembers(final String key, Set<String> values) {
        if (values != null && !values.isEmpty()) {
            redisTemplate.opsForSet().add(key, values.toArray(new String[0]));
        }
    }

    // 操作ZSet

    /**
     * 添加元素到有序集合
     * 
     * @param key    键
     * @param member 成员
     * @param score  分数
     */
    public void addZSetMember(final String key, Object member, double score) {
        redisTemplate.opsForZSet().add(key, member, score);
    }

    /**
     * 批量添加元素到有序集合
     *
     * @param key     键
     * @param members 成员集合（Map结构，key为成员对象，value为对应的分数）
     */
    public void batchAddZSetMembers(final String key, Map<Object, Double> members) {
        if (members == null || members.isEmpty()) {
            return; // 空集合直接返回，避免无效操作
        }

        // 转换为Redis所需的TypedTuple集合
        Set<ZSetOperations.TypedTuple<Object>> tuples = new HashSet<>(members.size());
        for (Map.Entry<Object, Double> entry : members.entrySet()) {
            tuples.add(new DefaultTypedTuple<>(entry.getKey(), entry.getValue()));
        }

        // 批量添加
        redisTemplate.opsForZSet().add(key, tuples);
    }

    /**
     * 按分数范围获取元素(正序，不带分页)
     * 
     * @param key           键
     * @param min           最小分数
     * @param max           最大分数
     * @param typeReference 类型模板
     * @return 元素集合
     */
    public <T> Set<T> getZSetByScoreRange(final String key, double min, double max,
            TypeReference<Set<T>> typeReference) {
        Set data = redisTemplate.opsForZSet().rangeByScore(key, min, max);
        return RedisJsonUtil.string2Obj(RedisJsonUtil.obj2String(data), typeReference);
    }

    /**
     * 按分数范围获取元素(正序，带分页)
     * 
     * @param key           键
     * @param min           最小分数
     * @param max           最大分数
     * @param offset        偏移量
     * @param count         数量
     * @param typeReference 类型模板
     * @return 元素集合
     */
    public <T> Set<T> getZSetByScoreRange(final String key, double min, double max, long offset, long count,
            TypeReference<Set<T>> typeReference) {
        Set data = redisTemplate.opsForZSet().rangeByScore(key, min, max, offset, count);
        return RedisJsonUtil.string2Obj(RedisJsonUtil.obj2String(data), typeReference);
    }

    /**
     * 按分数范围获取元素(倒序，不带分页)
     * 
     * @param key           键
     * @param min           最小分数
     * @param max           最大分数
     * @param typeReference 类型模板
     * @return 元素集合
     */
    public <T> Set<T> getZSetByScoreRangeDesc(final String key, double min, double max,
            TypeReference<Set<T>> typeReference) {
        Set data = redisTemplate.opsForZSet().reverseRangeByScore(key, min, max);
        return RedisJsonUtil.string2Obj(RedisJsonUtil.obj2String(data), typeReference);
    }

    /**
     * 按分数范围获取元素(倒序，带分页)
     * 
     * @param key           键
     * @param min           最小分数
     * @param max           最大分数
     * @param offset        偏移量
     * @param count         数量
     * @param typeReference 类型模板
     * @return 元素集合
     */
    public <T> Set<T> getZSetByScoreRangeDesc(final String key, double min, double max, long offset, long count,
            TypeReference<Set<T>> typeReference) {
        Set data = redisTemplate.opsForZSet().reverseRangeByScore(key, min, max, offset, count);
        return RedisJsonUtil.string2Obj(RedisJsonUtil.obj2String(data), typeReference);
    }

    /**
     * 按索引范围获取元素(正序)
     * 
     * @param key           键
     * @param start         开始索引
     * @param end           结束索引
     * @param typeReference 类型模板
     * @return 元素集合
     */
    public <T> Set<T> getZSetByIndexRange(final String key, long start, long end, TypeReference<Set<T>> typeReference) {
        Set data = redisTemplate.opsForZSet().range(key, start, end);
        return RedisJsonUtil.string2Obj(RedisJsonUtil.obj2String(data), typeReference);
    }

    /**
     * 按索引范围获取元素(倒序)
     * 
     * @param key           键
     * @param start         开始索引
     * @param end           结束索引
     * @param typeReference 类型模板
     * @return 元素集合
     */
    public <T> Set<T> getZSetByIndexRangeDesc(final String key, long start, long end,
            TypeReference<Set<T>> typeReference) {
        Set data = redisTemplate.opsForZSet().reverseRange(key, start, end);
        return RedisJsonUtil.string2Obj(RedisJsonUtil.obj2String(data), typeReference);
    }

    /**
     * 获取元素分数
     * 
     * @param key    键
     * @param member 成员
     * @return 分数
     */
    public Double getZSetScore(final String key, Object member) {
        return redisTemplate.opsForZSet().score(key, member);
    }

    /**
     * 统计分数范围内元素数量
     * 
     * @param key 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 元素数量
     */
    public Long getZSetSizeByScoreRange(final String key, double min, double max) {
        return redisTemplate.opsForZSet().count(key, min, max);
    }

    /**
     * 获取有序集合大小
     * 
     * @param key 键
     * @return 集合大小
     */
    public Long getZSetSize(final String key) {
        return redisTemplate.opsForZSet().size(key);
    }

    /**
     * 获取有序集合中指定分数范围内的元素数量
     *
     * @param key 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 分数范围内的元素数量
     */
    public Long countZSetByScoreRange(final String key, final double min, final double max) {
        return redisTemplate.opsForZSet().count(key, min, max);
    }

    /**
     * 增加元素分数
     * 
     * @param key    键
     * @param member 成员
     * @param delta  增量
     * @return 新分数
     */
    public Double incrementZSetScore(final String key, Object member, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, member, delta);
    }

    /**
     * 获取元素排名(正序)
     * 
     * @param key    键
     * @param member 成员
     * @return 排名(从0开始)
     */
    public Long getZSetRank(final String key, Object member) {
        return redisTemplate.opsForZSet().rank(key, member);
    }

    /**
     * 获取元素排名(倒序)
     * 
     * @param key    键
     * @param member 成员
     * @return 排名(从0开始)
     */
    public Long getZSetReverseRank(final String key, Object member) {
        return redisTemplate.opsForZSet().reverseRank(key, member);
    }

    /**
     * 根据排名范围删除元素
     * 
     * @param key   键
     * @param start 开始排名
     * @param end   结束排名
     * @return 删除数量
     */
    public Long removeZSetRangeByRank(final String key, long start, long end) {
        return redisTemplate.opsForZSet().removeRange(key, start, end);
    }

    /**
     * 获取Zset全部
     * 
     * @param key           键
     * @param typeReference 类型模版
     * @return
     */
    public Set<String> getCacheZSet(String key, TypeReference<Set<String>> typeReference) {
        Set data = redisTemplate.opsForZSet().range(key, 0, -1);
        return RedisJsonUtil.string2Obj(RedisJsonUtil.obj2String(data), typeReference);
    }

    /**
     * 是否有某个值
     * 
     * @param key
     * @return
     */
    public boolean zsetContainsKey(String key) {
        return redisTemplate.opsForZSet().getOperations().hasKey(key);
    }
    //  操作Hash

    /**
     * 缓存map数据
     * 
     * @param key     键
     * @param dataMap map数据
     * @param <T>     类型信息
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap) {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * 批量设置 Hash 数据（重载方法）
     * 
     * @param key     键
     * @param dataMap map数据
     * @param <T>     类型信息
     */
    public <T> void setHashAll(final String key, final Map<String, T> dataMap) {
        if (dataMap != null && !dataMap.isEmpty()) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * 获取 Hash 中的单个数据（支持泛型）
     * 
     * @param key   Redis键
     * @param hKey  Hash键
     * @param clazz 类型
     * @return Hash中的对象
     * @param <T> 对象类型
     */
    public <T> T getHashValue(final String key, final String hKey, Class<T> clazz) {
        Object value = redisTemplate.opsForHash().get(key, hKey);
        if (value == null) {
            return null;
        }
        return RedisJsonUtil.string2Obj(RedisJsonUtil.obj2String(value), clazz);
    }

    /**
     * 删除 Hash 中的某个字段
     * 
     * @param key  键
     * @param hKey Hash键
     * @return 结果
     */
    public Long deleteHashField(final String key, final String hKey) {
        return redisTemplate.opsForHash().delete(key, hKey);
    }

    /**
     * 往Hash中插入单个数据
     * 
     * @param key   键
     * @param hKey  hash键
     * @param value 值
     * @param <T>   类型信息
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value) {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 删除Hash中的某条数据
     * 
     * @param key  键
     * @param hKey Hash键
     * @return 结果
     */
    public void deleteCacheMapValue(final String key, final String hKey) {
        redisTemplate.opsForHash().delete(key, hKey);
    }

    /**
     * 获取缓存的map数据（支持复杂的泛型嵌套）
     * 
     * @param key           key
     * @param typeReference 类型模板
     * @return hash对应的map
     * @param <T> 对象类型
     */
    public <T> Map<String, T> getCacheMap(final String key, TypeReference<Map<String, T>> typeReference) {
        Map data = redisTemplate.opsForHash().entries(key);
        return RedisJsonUtil.string2Obj(RedisJsonUtil.obj2String(data), typeReference);
    }

    /**
     * 获取Hash中的单个数据
     * 
     * @param key  Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     * @param <T> 对象类型
     */
    public <T> T getCacheMapValue(final String key, final String hKey, Class<T> clazz) {
        Object value = redisTemplate.opsForHash().get(key, hKey);
        if (value == null)
            return null;
        return RedisJsonUtil.string2Obj(RedisJsonUtil.obj2String(value), clazz);
    }

    /**
     * 获取Hash中的多个数据
     *
     * @param key           Redis键
     * @param hKeys         Hash键集合
     * @param typeReference 对象模板
     * @return 获取的多个数据的集合
     * @param <T> 对象类型
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<?> hKeys,
            TypeReference<List<T>> typeReference) {
        List data = redisTemplate.opsForHash().multiGet(key, hKeys);
        return RedisJsonUtil.string2Obj(RedisJsonUtil.obj2String(data), typeReference);
    }

    /**
     * 执行Pipeline操作（高性能批量操作）
     * 
     * @param callback Pipeline操作回调
     * @return 执行结果列表
     */
    public List<Object> executePipelined(RedisCallback<?> callback) {
        return redisTemplate.executePipelined(callback);
    }

    /**
     * 对哈希表中指定字段的数值进行自增
     *
     * @param key     哈希表的键
     * @param hashKey 哈希表中的字段
     * @param delta   自增步长（可以为负数表示自减）
     * @return 自增后的结果
     */
    public Long hincrHashField(final String key, final String hashKey, final long delta) {
        return redisTemplate.opsForHash().increment(key, hashKey, delta);
    }


}
