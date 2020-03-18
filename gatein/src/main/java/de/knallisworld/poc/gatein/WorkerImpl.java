package de.knallisworld.poc.gatein;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkerImpl implements Worker {
    @Override
    public GateinMessage handle(final GateinMessage message) {
        log.info("Got message {}...", message);
        final var reply = new GateinMessage();
        reply.setValue("REPLY: " + message.getValue());
        log.info("Sending reply {}", reply);
        return reply;
    }
}
