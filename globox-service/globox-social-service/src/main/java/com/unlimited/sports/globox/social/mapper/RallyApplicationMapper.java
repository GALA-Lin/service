package com.unlimited.sports.globox.social.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.social.entity.RallyApplication;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RallyApplicationMapper extends BaseMapper<RallyApplication> {
    /**
     * 根据活动ID和申请人ID查询申请记录
     * 
     * @param rallyId 活动ID
     * @param applicantId 申请人ID
     * @return 申请记录实体
     */
    @Select("select * from rally_application where rally_post_id = #{rallyId} and applicant_id = #{applicantId}")
    RallyApplication selectByRallyIdAndApplicantId(@Param("rallyId") Long rallyId, @Param("applicantId") Long applicantId);

    /**
     * 根据申请人ID分页查询申请记录
     * 
     * @param applicantId 申请人ID
     * @param offset 偏移量
     * @param pageSize 页面大小
     * @return 申请记录列表
     */
    @Select("select * from rally_application where applicant_id = #{applicantId} limit #{offset},#{pageSize}")
    List<RallyApplication> getRallyApplicationByApplicantId(@Param("applicantId") Long applicantId, @Param("offset") int offset, @Param("pageSize") int pageSize);

    /**
     * 根据申请人ID统计申请记录数量
     * 
     * @param applicantId 申请人ID
     * @return 申请记录数量
     */
    @Select("select count(*) from rally_application where applicant_id = #{applicantId}")
    int countByApplicantId(@Param("applicantId") Long applicantId);


    /**
     * 根据活动ID查询申请记录，可选过滤申请人
     * 
     * @param rallyId 活动ID
     * @param applicantId 申请人ID（可选，如果为null则查询所有申请者）
     * @return 申请记录列表
     */
    List<RallyApplication> getRallyApplicationByRallyId(@Param("rallyId") Long rallyId , @Param("applicantId")Long applicantId);

    @Select("SELECT * FROM rally_application WHERE applicant_id = #{userId} AND status = #{status} ORDER BY applied_at DESC LIMIT #{offset}, #{pageSize}")
    List<RallyApplication> getRallyApplicationByApplicantIdAndStatus(@Param("userId") Long userId,
                                                                     @Param("status") Integer status,
                                                                     @Param("offset") int offset,
                                                                     @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM rally_application WHERE applicant_id = #{userId} AND status = #{status}")
    int countByApplicantIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);
}
