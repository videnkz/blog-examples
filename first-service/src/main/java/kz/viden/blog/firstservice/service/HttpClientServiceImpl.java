package kz.viden.blog.firstservice.service;

import kz.viden.blog.firstservice.dto.GetRequestDto;
import kz.viden.blog.firstservice.dto.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HttpClientServiceImpl {

    @Autowired
    private HttpClientFactory httpClientFactory;

    public Pair<Integer, String> callGet(final GetRequestDto requestDto) throws Exception {
        final String url = requestDto.getUrl();
        final String httpClientType = requestDto.getHttpClientType();
        return httpClientFactory.getCaller(httpClientType)
                .callGet(url);
    }

    public Pair<Integer, String> callAsyncGet(final GetRequestDto requestDto) throws Exception {
        final String url = requestDto.getUrl();
        final String httpClientType = requestDto.getHttpClientType();
        return httpClientFactory.getCaller(httpClientType)
                .asyncCallGet(url);
    }
}
