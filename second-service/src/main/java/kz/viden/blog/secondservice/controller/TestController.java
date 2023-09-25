package kz.viden.blog.secondservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blog/secondservice/test")
public class TestController {

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
