package de.knallisworld.poc.incoming;

import javax.annotation.PostConstruct;

import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.mapping.support.JsonHeaders;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.integration.support.json.JsonObjectMapper;

@SpringBootApplication
@EnableIntegration
@Slf4j
public class IncomingMain {

	@Autowired
	private ConnectionFactory connectionFactory;

	@Autowired
	private AmqpAdmin amqpAdmin;

	@Autowired
	private JsonObjectMapper objectMapper;

	private String queueName = "my_queue";

	public static void main(String[] args) {
		SpringApplication.run(IncomingMain.class, args);
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
	public IntegrationFlow incomingMessageFlow() {
		return IntegrationFlows.from(Amqp.inboundAdapter(connectionFactory, queueName)
				.configureContainer(c -> c.defaultRequeueRejected(false))
		)
				.log(LoggingHandler.Level.TRACE, LoggerFactory.getLogger(IncomingMain.class).getName())
				// ignore existing type definitions
				.headerFilter(JsonHeaders.RESOLVABLE_TYPE, JsonHeaders.TYPE_ID)
				.transform(Transformers.fromJson(IncomingMessage.class, objectMapper))
				.handle(myMessageConsumer(), "accept")
				.get();
	}

	@Bean
	public Consumer<IncomingMessage> myMessageConsumer() {
		return message -> log.info("GOT MESSAGE: " + message);
	}

}
