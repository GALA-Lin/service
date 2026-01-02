package com.unlimited.sports.globox.notification.constants;

import java.util.HashMap;
import java.util.Map;

/**
 * 腾讯云IM推送错误码映射
 * 帮助快速定位问题根因
 */
public class TencentImErrorCode {

    private static final Map<Integer, String> ERROR_CODE_MAP = new HashMap<>();

    static {
        // 通用错误码
        ERROR_CODE_MAP.put(0, "成功");
        ERROR_CODE_MAP.put(100001, "请求中没有操作权限");
        ERROR_CODE_MAP.put(100002, "不支持该请求");
        ERROR_CODE_MAP.put(100003, "请求包体中包含非法值");
        ERROR_CODE_MAP.put(100004, "后台解析请求包时出错");
        ERROR_CODE_MAP.put(100005, "后台处理时出错");
        ERROR_CODE_MAP.put(100006, "服务请求超时");
        ERROR_CODE_MAP.put(100007, "需要 UserSig 鉴权");
        ERROR_CODE_MAP.put(100008, "请求的用户账号不存在");
        ERROR_CODE_MAP.put(100009, "操作权限不足");
        ERROR_CODE_MAP.put(100010, "需要应用管理员权限");

        // 推送相关错误码
        ERROR_CODE_MAP.put(90001, "JSON格式解析失败，请检查请求包是否符合JSON规范；或To_Account格式不正确");
        ERROR_CODE_MAP.put(90009, "请求需要App管理员权限");
        ERROR_CODE_MAP.put(90045, "未开通全员/标签/单推推送功能");
        ERROR_CODE_MAP.put(90057, "客户业务自定义标识DataId过长，目前支持最大64字节");
        ERROR_CODE_MAP.put(91000, "服务内部错误，请重试");

        // 其他常见错误码
        ERROR_CODE_MAP.put(60000, "账号不存在");
        ERROR_CODE_MAP.put(60001, "应用不存在");
        ERROR_CODE_MAP.put(60002, "SDK AppID不存在");
        ERROR_CODE_MAP.put(60003, "多个管理员账号存在");
        ERROR_CODE_MAP.put(60004, "账号格式不正确");
        ERROR_CODE_MAP.put(60005, "账号长度超限");
        ERROR_CODE_MAP.put(60006, "账号不允许");
        ERROR_CODE_MAP.put(60007, "账号格式不正确");
        ERROR_CODE_MAP.put(60008, "请求中缺少必填参数");
        ERROR_CODE_MAP.put(60009, "网络异常，请重试");
        ERROR_CODE_MAP.put(60010, "请求超时");
        ERROR_CODE_MAP.put(60011, "管理员权限不足");
    }

    /**
     * 根据错误码获取错误描述
     *
     * @param errorCode 错误码
     * @return 错误描述
     */
    public static String getErrorMessage(Integer errorCode) {
        if (errorCode == null) {
            return "未知错误";
        }
        return ERROR_CODE_MAP.getOrDefault(errorCode, "未知错误代码: " + errorCode);
    }

    /**
     * 检查是否是成功状态
     *
     * @param errorCode 错误码
     * @return true表示成功，false表示失败
     */
    public static boolean isSuccess(Integer errorCode) {
        return errorCode != null && errorCode == 0;
    }

    /**
     * 常见的可重试错误码
     *
     * @param errorCode 错误码
     * @return true表示可以重试
     */
    public static boolean isRetryable(Integer errorCode) {
        return errorCode == 60009 || errorCode == 60010 || errorCode == 91000;
    }
}
