package com.hakyung.barleyssal_spring.infrastrutrue.elastic;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface TradeStatsRepository extends ElasticsearchRepository<TradeStatsDoc, String> {
}