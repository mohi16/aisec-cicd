package com.thesis.securitystudy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesis.securitystudy.dto.LoginRequest;
import com.thesis.securitystudy.model.Note;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.NoteRepository;
import com.thesis.securitystudy.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class NoteSearchTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private NoteRepository noteRepository;
    @Autowired private UserRepository userRepository;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        noteRepository.deleteAll();

        // Login as testuser to get token
        LoginRequest login = new LoginRequest();
        login.setUsername("testuser");
        login.setPassword("user1234");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        userToken = objectMapper.readTree(body).get("data").get("token").asText();

        // Create test notes
        User user = userRepository.findByUsername("testuser").orElseThrow();

        Note note1 = new Note("Meeting Notes", "Discuss project timeline", user);
        note1.setPublic(false);
        noteRepository.save(note1);

        Note note2 = new Note("Public Announcement", "Company picnic on Friday", user);
        note2.setPublic(true);
        noteRepository.save(note2);

        Note note3 = new Note("Secret Plans", "Launch date is confidential", user);
        note3.setPublic(false);
        noteRepository.save(note3);
    }

    @Test
    void searchOwnNotes_findsMatchingContent() throws Exception {
        mockMvc.perform(get("/api/notes/search")
                        .param("q", "project")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Meeting Notes"));
    }

    @Test
    void searchOwnNotes_findsMatchingTitle() throws Exception {
        mockMvc.perform(get("/api/notes/search")
                        .param("q", "secret")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Secret Plans"));
    }

    @Test
    void searchOwnNotes_caseInsensitive() throws Exception {
        mockMvc.perform(get("/api/notes/search")
                        .param("q", "MEETING")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void searchOwnNotes_noResults() throws Exception {
        mockMvc.perform(get("/api/notes/search")
                        .param("q", "nonexistent")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void searchPublicNotes_findsPublicOnly() throws Exception {
        mockMvc.perform(get("/api/notes/search")
                        .param("q", "picnic")
                        .param("isPublic", "true")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Public Announcement"));
    }

    @Test
    void searchNotes_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/notes/search")
                        .param("q", "test"))
                .andExpect(status().isUnauthorized());
    }
}
