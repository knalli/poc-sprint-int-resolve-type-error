package de.knallisworld.poc.gateout;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.handler.annotation.Payload;

@MessagingGateway()
public interface MessageService {

    @Gateway(requestChannel = "messageRequestChannel", replyChannel = "messageReplyChannel")
    GateoutMessage run(@Payload GateoutMessage message);

}
