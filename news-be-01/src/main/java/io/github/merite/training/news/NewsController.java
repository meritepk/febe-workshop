package io.github.merite.training.news;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    @GetMapping
    public Map<String, String> hello() {
        return Map.of("content", "Hello World!");
    }
}
