package com.unlimited.sports.globox.config;

import com.unlimited.sports.globox.prop.MQDlqProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MQDlqContainerFactoryConfig {

    private final MQDlqProperties props;

    @Bean
    public SimpleRabbitListenerContainerFactory dlqContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);

        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(props.getPrefetch());
        factory.setConcurrentConsumers(props.getConcurrentConsumers());
        factory.setMaxConcurrentConsumers(props.getMaxConcurrentConsumers());

        return factory;
    }
}