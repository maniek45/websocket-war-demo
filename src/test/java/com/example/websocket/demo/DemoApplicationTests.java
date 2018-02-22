package com.example.websocket.demo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class DemoApplicationTests {

	@Autowired
	private MockMvc mvc;

	@Test
	public void shouldReturnHelloWorld() throws Exception {
		this.mvc.perform(get("/Marcin").accept(MediaType.TEXT_PLAIN))
				.andExpect(status().isOk()).andExpect(content().string("Hello Marcin"));
	}

}
