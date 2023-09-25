package kz.viden.blog.firstservice.service;

import kz.viden.blog.firstservice.dto.Pair;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Response;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class JettyHttpClientCallerServiceImpl implements HttpClientCallerService {
    @Override
    public Pair<Integer, String> callGet(String url) throws Exception {
        ContentResponse response = getHttpClient()
                .GET(url);
        int status = response.getStatus();
        return new Pair<>(status, response.getContentAsString());
    }

    @Override
    public Pair<Integer, String> asyncCallGet(String url) throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicReference<Pair<Integer, String>> callResponse = new AtomicReference<>();
        getHttpClient().newRequest(url)
                .send(result -> {
                    try {
                        Response response = result.getResponse();
                        int statusCode = response.getStatus();
                        String strResult = response.getReason();
                        callResponse.set(new Pair<>(statusCode, strResult));
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        countDownLatch.countDown();
                    }
                });
        countDownLatch.await();
        return callResponse.get();
    }

    @Override
    public String name() {
        return "jetty";
    }

    private HttpClient getHttpClient() throws Exception {
        HttpClient httpClient = new HttpClient();
        httpClient.start();
        return httpClient;
    }
}
