package com.hakyung.barleyssal_spring.infrastruture.elastic;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDateTime;

@Document(indexName = "trade-stats")
@Setting(shards = 1, replicas = 0)
public record
TradeStatsDoc(
        @Id String id,
        @Field(type = FieldType.Long) Long userId,
        @Field(type = FieldType.Keyword) String stockCode,
        @Field(type = FieldType.Keyword) String orderSide,
        @Field(type = FieldType.Double) Double executedPrice,
        @Field(type = FieldType.Long) Long quantity,
        @Field(type = FieldType.Double) Double profitRate,
        @Field(type = FieldType.Date) LocalDateTime timestamp
) {}