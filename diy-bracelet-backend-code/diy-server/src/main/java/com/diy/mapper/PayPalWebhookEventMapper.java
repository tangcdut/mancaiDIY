package com.diy.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;

/**
 * PayPal Webhook事件记录Mapper（幂等处理）
 */
@Mapper
public interface PayPalWebhookEventMapper {

    /**
     * 检查事件是否已处理
     */
    @Select("select count(1) from paypal_webhook_event where event_id = #{eventId}")
    int countByEventId(@Param("eventId") String eventId);

    /**
     * 记录事件为已处理
     */
    @Insert("insert into paypal_webhook_event(event_id, event_type, resource_id, payload, processed, create_time) " +
            "values(#{eventId}, #{eventType}, #{resourceId}, #{payload}, 1, now())")
    void insertProcessed(@Param("eventId") String eventId,
                         @Param("eventType") String eventType,
                         @Param("resourceId") String resourceId,
                         @Param("payload") String payload);
}
