package com.capstone.rentit.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;

@Configuration
public class LockerMessagingConfig {

    /* ===== MQTT 브로커 접속 정보 ===== */
    @Value("${mqtt.broker}")
    private String brokerUrl;

    @Value("${mqtt.username:}")
    private String username;

    @Value("${mqtt.password:}")
    private String password;

    /* ===== Topic 규칙 상수 ===== */
    /** 단말 → 서버 요청: locker/request/{eligible|available|...} */
    public static final String REQ_TOPIC_PREFIX = "locker/request/";
    /** 서버 → 단말 응답: locker/{deviceId}/{eligible|available|result} */
    public static final String RES_TOPIC_PREFIX = "locker/";

    /* ---------- 공통 MQTT ClientFactory ---------- */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setServerURIs(new String[]{brokerUrl});
//        opts.setUserName(username);
//        opts.setPassword(password.toCharArray());
        opts.setAutomaticReconnect(true);
        opts.setCleanSession(true);
        factory.setConnectionOptions(opts);
        return factory;
    }

    /* ---------- Outbound (서버 → 단말) ---------- */

    /** 내부 코드에서 publish 할 때 보낼 채널 */
    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    /** mqttOutboundChannel → 실제 MQTT publish */
    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutboundHandler(MqttPahoClientFactory cf) {
        MqttPahoMessageHandler handler =
                new MqttPahoMessageHandler("rentit-server-pub", cf);
        handler.setAsync(true);
        handler.setDefaultQos(1);
        return handler;
    }

    /* ---------- Inbound (단말 → 서버) ---------- */

    /** 서버가 구독한 메시지를 전달받는 채널 */
    @Bean
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter(
            MqttPahoClientFactory cf) {

        // locker/request/# 하위 모든 토픽 구독
        String[] topics = { REQ_TOPIC_PREFIX + "#" };

        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter("rentit-server-sub", cf, topics);
        adapter.setOutputChannel(mqttInboundChannel());
        adapter.setQos(1);
        return adapter;
    }

    /* ---------- 유틸 헬퍼 (선택) ---------- */

    /**
     * JSON 혹은 Map 등을 payload 로 받아 적절한 topic 으로 publish 할 때 유용.
     *  예) send("locker/42/eligible", payload);
     */
    public void send(MessageChannel ch, String topic, Object payload) {
        ch.send(MessageBuilder.withPayload(payload)
                .setHeader(MqttHeaders.TOPIC, topic)
                .build());
    }
}
