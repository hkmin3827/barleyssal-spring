package com.hakyung.barleyssal_spring.infrastruture.elastic;

import com.hakyung.barleyssal_spring.domain.order.Order;
import com.hakyung.barleyssal_spring.global.utils.TimeConverter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;


// 하루 지난 데이터들만 저장. 14일간
@Document(indexName = "order-history")
@Setting(shards = 1, replicas = 0)
public record OrderHistoryDoc(
        @Id String id,
        @Field(type = FieldType.Long) Long orderId,
        @Field(type = FieldType.Long) Long accountId,
        @Field(type = FieldType.Keyword) String stockCode,
        @Field(type = FieldType.Keyword) String orderSide,
        @Field(type = FieldType.Keyword) String orderType,
        @Field(type = FieldType.Long) Long quantity,
        @Field(type = FieldType.Double) Double limitPrice,
        @Field(type = FieldType.Double) Double executedPrice,
        @Field(type = FieldType.Long) Long executedQuantity,
        @Field(type = FieldType.Keyword) String createdAt,
        @Field(type = FieldType.Keyword) String rejectReason,
        @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
        LocalDateTime timestamp
) {
    public static OrderHistoryDoc from(Order order){
        return new OrderHistoryDoc(
                null,
                order.getId(),
                order.getAccountId(),
                order.getStockCode().value(),
                order.getOrderSide().name(),
                order.getOrderType().name(),
                order.getQuantity(),
                order.getLimitPrice() != null ? order.getLimitPrice().doubleValue() : null,
                order.getExecutedPrice() != null ? order.getExecutedPrice().doubleValue() : null,
                order.getExecutedQuantity(),
                String.valueOf(order.getCreatedAt()),
                order.getRejectReason() != null ? order.getRejectReason().name() : null,
                TimeConverter.now()
        );
    }
}
