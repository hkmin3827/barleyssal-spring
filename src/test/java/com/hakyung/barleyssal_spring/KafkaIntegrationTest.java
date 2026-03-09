//package com.hakyung.barleyssal_spring;
//
//import com.hakyung.barleyssal_spring.infrastruture.kafka.events.ExecutionEvent;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.kafka.core.KafkaTemplate;
//
//import java.math.BigDecimal;
//
//@SpringBootTest
//public class KafkaIntegrationTest {
//
//    @Autowired
//    private KafkaTemplate<String, Object> kafkaTemplate;
//
//    @Test
//    void sendTestMessage() {
//        // TradeStatisticsService가 구독하는 토픽명과 일치해야 함
//        String topic = "execution.event";
//
//        // 실제 ExecutionEvent 객체 생성 (프로젝트 내 Record 구조에 맞게 수정하세요)
//        ExecutionEvent event = new ExecutionEvent(
//            "ORD-TEST-001",
//            "1",
//            "hkmin999",
//            "005930",
//            "BUY",
//            "5", 
//            BigDecimal.ZERO
//        );
//
//        kafkaTemplate.send(topic, event);
//        System.out.println(">>> 테스트 메시지 발행 완료!");
//    }
//}