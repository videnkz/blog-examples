package kz.viden.blog.firstservice.rabbit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blog/rabbit")
public class RabbitController {
    @Autowired
    private RabbitService rabbitService;

    @PostMapping("/send")
    public void postMessage(@RequestBody RabbitDto rabbitDto) {
        rabbitService.sendMsg(rabbitDto);
    }
}
