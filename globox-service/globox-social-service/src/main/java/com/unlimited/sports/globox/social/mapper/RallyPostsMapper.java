package com.unlimited.sports.globox.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.social.dto.RallyQueryDto;
import com.unlimited.sports.globox.model.social.entity.RallyApplication;
import com.unlimited.sports.globox.model.social.entity.RallyPosts;

import com.unlimited.sports.globox.model.social.vo.RallyPostsVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 集结帖数据访问层
 **/
@Mapper
public interface RallyPostsMapper extends BaseMapper<RallyPosts> {
    /**
     * 获取集结帖列表
     *
      * @param area 查询条件
     * @param offset        偏移量
     * @param pageSize      页面大小
     * @return 集结帖列表
     */
    List<RallyPosts> getRallyPostsList(
            @Param("area") List<String> area,
            @Param("timeRange") Integer timeRange,
            @Param("genderLimit") Integer genderLimit,
            @Param("ntrpMin") Double ntrpMin,
            @Param("ntrpMax") Double ntrpMax,
            @Param("activityType") Integer activityType,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);

    /**
     * 根据帖子ID查询集结帖
     * @param postId 帖子ID
     * @return 集结帖实体
     */
    @Select("SELECT * FROM rally_posts WHERE rally_post_id = #{postId}")
    RallyPosts selectByPostId(Long postId);

    /**
     * 获取我的活动列表
     *
     * @param offset   偏移量
     * @param pageSize 页面大小
     * @param userId   用户ID
     * @return 我的活动列表
     */
    @Select("SELECT * FROM rally_posts WHERE initiator_id = #{userId} ORDER BY rally_created_at DESC LIMIT #{offset}, #{pageSize}")
    List<RallyPosts> myActivities(@Param("offset") Integer offset,
                                    @Param("pageSize") Integer pageSize,
                                    @Param("userId")Long userId);

    /**
     * 统计我的活动数量
     *
     * @param userId 用户ID
     * @return 我的活动数量
     */
    @Select("select count(*) from rally_posts where initiator_id = #{userId}")
    int myActivitiesCount(@Param("userId")Long userId);


    Long countRallyPostsList(@Param("area") List<String> area,
                         @Param("timeRange") Integer timeRange,
                         @Param("genderLimit") Integer genderLimit,
                         @Param("ntrpMin") Double ntrpMin,
                         @Param("ntrpMax") Double ntrpMax,
                         @Param("activityType") Integer activityType);

    @Select("SELECT * FROM rally_posts WHERE initiator_id = #{userId} AND rally_status = 0 ORDER BY rally_created_at DESC LIMIT #{offset}, #{pageSize}")
    List<RallyPosts> getPublishedActivitiesByUser(@Param("userId") Long userId,
                                                  @Param("offset") int offset,
                                                  @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM rally_posts WHERE initiator_id = #{userId} AND rally_status = 0")
    int countPublishedActivitiesByUser(@Param("userId") Long userId);

    @Select({
            "SELECT * FROM rally_posts WHERE rally_status = 2",
            "AND (initiator_id = #{userId} OR rally_post_id IN (",
            "  SELECT rally_post_id FROM rally_participant WHERE participant_id = #{userId}",
            "))",
            "ORDER BY rally_created_at DESC LIMIT #{offset}, #{pageSize}"
    })
    List<RallyPosts> getCancelledActivities(@Param("userId") Long userId,
                                            @Param("offset") int offset,
                                            @Param("pageSize") int pageSize);

    @Select({
            "SELECT COUNT(*) FROM rally_posts WHERE rally_status = 4",
            "AND (initiator_id = #{userId} OR rally_post_id IN (",
            "  SELECT rally_post_id FROM rally_participant WHERE participant_id = #{userId}",
            "))"
    })
    int countCancelledActivities(@Param("userId") Long userId);

    /**
     * 原子扣减剩余人数，避免超额通过
     *
     * @param postId 帖子ID
     * @return 影响行数
     */
    int decrementRemainingPeopleIfAvailable(@Param("postId") Long postId);
    /**
     * 更新剩余人数（基于数据库实时计算）
     * 使用子查询确保数据一致性
     *
     * @param postId 帖子ID
     * @param totalPeople 总人数
     * @return 影响行数
     */
    @Update("UPDATE rally_posts " +
            "SET rally_remaining_people = #{totalPeople} - " +
            "    (SELECT COUNT(*) FROM rally_participant " +
            "     WHERE rally_post_id = #{postId} AND is_initiator = 0), " +
            "    rally_total_people = #{totalPeople}, " +
            "    rally_updated_at = NOW() " +
            "WHERE rally_post_id = #{postId}")
    int updateRemainingPeopleByCalculation(@Param("postId") Long postId,
                                           @Param("totalPeople") Integer totalPeople);

    /**
     * 同步剩余人数（不修改总人数，仅重新计算剩余人数）
     * 用于数据修复或定时同步
     *
     * @param postId 帖子ID
     * @return 影响行数
     */
    @Update("UPDATE rally_posts " +
            "SET rally_remaining_people = rally_total_people - " +
            "    (SELECT COUNT(*) FROM rally_participant " +
            "     WHERE rally_post_id = #{postId}), " +
            "    rally_updated_at = NOW() " +
            "WHERE rally_post_id = #{postId}")
    int syncRemainingPeople(@Param("postId") Long postId);

    /**
     * 原子增加剩余人数，并根据需要自动恢复活动状态
     */
    @Update("UPDATE rally_posts " +
            "SET rally_remaining_people = rally_remaining_people + 1, " +
            "    rally_status = CASE WHEN rally_status = 2 THEN 1 ELSE rally_status END, " +
            "    rally_updated_at = NOW() " +
            "WHERE rally_post_id = #{postId}")
    int incrementRemainingPeople(@Param("postId") Long postId);
}
