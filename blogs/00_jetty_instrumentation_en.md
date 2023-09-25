## Jetty Http Client instrumentation
### Intro
Elastic APM such another APM tools provides monitoring and troubleshooting of application performance.
You can find more details about this here [link](https://www.elastic.co/blog/monitoring-java-applications-and-getting-started-with-the-elastic-apm-java-agent).

Elastic APM automatically instruments various APIs, frameworks, etc, that you can find here [link](https://www.elastic.co/guide/en/apm/agent/java/current/supported-technologies-details.html).
But, not all technologies are covered by Agent in the box.
In this case Elastic APM Agent Java provides 4 ways enhance the out-of-the-box-instrumentation with [manual instrumentation](https://www.elastic.co/guide/en/apm/agent/java/current/apis.html):
- Public API
- OpenTelemetry bridge
- OpenTracing bridge
- Plugin API

In this article with help of `Plugin API` we implement a new instrumentation for one of familiar http clients - [Jetty HttpClient](https://javadoc.io/static/org.eclipse.jetty/jetty-client/9.1.0.M0/index.html?org/eclipse/jetty/client/HttpClient.html).

### Prepare for implementation
Here you can find fully described article about how to add a new instrumentation - [link](https://www.elastic.co/blog/create-your-own-instrumentation-with-the-java-agent-plugin).

At first we need to write simple test that uses technology that we want to instrument.
For this purpose we create simple http server with several request handles - [HttpServer](https://github.com/videnkz/apm-jetty-httpclient-plugin/blob/master/application/src/main/java/com/kananindzya/elastic/apm/example/webserver/ExampleAlreadyInstrumentedHttpServer.java).

Then we create two handlers that implements `com.sun.net.httpserver.HttpHandler`:
```
    public static class SyncHandler extends MyHttpHandler {

        private final HttpClient httpClient;

        public SyncHandler(final HttpClient httpClient) {
            this.httpClient = httpClient;
        }

        @Override
        public String getContext() {
            return "/sync";
        }

        public void myHandle(HttpExchange t) throws IOException, ExecutionException, InterruptedException, TimeoutException {
            this.httpClient.GET("https://example.com");
            String response = "HelloWorld";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
    
    public static class AsyncHandler extends MyHttpHandler {

        private final HttpClient httpClient;

        public AsyncHandler(final HttpClient httpClient) {
            this.httpClient = httpClient;
        }

        @Override
        public String getContext() {
            return "/async";
        }

        public void myHandle(HttpExchange t) throws IOException, InterruptedException {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            this.httpClient.newRequest("https://example.com")
                    .send(new Response.CompleteListener() {
                        @Override
                        public void onComplete(Result result) {
                            // ignore
                            countDownLatch.countDown();
                        }
                    });
            countDownLatch.await();
            String response = "HelloWorld";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
```
On code above we see synchronous and asynchronous requests with `org.eclipse.jetty.client.HttpClient`.

Next, we need write test that checks work of our new instrumentation.
We create base [AbstractInstrumentationTest](https://github.com/videnkz/apm-jetty-httpclient-plugin/blob/master/jetty-httpclient-plugin/src/test/java/co/elastic/apm/base/AbstractInstrumentationTest.java) 
and [MockApmServer](https://github.com/videnkz/apm-jetty-httpclient-plugin/blob/master/jetty-httpclient-plugin/src/test/java/co/elastic/apm/mock/MockApmServer.java). 
I took this two helper classes from [repository](https://github.com/elastic/apm-agent-java-plugin-example).
We write parametrized test that calls two HttpServer handlers that we create in our application.
```
    @ParameterizedTest
    @ValueSource(strings = {"sync", "async"})
    public void testAsyncInstrumentation(String requestPath) throws IOException, InterruptedException, TimeoutException, ExecutionException {
        Pair<Integer, String> statusCode = executeRequest(requestPath);
        assertEquals(statusCode.getFirst(), 200);
        assertEquals(statusCode.getSecond(), "HelloWorld");

        assertTransactionAndSpan(1000);
    }

    private void assertTransactionAndSpan(long timeoutInMillis) throws TimeoutException {
        JsonNode transaction = ApmServer.getAndRemoveTransaction(0, timeoutInMillis);
        assertNotNull(transaction, "http jdk server instrumentation should creates transaction");

        JsonNode jettyRequestSpan = ApmServer.getAndRemoveSpan(0, 1000);
        assertNotNull(jettyRequestSpan, "Span should be exist");
        assertEquals("GET example.com", jettyRequestSpan.get("name").textValue(), "Span name should be set properly");
        assertEquals("success", jettyRequestSpan.get("outcome").textValue(), "Span outcome should be success");
        JsonNode spanContext = jettyRequestSpan.get("context");
        assertEquals("http", spanContext.get("service").get("target").get("type").textValue(), "Service's target type should be `http` type");
        JsonNode spanDestination = spanContext.get("destination");
        assertEquals("example.com", spanDestination.get("address").textValue(), "Address should contain called domain");
        assertEquals(443, spanDestination.get("port").intValue(), "`Port` field should captured properly.");
    }

    private static Pair<Integer, String> executeRequest(String req) throws IOException, InterruptedException, ExecutionException {
        System.out.println("Trying to get request " + req);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<Pair<Integer, String>> future = executorService.submit(() -> {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + PORT + "/" + req))
                    .GET()
                    .build();

            HttpResponse<String> response = Client.send(request, HttpResponse.BodyHandlers.ofString());
            return new Pair<>(response.statusCode(), response.body());
        });
        return future.get();
    }
```

## Implementation
Now, we need to find place for instrumentation. Let's start with synchronous method executing work:
```
org.eclipse.jetty.client.HttpClient
...
public ContentResponse GET(URI uri) throws InterruptedException, ExecutionException, TimeoutException
{
    return newRequest(uri).send();
}
```
that executes method:
```    
public ContentResponse GET(URI uri) throws InterruptedException, ExecutionException, TimeoutException {
    return newRequest(uri).send();
}
```    
Let's see `org.eclipse.jetty.client.api.Request#send()` method implementation in `org.eclipse.jetty.client.HttpRequest`:
```
@Override
public ContentResponse send() throws InterruptedException, TimeoutException, ExecutionException {
    FutureResponseListener listener = new FutureResponseListener(this);
    send(listener);
...
}
```
Next, we see method `send` that takes `Response.CompleteListener` argument:
```    
@Override
public void send(Response.CompleteListener listener) {
    sendAsync(client::send, listener);
}
```
And then wee see method that takes 2 arguments:
```    
protected void send(final HttpRequest request, List<Response.ResponseListener> listeners) {
    HttpDestination destination = (HttpDestination)resolveDestination(request);
    destination.send(request, listeners);
}
```
We see that internally `HttpClient` uses async mechanism that uses `Response.ResponseListener`.
If we look into asynchronous request work, it internally uses same method that we list above.

So, we found class `org.eclipse.jetty.client.HttpClient` and method `protected void send(final HttpRequest request, List<Response.ResponseListener> listeners)`
that we need to instrument.
The `org.eclipse.jetty.client.api.HttpRequest` class provides information about host, port.
The 2nd argument `List<Response.ResponseListener> responseListeners` provides us opportunity to add our response listener,
where we can implement `onComplete` method:
```
public class SpanResponseCompleteListenerWrapper implements Response.CompleteListener {

    private final Span span;

    public SpanResponseCompleteListenerWrapper(Span span) {
        this.span = span;
    }

    @Override
    public void onComplete(Result result) {
        if (span == null) {
            return;
        }
        try {
            Response response = result.getResponse();
            Throwable t = result.getFailure();
            if (t != null) {
                span.setOutcome(Outcome.FAILURE);
            }
            if (response != null && t == null) {
                span.setOutcome(Outcome.SUCCESS);
            }
            span.captureException(t);
        } finally {
            span.end();
        }
    }
}
```
Our all implementation will look like below:
```
public class JettyHttpClientInstrumentation extends ElasticApmInstrumentation {
    @Override
    public ElementMatcher<? super TypeDescription> getTypeMatcher() {
        return named("org.eclipse.jetty.client.HttpClient");
    }

    @Override
    public ElementMatcher<? super MethodDescription> getMethodMatcher() {
        return named("send")
                .and(takesArgument(0, namedOneOf("org.eclipse.jetty.client.HttpRequest", "org.eclipse.jetty.client.api.Request"))
                        .and(takesArgument(1, List.class)));
    }

    @Override
    public String getAdviceClassName() {
        return "co.elastic.apm.agent.jettyclient.plugin.JettyHttpClientInstrumentation$JettyHttpClientAdvice";
    }

    @Override
    public Collection<String> getInstrumentationGroupNames() {
        return Arrays.asList("http-client", "jetty-client");
    }

    public static class JettyHttpClientAdvice {
        @Nullable
        @Advice.OnMethodEnter(suppress = Throwable.class, inline = false)
        public static Object onBeforeSend(@Advice.Argument(0) Request request,
                                          @Advice.Argument(1) List<Response.ResponseListener> responseListeners) {
            Transaction parent = ElasticApm.currentTransaction();
            if (parent.getId().isEmpty() || request == null) {
                return null;
            }
            Span ret = parent.startExitSpan("external", "http", "");
            String requestHost = request.getHost();
            if (requestHost == null) {
                URI uri = request.getURI();
                requestHost = uri.getHost();
            }
            ret.setName(request.getMethod() + " " + requestHost);
            ret.setDestinationAddress(requestHost, request.getPort());
            ret.injectTraceHeaders((headerName, headerValue) -> request.header(headerName, headerValue));
            responseListeners.add(new SpanResponseCompleteListenerWrapper(ret));
            ret.activate();
            return ret;
        }

        @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class, inline = false)
        public static void onAfterSend(@Advice.Thrown Throwable thrown, @Advice.Enter @Nullable Object spanObject) {
            if (spanObject instanceof Span) {
                ((Span) spanObject).captureException(thrown);
            }
        }
    }
}
```
You can notice that we call `co.elastic.apm.api.Span#injectTraceHeaders`, it's need for injecting tracing headers, which helps see integrated services.

## Demo
If you want run it locally, you need clone or fork repository:
```
https://github.com/videnkz/blog-examples
```
You need already installed on your PC - `docker` and `docker-compose`.
Up all services with docker-compose
```
docker-compose up -d
```

Open kibana with browser: http://localhost:5601.

Wait until kibana, apm-service started. Open `APM` tab and you should see two services
`first-service`, `second-service` without any traces.

Then call `first-service` with postman, curl or any other http client, or if you use Jetbrains IDE, open `first-service/request-examples.http` file.
```
POST http://localhost:8080/api/blog/httpclient/sync
Content-Type: application/json

{
  "httpClientType" : "jetty",
  "url" : "http://second-service:8080/api/blog/secondservice/test/ping"
}

response:
{
  "first": 200,
  "second": "pong"
}
```
In `Transactions` tab of `first-service` you will find that instrumentation properly works:
![00_0_transactions_tab.png](images%2F00%2F00_0_transactions_tab.png)

![00_1_transaction_details.png](images%2F00%2F00_1_transaction_details.png)

![00_2_span_details.png](images%2F00%2F00_2_span_details.png)

![00_3_span_metadata.png](images%2F00%2F00_3_span_metadata.png)

## Links
- Monitoring Java applications with Elastic: Getting started with the Elastic APM Java Agent - https://www.elastic.co/blog/monitoring-java-applications-and-getting-started-with-the-elastic-apm-java-agent
- Elastic APM Agent Java supported technologies - https://www.elastic.co/guide/en/apm/agent/java/current/supported-technologies-details.html
- Manual instrumentation - https://www.elastic.co/guide/en/apm/agent/java/current/apis.html
- Jetty HttpClient java docs - https://javadoc.io/static/org.eclipse.jetty/jetty-client/9.1.0.M0/index.html?org/eclipse/jetty/client/HttpClient.html
- Create your own instrumentation with the Java Agent Plugin - https://www.elastic.co/blog/create-your-own-instrumentation-with-the-java-agent-plugin
- Source code of Jetty HttpClient instrumentation - https://github.com/videnkz/apm-jetty-httpclient-plugin
- Source code for Demo - https://github.com/videnkz/blog-examples
