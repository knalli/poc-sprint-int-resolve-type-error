package de.knallisworld.poc.gateout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.integration.support.json.JsonObjectMapper;
import org.springframework.messaging.MessageChannel;

import javax.annotation.PostConstruct;

import static org.springframework.integration.dsl.IntegrationFlows.from;

@SpringBootApplication
@EnableIntegration
@IntegrationComponentScan
@Slf4j
public class GateoutMain {

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private JsonObjectMapper objectMapper;

    private String queueName = "my_queue";

    public static void main(String[] args) {
        SpringApplication.run(GateoutMain.class, args);
    }

    @Bean
    public CommandLineRunner runner(final MessageService messageService) {
        return args -> {
            final var message = new GateoutMessage("XYZ");
            log.info("Sending message {}...", message);
            final var reply = messageService.run(message);
            log.info("Got reply {}", reply);
        };
    }

    @Bean
    public JsonObjectMapper<?, ?> jsonMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonObjectMapper(objectMapper);
    }

    @PostConstruct
    public void setupQueue() {
        try {
            if (amqpAdmin.getQueueProperties(queueName) == null) {
                final Queue queue = QueueBuilder.durable(queueName)
                                                .build();
                setupQueueDeclaration(queue);
            } else {
                log.info("Queue '{}' already found and being declared", queueName);
            }
        } catch (final Exception e) {
            log.error("Could not connect to RabbitMQ and configure queue: {}", e.getMessage());
        }
    }

    private void setupQueueDeclaration(final Queue queue) {
        try {
            amqpAdmin.declareQueue(queue);
            log.info("Queue '{}' declared successfully", queueName);
        } catch (final Exception e) {
            log.error("Queue '{}' declaring failed due: {}", queueName, e.getMessage());
        }
    }

    @Bean
    public MessageChannel messageRequestChannel() {
        return MessageChannels.direct("messageRequestChannel")
                              .get();
    }

    @Bean
    public MessageChannel messageReplyChannel() {
        return MessageChannels.direct("messageReplyChannel")
                              .get();
    }

    @Bean
    public AmqpTemplate messageAmqpTemplate() {
        final RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setDefaultReceiveQueue(queueName);
        return template;
    }

    @Bean
    public IntegrationFlow messageHandlingFlow() {
        return from(messageRequestChannel())
                .transform(Transformers.toJson(objectMapper, ObjectToJsonTransformer.ResultType.STRING))
                .log(LoggingHandler.Level.TRACE, LoggerFactory.getLogger(GateoutMain.class).getName())
                .handle(Amqp.outboundGateway(messageAmqpTemplate())
                            .mappedReplyHeaders("!json_*", "*")
                            .routingKey(queueName))
                .transform(Transformers.fromJson(GateoutMessage.class, objectMapper))
                .channel(messageReplyChannel())
                .get();
    }

}
