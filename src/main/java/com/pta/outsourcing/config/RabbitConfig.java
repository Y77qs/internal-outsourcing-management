package com.pta.outsourcing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String NOTIFICATION_EXCHANGE = "pta.notification.exchange";
    public static final String NOTIFICATION_QUEUE = "pta.notification.onboarding.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "onboarding.notification";
    public static final String NOTIFICATION_DLX = "pta.notification.dlx";
    public static final String NOTIFICATION_DLQ = "pta.notification.onboarding.dlq";
    public static final String NOTIFICATION_DLQ_ROUTING_KEY = "onboarding.notification.dead";

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange notificationDeadLetterExchange() {
        return new DirectExchange(NOTIFICATION_DLX, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", NOTIFICATION_DLX);
        args.put("x-dead-letter-routing-key", NOTIFICATION_DLQ_ROUTING_KEY);
        return new Queue(NOTIFICATION_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue notificationDeadLetterQueue() {
        return new Queue(NOTIFICATION_DLQ, true);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(notificationExchange())
                .with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding notificationDeadLetterBinding() {
        return BindingBuilder.bind(notificationDeadLetterQueue())
                .to(notificationDeadLetterExchange())
                .with(NOTIFICATION_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
