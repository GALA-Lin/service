package com.unlimited.sports.globox.model.social.entity;

public enum TencentCloudImApiEnum {
 
    /**
     * 账号管理
     */
    ACCOUNT_IMPORT("导入单个账号", "v4/im_open_login_svc/account_import"),
    MULTI_ACCOUNT_IMPORT("导入多个账号", "v4/im_open_login_svc/multiaccount_import"),
    ACCOUNT_DELETE("删除账号", "v4/im_open_login_svc/account_delete"),
    ACCOUNT_CHECK("查询账号", "v4/im_open_login_svc/account_check"),
    KICK("失效账号登录状态", "v4/im_open_login_svc/kick"),
    QUERY_ONLINE_STATE("查询账号在线状态", " v4/openim/query_online_status"),
 
    /**
     * 单聊消息
     */
    SEND_MSG("单发单聊消息", "v4/openim/sendmsg"),
    BATCH_SEND_MSG("批量发单聊消息", "v4/openim/batchsendmsg"),
    IMPORT_MSG("导入单聊消息", "v4/openim/importmsg"),
    ADMIN_GET_ROAM_MSG("查询单聊消息", "v4/openim/admin_getroammsg"),
    ADMIN_MSG_WITH_DRAW("撤回单聊消息", "v4/openim/admin_msgwithdraw"),
    ADMIN_SET_MSG_READ("设置单聊消息已读", "v4/openim/admin_set_msg_read"),
    GET_C2C_UNREAD_MSG_NUM("查询单聊未读消息计数", "v4/openim/get_c2c_unread_msg_num"),
    MODIFY_C2C_MSG("修改单聊历史消息", "v4/openim/modify_c2c_msg"),
 
    /**
     * 全员推送
     */
    IM_PUSH("全员推送", "v4/all_member_push/im_push"),
    IM_SET_ATTR_NAME("设置应用属性名称", "v4/all_member_push/im_set_attr_name"),
    IM_GET_ATTR_NAME("获取应用属性名称", "v4/all_member_push/im_get_attr_name"),
    IM_GET_ATTR("获取用户属性", "v4/all_member_push/im_get_attr"),
    IM_SET_ATTR("设置用户属性", "v4/all_member_push/im_set_attr"),
    IM_REMOVE_ATTR("删除用户属性", "v4/all_member_push/im_remove_attr"),
    IM_GET_TAG("获取用户标签", "v4/all_member_push/im_get_tag"),
    IM_ADD_TAG("添加用户标签", "v4/all_member_push/im_add_tag"),
    IM_REMOVE_TAG("删除用户标签", "v4/all_member_push/im_remove_tag"),
    IM_REMOVE_ADD_TAGS("删除用户所有标签", "v4/all_member_push/im_remove_all_tags"),
 
    /**
     * 资料管理
     */
    PORTRAIT_SET("设置资料", "v4/profile/portrait_set"),
    PORTRAIT_GET("拉取资料", "v4/profile/portrait_get"),
 
    /**
     * 关系链管理
     */
    FRIEND_ADD("添加好友", "v4/sns/friend_add"),
    FRIEND_IMPORT("导入好友", "v4/sns/friend_import"),
    FRIEND_UPDATE("更新好友", "v4/sns/friend_update"),
    FRIEND_DELETE("删除好友", "v4/sns/friend_delete"),
    FRIEND_DELETE_ALL("删除所有好友", "v4/sns/friend_delete_all"),
    FRIEND_CHECK("校验好友", "v4/sns/friend_check"),
    FRIEND_GET("拉取好友", "v4/sns/friend_get"),
    FRIEND_GET_LIST("拉取指定好友", "v4/sns/friend_get_list"),
    BLACK_LIST_ADD("添加黑名单", "v4/sns/black_list_add"),
    BLACK_LIST_DELETE("删除黑名单", "v4/sns/black_list_delete"),
    BLACK_LIST_GET("拉取黑名单", "v4/sns/black_list_get"),
    BLACK_LIST_CHECK("校验黑名单", "v4/sns/black_list_check"),
    GROUP_ADD("添加分组", "v4/sns/group_add"),
    GROUP_DELETE("删除分组", "v4/sns/group_delete"),
    GROUP_GET("拉取分组", "v4/sns/group_get"),
 
    /**
     * 最近联系人
     */
    CONTACT_GET_LIST("拉取会话列表", "v4/recentcontact/get_list"),
    CONTACT_DELETE("删除单个会话", "v4/recentcontact/delete"),
    CREATE_CONTACT_GROUP("创建会话分组数据", "v4/recentcontact/create_contact_group"),
    DEL_CONTACT_GROUP("删除会话分组数据", "v4/recentcontact/del_contact_group"),
    UPDATE_CONTACT_GROUP("更新会话分组数据", "v4/recentcontact/update_contact_group"),
    SEARCH_CONTACT_GROUP("搜索会话分组标记数据", "v4/recentcontact/search_contact_group"),
    MARK_CONTACT("创建或更新会话标记数据", "v4/recentcontact/mark_contact"),
    GET_CONTACT_GROUP("拉取会话分组标记数据", "v4/recentcontact/get_contact_group"),
 
    /**
     * 群组管理
     */
    GET_APPID_GROUP_LIST("获取APP中的所有群组", "v4/group_open_http_svc/get_appid_group_list"),
    CREATE_GROUP("创建群组", "v4/group_open_http_svc/create_group"),
    GET_GROUP_INFO("获取群详细资料", "v4/group_open_http_svc/get_group_info"),
    GET_GROUP_MEMBER_INFO("获取群成员详细资料", "v4/group_open_http_svc/get_group_member_info"),
    MODIFY_GROUP_BASE_INFO("修改群基础资料", "v4/group_open_http_svc/modify_group_base_info"),
    ADD_GROUP_MEMBER("增加群成员", "v4/group_open_http_svc/add_group_member"),
    DELETE_GROUP_MEMBER("删除群成员", "v4/group_open_http_svc/delete_group_member"),
    MODIFY_GROUP_MEMBER_INFO("修改群成员资料", "v4/group_open_http_svc/modify_group_member_info"),
    DESTROY_GROUP("解散群组", "v4/group_open_http_svc/destroy_group "),
    GET_JOINED_GROUP_LIST("获取用户所加入的群组", "v4/group_open_http_svc/get_joined_group_list"),
    GET_ROLE_IN_GROUP("查询用户在群组中的身份", "v4/group_open_http_svc/get_role_in_group"),
    FORBID_SEND_MSG("批量禁言和取消禁言", "v4/group_open_http_svc/forbid_send_msg"),
    GET_GROUP_MUTED_ACCOUNT("获取被禁言群成员列表", "v4/group_open_http_svc/get_group_muted_account"),
    SEND_GROUP_MSG("在群组中发送普通消息", "v4/group_open_http_svc/send_group_msg"),
    SEND_GROUP_SYSTEM_NOTIFICATION("在群组中发送系统通知", "v4/group_open_http_svc/send_group_system_notification"),
    GROUP_MSG_RECALL("撤回群消息", "v4/group_open_http_svc/group_msg_recall"),
    CHANGE_GROUP_OWNER("转让群主", "v4/group_open_http_svc/change_group_owner"),
    IMPORT_GROUP("导入群基础资料", "v4/group_open_http_svc/import_group"),
    IMPORT_GROUP_MSG("导入群消息", "v4/group_open_http_svc/import_group_msg "),
    IMPORT_GROUP_MEMBER("导入群成员", "v4/group_open_http_svc/import_group_member"),
    SET_UNREAD_MSG_NUM("设置成员未读消息计数", "v4/group_open_http_svc/set_unread_msg_num"),
    DELETE_GROUP_MSG_BY_SENDER("删除指定用户发送的消息", "v4/group_open_http_svc/delete_group_msg_by_sender"),
    GROUP_MSG_GET_SIMPLE("拉取群历史消息", "v4/group_open_http_svc/group_msg_get_simple"),
    GET_ONLINE_MEMBER_NUM("获取直播群在线人数", "v4/group_open_http_svc/get_online_member_num"),
    GET_MEMBERS("获取直播群在线成员列表", "v4/group_open_avchatroom_http_svc/get_members"),
    MODIFY_USER_INFO("设置直播群成员标记", "v4/group_open_avchatroom_http_svc/modify_user_info"),
    GET_GROUP_ATTR("获取群自定义属性", "v4/group_open_attr_http_svc/get_group_attr"),
    GET_GROUP_BAN_MEMBER("获取封禁群成员列表", "v4/group_open_http_svc/get_group_ban_member"),
    BAN_GROUP_MEMBER("群成员封禁", "v4/group_open_http_svc/ban_group_member"),
    UNBAN_GROUP_MEMBER("群成员解封", "v4/group_open_http_svc/unban_group_member"),
    MODIFY_GROUP_ATTR("修改群自定义属性", "v4/group_open_http_svc/modify_group_attr"),
    CLEAR_GROUP_ATTR("清空群自定义属性", "v4/group_open_http_svc/clear_group_attr"),
    SET_GROUP_ATTR("重置群自定义属性", "v4/group_open_http_svc/set_group_attr"),
    MODIFY_GROUP_MSG("修改群聊历史消息", "v4/openim/modify_group_msg"),
    SEND_BROADCAST_MSG("直播群广播消息", "v4/group_open_http_svc/send_broadcast_msg"),
    GET_GROUP_COUNTER("获取群计数器", "v4/group_open_http_svc/get_group_counter"),
    UPDATE_GROUP_COUNTER("更新群计数器", "v4/group_open_http_svc/update_group_counter"),
    DELETE_GROUP_COUNTER("删除群计数器", "v4/group_open_http_svc/delete_group_counter"),
 
    /**
     * 全局禁言管理
     */
    SET_NO_SPEAKING("设置全局禁言", " v4/openconfigsvr/setnospeaking"),
    GET_NO_SPEAKING("查询全局禁言", " v4/openconfigsvr/getnospeaking"),
 
    /**
     * 运营管理
     */
    GET_APP_INFO("拉取运营数据", "v4/openconfigsvr/getappinfo"),
    GET_HISTORY("下载消息记录", "v4/open_msg_svc/get_history"),
    GET_IP_LIST("获取服务器IP地址", "v4/ConfigSvc/GetIPList"),
    FORBID_ILLEGAL_OBJECT("聊天文件封禁", "v4/im_cos_msg/forbid_illegal_object"),
    ALLOW_BANNED_OBJECT("聊天文件解封", "v4/im_cos_msg/allow_banned_object"),
    GET_COS_SIG("聊天文件签名", "v4/im_cos_msg/get_cos_sig");
 
    private final String description;
 
    private final String url;
 
    TencentCloudImApiEnum(String description, String url) {
        this.description = description;
        this.url = url;
    }
 
    public String description() {
        return description;
    }
 
    public String getUrl() {
        return url;
    }
}