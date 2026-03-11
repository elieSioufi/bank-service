package com.examples.bankservice.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CREDIT_REQUEST_QUEUE = "credit.request.queue";
    public static final String CREDIT_RESPONSE_QUEUE = "credit.response.queue";

    @Bean
    public Queue creditRequestQueue() {
        return new Queue(CREDIT_REQUEST_QUEUE, true);
    }

    @Bean
    public Queue creditResponseQueue() {
        return new Queue(CREDIT_RESPONSE_QUEUE, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
