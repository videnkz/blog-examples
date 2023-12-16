package kz.viden.blog.firstservice.rabbit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RabbitService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendMsg(RabbitDto rabbitDto) {
        rabbitTemplate.convertAndSend("send-x", "*", rabbitDto.getMessage());
    }
}
