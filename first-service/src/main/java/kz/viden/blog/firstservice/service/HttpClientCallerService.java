package kz.viden.blog.firstservice.service;

import kz.viden.blog.firstservice.dto.Pair;

public interface HttpClientCallerService {

    public Pair<Integer, String> callGet(String url) throws Exception;

    public Pair<Integer, String> asyncCallGet(String url) throws Exception;

    String name();
}
