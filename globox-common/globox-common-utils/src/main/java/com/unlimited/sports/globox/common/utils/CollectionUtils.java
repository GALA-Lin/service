package com.unlimited.sports.globox.common.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * 集合工具类
 * 提供通用的集合操作方法
 */
public class CollectionUtils {

    /**
     * 按照连续性分组
     * 根据给定的连续性判断函数，将列表分成多个连续的子列表
     *
     * @param list               要分组的列表
     * @param continuousPredicate 连续性判断函数 (prev, curr) -> boolean，返回true表示连续
     * @param <T>                列表元素类型
     * @return 分组后的列表，每个子列表包含连续的元素
     */
    public static <T> List<List<T>> splitByContinuity(
            List<T> list,
            BiPredicate<T, T> continuousPredicate) {

        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<T>> result = new ArrayList<>();
        List<T> currentGroup = new ArrayList<>();

        T prev = null;
        for (T curr : list) {
            if (prev != null && !continuousPredicate.test(prev, curr)) {
                // 不连续，保存当前组，开始新组
                result.add(currentGroup);
                currentGroup = new ArrayList<>();
            }
            currentGroup.add(curr);
            prev = curr;
        }

        // 添加最后一组
        result.add(currentGroup);

        return result;
    }

}
