package io.github.merite.training.news;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class NewsService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final NewsRepository repository;

    @Value("${news.page.min:0}")
    public int pageMin = 0;

    @Value("${news.page.size.min:1}")
    public int pageSizeMin = 1;

    @Value("${news.page.size.max:100}")
    public int pageSizeMax = 100;

    public NewsService(NewsRepository repository) {
        this.repository = repository;
    }

    public List<News> findAll(int page, int size, String title) {
        if (page < pageMin) {
            page = pageMin;
        }
        if (size > pageSizeMax) {
            size = pageSizeMax;
        } else if (size < pageSizeMin) {
            size = pageSizeMin;
        }
        logger.debug("News search with page: {}, size: {}, title: {}", page, size, title);
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
            logger.warn("News with id {} already exists", news.getId());
            return null;
        }
        news.setId(System.currentTimeMillis());
        news.setReportedAt(LocalDateTime.now());
        return repository.save(news);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
