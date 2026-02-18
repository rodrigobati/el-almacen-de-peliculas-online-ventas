package unrn.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.ventas.events.exchange}")
    private String ventasEventsExchangeName;

    @Value("${rabbitmq.catalogo.events.exchange}")
    private String catalogoEventsExchangeName;

    @Value("${rabbitmq.ventas.stock.rechazado.queue}")
    private String ventasStockRechazadoQueueName;

    @Value("${rabbitmq.ventas.stock.rechazado.retry.queue}")
    private String ventasStockRechazadoRetryQueueName;

    @Value("${rabbitmq.ventas.stock.rechazado.retry.routing-key}")
    private String ventasStockRechazadoRetryRoutingKey;

    @Value("${rabbitmq.ventas.stock.rechazado.retry.ttl-ms}")
    private Integer ventasStockRechazadoRetryTtlMs;

    @Value("${rabbitmq.ventas.stock.rechazado.dlq.queue}")
    private String ventasStockRechazadoDlqQueueName;

    @Value("${rabbitmq.ventas.stock.rechazado.dlq.routing-key}")
    private String ventasStockRechazadoDlqRoutingKey;

    @Value("${rabbitmq.catalogo.stock.rechazado.routing-key}")
    private String catalogoStockRechazadoRoutingKey;

    @Value("${rabbitmq.ventas.dlx.exchange}")
    private String ventasDeadLetterExchangeName;

    @Value("${rabbitmq.ventas.retry.exchange}")
    private String ventasRetryExchangeName;

    @Bean
    public TopicExchange ventasEventsExchange() {
        return new TopicExchange(ventasEventsExchangeName, true, false);
    }

    @Bean
    public TopicExchange catalogoEventsExchange() {
        return new TopicExchange(catalogoEventsExchangeName, true, false);
    }

    @Bean
    public DirectExchange ventasRetryExchange() {
        return new DirectExchange(ventasRetryExchangeName, true, false);
    }

    @Bean
    public DirectExchange ventasDeadLetterExchange() {
        return new DirectExchange(ventasDeadLetterExchangeName, true, false);
    }

    @Bean
    public Queue ventasStockRechazadoQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(ventasStockRechazadoQueueName)
                .deadLetterExchange(ventasRetryExchangeName)
                .deadLetterRoutingKey(ventasStockRechazadoRetryRoutingKey)
                .build();
    }

    @Bean
    public Queue ventasStockRechazadoRetryQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(ventasStockRechazadoRetryQueueName)
                .ttl(ventasStockRechazadoRetryTtlMs)
                .deadLetterExchange(catalogoEventsExchangeName)
                .deadLetterRoutingKey(catalogoStockRechazadoRoutingKey)
                .build();
    }

    @Bean
    public Queue ventasStockRechazadoDlqQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(ventasStockRechazadoDlqQueueName).build();
    }

    @Bean
    public Binding ventasStockRechazadoBinding() {
        return BindingBuilder.bind(ventasStockRechazadoQueue())
                .to(catalogoEventsExchange())
                .with(catalogoStockRechazadoRoutingKey);
    }

    @Bean
    public Declarables ventasStockRechazadoRetryAndDlqBindings() {
        Binding retryBinding = BindingBuilder.bind(ventasStockRechazadoRetryQueue())
                .to(ventasRetryExchange())
                .with(ventasStockRechazadoRetryRoutingKey);

        Binding dlqBinding = BindingBuilder.bind(ventasStockRechazadoDlqQueue())
                .to(ventasDeadLetterExchange())
                .with(ventasStockRechazadoDlqRoutingKey);

        return new Declarables(retryBinding, dlqBinding);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("unrn.event", "unrn.dto", "java.util", "java.lang");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            MeterRegistry meterRegistry) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                meterRegistry.counter("ventas.rabbit.publisher.nack.total").increment();
            }
        });
        rabbitTemplate.setReturnsCallback(returned -> meterRegistry
                .counter("ventas.rabbit.publisher.returned.total")
                .increment());

        if (connectionFactory instanceof CachingConnectionFactory cachingConnectionFactory) {
            cachingConnectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
            cachingConnectionFactory.setPublisherReturns(true);
        }
        return rabbitTemplate;
    }

    @Bean(name = "manualAckRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory manualAckRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        return factory;
    }
}
