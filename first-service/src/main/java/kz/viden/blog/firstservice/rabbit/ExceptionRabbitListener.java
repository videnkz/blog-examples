package kz.viden.blog.firstservice.rabbit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@EnableRabbit
@Component
public class ExceptionRabbitListener {

    @RabbitListener(bindings = @QueueBinding(exchange = @Exchange(value = "send-x", type = ExchangeTypes.FANOUT),
            value = @Queue(value = "send-q")))
    public void handleMessage(String msg) {
        log.info("Got msg {}", msg);
        throw new RuntimeException("Bugged.");
    }
}
