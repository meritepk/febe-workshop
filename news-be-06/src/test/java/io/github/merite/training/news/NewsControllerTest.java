package io.github.merite.training.news;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
public class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testNewsGet() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/news?title=%e 4%"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNotFound());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/news/456"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/news/456")
                .with(testUser("reporter", "REPORTER")))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNotFound());

        testNewsPost();

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/news"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(3)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/news?title=%e 4%"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].id", Matchers.notNullValue()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].title", Matchers.is("title 456")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].details", Matchers.is("details 456")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].tags", Matchers.is("tags 456")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].reportedAt", Matchers.notNullValue()));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/news/1")
                .with(testUser("reporter", "REPORTER")))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.id", Matchers.is(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.title", Matchers.is("title 1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.details", Matchers.is("details 1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.tags", Matchers.is("tags 1")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.reportedAt", Matchers.notNullValue()));
    }

    public void testNewsPost() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/news")
                .with(testUser("reporter", "REPORTER"))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/json")
                .content("{\"id\":1,\"title\":\"title 1\",\"details\":\"details 1\",\"tags\":\"tags 1\"}"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isConflict());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/news")
                .with(testUser("reporter", "REPORTER"))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/json")
                .content("{\"title\":\"title 456\",\"details\":\"details 456\",\"tags\":\"tags 456\"}"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.id", Matchers.notNullValue()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.title", Matchers.is("title 456")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.details", Matchers.is("details 456")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.tags", Matchers.is("tags 456")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.content.reportedAt", Matchers.notNullValue()));
    }

    private RequestPostProcessor testUser(String userName, String authoriy) {
        return SecurityMockMvcRequestPostProcessors.user(userName).authorities(new SimpleGrantedAuthority(authoriy));
    }
}
