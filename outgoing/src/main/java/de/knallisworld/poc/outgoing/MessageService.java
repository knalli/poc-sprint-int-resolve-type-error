package de.knallisworld.poc.outgoing;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.handler.annotation.Payload;

@MessagingGateway()
public interface MessageService {

	@Gateway(requestChannel = "messageRequestChannel")
	void run(@Payload OutgoingMessage message);

}
