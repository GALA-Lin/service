package com.unlimited.sports.globox.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.social.dto.RallyQueryDto;
import com.unlimited.sports.globox.model.social.entity.RallyApplication;
import com.unlimited.sports.globox.model.social.entity.RallyPosts;

import com.unlimited.sports.globox.model.social.vo.RallyPostsVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 集结帖数据访问层
 **/
@Mapper
public interface RallyPostsMapper extends BaseMapper<RallyPosts> {
    /**
     * 获取集结帖列表
     *
     * @param rallyQueryDto 查询条件
     * @param offset        偏移量
     * @param pageSize      页面大小
     * @return 集结帖列表
     */
    List<RallyPosts> getRallyPostsList(
            @Param("rallyQueryDto") RallyQueryDto rallyQueryDto,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);

    /**
     * 根据帖子ID查询集结帖
     *
     * @param postId 帖子ID
     * @return 集结帖实体
     */
    RallyPosts selectByPostId(Long postId);

    /**
     * 获取我的活动列表
     *
     * @param offset   偏移量
     * @param pageSize 页面大小
     * @param userId   用户ID
     * @return 我的活动列表
     */
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
}
