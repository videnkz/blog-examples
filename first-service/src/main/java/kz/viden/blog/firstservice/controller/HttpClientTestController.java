package kz.viden.blog.firstservice.controller;

import kz.viden.blog.firstservice.dto.GetRequestDto;
import kz.viden.blog.firstservice.dto.Pair;
import kz.viden.blog.firstservice.service.HttpClientServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blog/httpclient")
public class HttpClientTestController {

    @Autowired
    private HttpClientServiceImpl httpClientService;

    @PostMapping("/sync")
    public Pair<Integer, String> callGet(@RequestBody GetRequestDto requestDto) throws Exception {
        return httpClientService.callGet(requestDto);
    }

    @PostMapping("/async")
    public Pair<Integer, String> callAsyncGet(@RequestBody GetRequestDto requestDto) throws Exception {
        return httpClientService.callAsyncGet(requestDto);
    }
}
