package com.unlimited.sports.globox.search.repository;

import com.unlimited.sports.globox.search.document.VenueSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 场馆搜索文档的Elasticsearch存储库
 */
@Repository
public interface VenueSearchRepository extends ElasticsearchRepository<VenueSearchDocument, String> {

}
