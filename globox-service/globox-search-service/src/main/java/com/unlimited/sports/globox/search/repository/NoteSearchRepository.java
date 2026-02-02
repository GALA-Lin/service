package com.unlimited.sports.globox.search.repository;

import com.unlimited.sports.globox.search.document.NoteSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 笔记搜索文档的Elasticsearch存储库
 */
@Repository
public interface NoteSearchRepository extends ElasticsearchRepository<NoteSearchDocument, String> {

}
