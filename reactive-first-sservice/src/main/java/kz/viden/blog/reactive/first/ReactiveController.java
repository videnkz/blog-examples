package kz.viden.blog.reactive.first;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

@RestController
@RequestMapping("/api/blog/reactive")
public class ReactiveController {

    @Autowired
    private WebClient webClient;

    @GetMapping("/mono")
    public Mono<String> getMono() {
        return webClient.get().uri("/api/blog/reactivesecondservice/test/mono")
                .exchangeToMono(response -> response.bodyToMono(String.class))
                .contextWrite(ctx -> ctx.put())
                .doOnEach(k -> {
                    System.out.println("Type signal is " + k.getType());
                    ContextView context = k.getContextView();
                    Object val = context.get("key");
                    System.out.println("Val " + val);
                });
    }

    @GetMapping("/flux")
    public Flux<String> getFlux() {
        return webClient.get().uri("/api/blog/reactivesecondservice/test/flux")
                .retrieve().bodyToFlux(String.class);
    }
}
