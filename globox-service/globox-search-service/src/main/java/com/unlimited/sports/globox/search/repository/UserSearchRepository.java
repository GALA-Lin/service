package com.unlimited.sports.globox.search.repository;

import com.unlimited.sports.globox.search.document.UserSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户搜索文档的Elasticsearch存储库
 */
@Repository
public interface UserSearchRepository extends ElasticsearchRepository<UserSearchDocument, String> {

}
