package com.example.websocket.demo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.HttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class DemoApplicationTests {

    @Autowired
    private MockMvc mvc;

    @Test
    public void shouldLoginWithValidCredentials() throws Exception {
        this.mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"user\": \"user\", \"password\": \"password\" }"))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldLogoutAfterLogin() throws Exception {
        HttpSession session = this.mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"user\": \"user\", \"password\": \"password\" }"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession();

        this.mvc.perform(put("/logout").session((MockHttpSession) session))
                .andExpect(status().isNoContent());
    }

    @Test
    public void shouldReturnHelloWorld() throws Exception {
        HttpSession session = this.mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"user\": \"user\", \"password\": \"password\" }"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession();

        this.mvc.perform(get("/rest/Marcin").session((MockHttpSession) session))
                .andExpect(status().isOk()).andExpect(content().string("Hello Marcin"));
    }

}
