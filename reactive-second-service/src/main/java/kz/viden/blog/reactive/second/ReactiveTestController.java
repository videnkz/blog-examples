package kz.viden.blog.reactive.second;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/blog/reactivesecondservice/test")
public class ReactiveTestController {

    @GetMapping("/mono")
    public Mono<String> mono() {
        return Mono.just("Hello");
    }

    @GetMapping("/flux")
    public Flux<String> flux() {
        return Flux.just("Hello", ",", " ", "World", "!");
    }

}
