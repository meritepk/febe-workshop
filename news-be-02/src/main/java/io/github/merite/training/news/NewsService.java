package io.github.merite.training.news;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class NewsService {

    public static final int PAGE_MIN = 0;
    public static final int PAGE_SIZE_MIN = 1;
    public static final int PAGE_SIZE_MAX = 100;

    private final NewsRepository repository;

    public NewsService(NewsRepository repository) {
        this.repository = repository;
    }

    public List<News> findAll(int page, int size, String title) {
        if (page < PAGE_MIN) {
            page = PAGE_MIN;
        }
        if (size > PAGE_SIZE_MAX) {
            size = PAGE_SIZE_MAX;
        } else if (size < PAGE_SIZE_MIN) {
            size = PAGE_SIZE_MIN;
        }
        Pageable pageable = PageRequest.of(page, size);
        if (title.isEmpty() || title.isBlank()) {
            return repository.findByOrderByIdDesc(pageable).getContent();
        }
        return repository.findByTitleLikeOrderByIdDesc(pageable, title).getContent();
    }

    public News findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public News create(News news) {
        if (news.getId() != null && repository.existsById(news.getId())) {
            return null;
        }
        news.setId(System.currentTimeMillis());
        news.setReportedAt(LocalDateTime.now());
        return repository.save(news);
    }
}
