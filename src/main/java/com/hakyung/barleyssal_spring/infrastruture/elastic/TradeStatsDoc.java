package com.hakyung.barleyssal_spring.infrastruture.elastic;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

@Document(indexName = "trade-stats")
@Setting(shards = 1, replicas = 0)
public record
TradeStatsDoc(
        @Id String id,
        @Field(type = FieldType.Long) Long userId,
        @Field(type = FieldType.Keyword) String userName,
        @Field(type = FieldType.Keyword) String stockCode,
        @Field(type = FieldType.Keyword) String orderSide,
        @Field(type = FieldType.Double) Double executedPrice,
        @Field(type = FieldType.Long) Long quantity,
        @Field(type = FieldType.Double) Double finalProfitRate,
        @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
        LocalDateTime timestamp
) {}