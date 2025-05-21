package com.capstone.rentit.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Profile("!test")
@Configuration
public class LockerMessagingConfig {

    @Value("${mqtt.broker}")
    private String brokerUrl;

    @Value("${mqtt.username:}")
    private String username;

    @Value("${mqtt.password:}")
    private String password;

    public static final String REQ_TOPIC_PREFIX = "locker/request/";
    public static final String RES_TOPIC_PREFIX = "locker/";

    // ─── MQTT Client Factory ─────────────────────────────────────────────────────

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setServerURIs(new String[]{brokerUrl});
         opts.setUserName(username);
         opts.setPassword(password.toCharArray());
        opts.setAutomaticReconnect(true);
        opts.setCleanSession(false);      // 세션 유지 모드
        opts.setMaxInflight(500);         // 동시에 in-flight 메시지 허용 수
        factory.setConnectionOptions(opts);
        return factory;
    }

    // ─── Executor (비동기) 채널 ───────────────────────────────────────────────────

    @Bean
    public ThreadPoolTaskExecutor mqttExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(10);
        exec.setMaxPoolSize(20);
        exec.setQueueCapacity(200);
        exec.setThreadNamePrefix("mqtt-in-");
        exec.initialize();
        return exec;
    }

    @Bean
    public MessageChannel mqttInboundChannel(ThreadPoolTaskExecutor mqttExecutor) {
        // executor 달린 PublishSubscribeChannel
        return new PublishSubscribeChannel(mqttExecutor);
    }

    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInboundAdapter(
            MqttPahoClientFactory cf,
            @Qualifier("mqttInboundChannel") MessageChannel inboundCh
    ) {
        String[] topics = { REQ_TOPIC_PREFIX + "#" };
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter("rentit-server-sub", cf, topics);
        adapter.setOutputChannel(inboundCh);
        adapter.setQos(1);
        return adapter;
    }

    // ─── Outbound Channel & Handler ───────────────────────────────────────────────

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutboundHandler(MqttPahoClientFactory cf) {
        MqttPahoMessageHandler handler =
                new MqttPahoMessageHandler("rentit-server-pub", cf);
        handler.setAsync(true);
        handler.setDefaultQos(1);
        return handler;
    }
}
