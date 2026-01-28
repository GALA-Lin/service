package com.unlimited.sports.globox.search.repository;

import com.unlimited.sports.globox.search.document.UnifiedSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 统一搜索文档的Elasticsearch存储库
 */
@Repository
public interface UnifiedSearchRepository extends ElasticsearchRepository<UnifiedSearchDocument, String> {

}
