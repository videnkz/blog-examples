package kz.viden.blog.firstservice.service;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import kz.viden.blog.firstservice.dto.Pair;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class GrizzlyHttpClientCallerServiceImpl implements HttpClientCallerService {
    @Override
    public Pair<Integer, String> callGet(String url) throws Exception {
        var futureResponse = getHttpClient().prepareGet(url).execute();
        var res = futureResponse.get();
        return new Pair<>(res.getStatusCode(), res.getResponseBody());
    }

    @Override
    public Pair<Integer, String> asyncCallGet(String url) throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicReference<Pair<Integer, String>> callResponse = new AtomicReference<>();
        getHttpClient().prepareGet(url)
                .execute(new AsyncCompletionHandler<Response>() {
                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        int statusCode = response.getStatusCode();
                        String strResult = response.getResponseBody();
                        callResponse.set(new Pair<>(statusCode, strResult));
                        countDownLatch.countDown();
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        countDownLatch.countDown();
                    }
                });
        countDownLatch.await();
        return callResponse.get();
    }

    @Override
    public String name() {
        return "grizzly";
    }

    private AsyncHttpClient getHttpClient() throws Exception {
        return new AsyncHttpClient();
    }
}
