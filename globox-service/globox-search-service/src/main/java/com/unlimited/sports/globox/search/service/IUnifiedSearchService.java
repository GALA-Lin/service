package com.unlimited.sports.globox.search.service;

import com.unlimited.sports.globox.model.search.dto.UnifiedSearchResultVo;
import com.unlimited.sports.globox.search.document.UnifiedSearchDocument;

import java.util.List;
import java.util.Map;

/**
 * 统一搜索服务接口
 * 用于全局搜索（如搜"网球"返回场馆+教练+笔记+约球混合结果）
 */
public interface IUnifiedSearchService {

    UnifiedSearchResultVo searchUnified(String keyword, List<String> types,
                                         String sortBy, Integer page, Integer pageSize);

    /**
     * 批量保存或更新到统一索引（不存在就新增，存在就更新）
     * @param documents 统一搜索文档列表
     */
    void saveOrUpdateToUnified(List<UnifiedSearchDocument> documents);

    /**
     * 批量删除统一索引中的文档
     * @param docIds 文档ID列表
     */
    void deleteFromUnified(List<String> docIds);

}
