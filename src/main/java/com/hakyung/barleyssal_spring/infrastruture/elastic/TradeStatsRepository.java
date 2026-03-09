package com.hakyung.barleyssal_spring.infrastruture.elastic;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface TradeStatsRepository extends ElasticsearchRepository<TradeStatsDoc, String> {
}