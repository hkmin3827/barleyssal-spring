package com.hakyung.barleyssal_spring.infrastrutrue.elastic;

import jakarta.persistence.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(indexName = "trade-stats")
public record TradeStatsDoc(
        @Id String id,
        @Field(type = FieldType.Long) Long userId,
        @Field(type = FieldType.Keyword) String stockCode,
        @Field(type = FieldType.Keyword) String orderSide,
        @Field(type = FieldType.Double) BigDecimal executedPrice,
        @Field(type = FieldType.Long) Long quantity,
        @Field(type = FieldType.Double) Double profitRate,
        @Field(type = FieldType.Date) LocalDateTime timestamp
) {}