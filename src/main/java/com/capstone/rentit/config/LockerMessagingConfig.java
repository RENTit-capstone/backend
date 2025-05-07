package com.capstone.rentit.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LockerMessagingConfig {

    /* 단말 → 서버 요청 */
    public static final String REQUEST_EX = "locker.device.request.exchange";
    public static final String REQUEST_Q  = "locker.device.requests";
    public static final String REQUEST_KEY_PATTERN = "locker.request.#";

    @Bean TopicExchange lockerRequestEx() { return new TopicExchange(REQUEST_EX); }
    @Bean Queue lockerRequestQueue() { return QueueBuilder.durable(REQUEST_Q).build(); }
    @Bean Binding lockerRequestBind() {
        return BindingBuilder.bind(lockerRequestQueue())
                .to(lockerRequestEx())
                .with(REQUEST_KEY_PATTERN);
    }

    /* 서버 → 단말 응답·명령 */
    public static final String DEVICE_EX = "locker.device.exchange";
    @Bean TopicExchange lockerDeviceEx() { return new TopicExchange(DEVICE_EX); }
}
