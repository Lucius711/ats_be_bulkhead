package fu.ats.messaging;

import fu.ats.config.CvParseProperties;
import fu.ats.shared.commonweb.messaging.rabbitmq.RabbitMessagingNames;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CvParseRabbitConfiguration {

    @Bean
    DirectExchange cvExchange() {
        return new DirectExchange(RabbitMessagingNames.CV_EXCHANGE);
    }

    @Bean
    DirectExchange cvDeadLetterExchange() {
        return new DirectExchange(RabbitMessagingNames.CV_DEAD_LETTER_EXCHANGE);
    }

    @Bean
    Queue cvParseQueue() {
        return QueueBuilder.durable(RabbitMessagingNames.CV_PARSE_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMessagingNames.CV_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMessagingNames.CV_PARSE_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue cvResultQueue() {
        return QueueBuilder.durable(RabbitMessagingNames.CV_RESULT_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMessagingNames.CV_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMessagingNames.CV_RESULT_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue cvParseDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMessagingNames.CV_PARSE_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Queue cvResultDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMessagingNames.CV_RESULT_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding cvParseBinding(
            @Qualifier("cvParseQueue") Queue queue,
            @Qualifier("cvExchange") DirectExchange exchange
    ) {
        return new Binding(
                queue.getName(),
                DestinationType.QUEUE,
                exchange.getName(),
                RabbitMessagingNames.CV_PARSE_ROUTING_KEY,
                null
        );
    }

    @Bean
    Binding cvResultBinding(
            @Qualifier("cvResultQueue") Queue queue,
            @Qualifier("cvExchange") DirectExchange exchange
    ) {
        return new Binding(
                queue.getName(),
                DestinationType.QUEUE,
                exchange.getName(),
                RabbitMessagingNames.CV_RESULT_ROUTING_KEY,
                null
        );
    }

    @Bean
    Binding cvParseDeadLetterBinding(
            @Qualifier("cvParseDeadLetterQueue") Queue queue,
            @Qualifier("cvDeadLetterExchange") DirectExchange exchange
    ) {
        return new Binding(
                queue.getName(),
                DestinationType.QUEUE,
                exchange.getName(),
                RabbitMessagingNames.CV_PARSE_DEAD_LETTER_ROUTING_KEY,
                null
        );
    }

    @Bean
    Binding cvResultDeadLetterBinding(
            @Qualifier("cvResultDeadLetterQueue") Queue queue,
            @Qualifier("cvDeadLetterExchange") DirectExchange exchange
    ) {
        return new Binding(
                queue.getName(),
                DestinationType.QUEUE,
                exchange.getName(),
                RabbitMessagingNames.CV_RESULT_DEAD_LETTER_ROUTING_KEY,
                null
        );
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            CvParseProperties properties
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setDefaultRequeueRejected(false);
        // Listener trả về CompletableFuture: MANUAL để container chỉ ack khi parse xong (không ack ngay khi nhận).
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        // Tối đa bấy nhiêu tin nhắn chưa ack được giao cho service; phần còn lại chờ trong RabbitMQ.
        factory.setPrefetchCount(properties.batch().prefetch());
        return factory;
    }
}
