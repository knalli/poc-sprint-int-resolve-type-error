package de.knallisworld.poc.gatein;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Payload;

public interface Worker {

    @ServiceActivator
    GateinMessage handle(@Payload GateinMessage message);

}
