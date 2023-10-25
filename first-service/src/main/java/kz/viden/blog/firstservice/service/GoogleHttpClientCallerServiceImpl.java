package kz.viden.blog.firstservice.service;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import kz.viden.blog.firstservice.dto.Pair;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Response;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class GoogleHttpClientCallerServiceImpl implements HttpClientCallerService {
    @Override
    public Pair<Integer, String> callGet(String url) throws Exception {
        var req = getHttpClient()
                .buildGetRequest(new GenericUrl(url));
        var res = req.execute();
        int status = res.getStatusCode();
        return new Pair<>(status, res.parseAsString());
    }

    @Override
    public Pair<Integer, String> asyncCallGet(String url) throws Exception {
//        CountDownLatch countDownLatch = new CountDownLatch(1);
//        AtomicReference<Pair<Integer, String>> callResponse = new AtomicReference<>();
//        getHttpClient().newRequest(url)
//                .send(result -> {
//                    try {
//                        Response response = result.getResponse();
//                        int statusCode = response.getStatus();
//                        String strResult = response.getReason();
//                        callResponse.set(new Pair<>(statusCode, strResult));
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    } finally {
//                        countDownLatch.countDown();
//                    }
//                });
//        countDownLatch.await();
//        return callResponse.get();
        return null;
    }

    @Override
    public String name() {
        return "google";
    }

    private HttpRequestFactory getHttpClient() throws Exception {
        HttpRequestFactory requestFactory
                = new NetHttpTransport().createRequestFactory();
        return requestFactory;
    }
}
