package com.unlimited.sports.globox.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.social.entity.RallyParticipant;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 集结参与者数据访问层
 */
public interface RallyParticipantMapper extends BaseMapper<RallyParticipant> {

    /**
     * 根据集结帖子ID获取参与者列表（未取消的）
     *
     * @param rallyPostId 集结帖子ID
     * @return 参与者列表
     */
    @Select("SELECT * FROM rally_participant WHERE rally_post_id = #{rallyPostId} AND is_voluntarily_cancel = 0")
    List<RallyParticipant> getRallyParticipantList(Long rallyPostId);

    /**
     * 根据集结帖子ID和参与者ID获取申请信息
     *
     * @param rallyPostId   集结帖子ID
     * @param participantId 参与者ID
     * @return 集结参与者实体
     */
    RallyParticipant getRallyApplicationByRallyPostIdAndParticipantId(@Param("rallyPostId") Long rallyPostId, @Param("participantId") Long participantId);

    /**
     * 根据参与者ID获取集结申请列表（分页）
     *
     * @param participantId 参与者ID
     * @param offset        偏移量
     * @param pageSize      页面大小
     * @return 集结参与者实体列表（实际只包含rally_post_id字段）
     */
    @Select("SELECT rally_post_id FROM rally_participant WHERE participant_id = #{participantId} limit #{offset},#{pageSize} ")
    List<RallyParticipant> getRallyApplicationByParticipantId(@Param("participantId") Long participantId,@Param("offset")Integer offset,@Param("pageSize")Integer pageSize);
    
    /**
     * 根据参与者ID统计申请数量
     *
     * @param participantId 参与者ID
     * @return 申请数量
     */
    @Select("SELECT count(*) FROM rally_participant WHERE participant_id = #{participantId}")
    int countByParticipantId(@Param("participantId") Long participantId);
}
