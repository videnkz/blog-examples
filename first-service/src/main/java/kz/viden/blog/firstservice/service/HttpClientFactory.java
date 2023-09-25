package kz.viden.blog.firstservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class HttpClientFactory {

    @Autowired
    private List<HttpClientCallerService> callerImpls;

    public HttpClientCallerService getCaller(String callerName) {
        return callerImpls
                .stream()
                .filter(callerImpl -> Objects.equals(callerImpl.name(), callerName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Http client implementation not found"));
    }
}
