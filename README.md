= Demo project for type resolving error

For spring-integration 5.2.x:
```
org.springframework.integration.transformer.MessageTransformationException: failed to transform message; nested exception is java.lang.IllegalStateException: java.lang.ClassNotFoundException

by JsonToObjectTransformer.getClassForValue
```


For spring-integration 5.2.4 (with filtered resolvabletype and typeId)
```
DefaultAmqpHeaderMapper  : error occurred while mapping from AMQP properties to MessageHeaders

java.lang.IllegalStateException: java.lang.ClassNotFoundException
```

This projects uses AMQP, so provide or start a suitable broker like
```
docker run --rm --hostname poc -p 5672:5672 rabbitmq:3
```
