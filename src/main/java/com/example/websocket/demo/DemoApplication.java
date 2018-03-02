package com.example.websocket.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @RestController
    class HelloController {

        @GetMapping("/{name}")
        String helloWorld(@PathVariable("name") String name) {
            return "Hello " + name;
        }

    }

    @RestController
    class AuthController {

        @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
        ResponseEntity<?> login(@Valid @RequestBody AuthEntity authEntity, HttpServletRequest httpServletRequest)
                throws ServletException {
            httpServletRequest.login(authEntity.user, authEntity.password);
            return ResponseEntity.ok().build();
        }
    }

    static class AuthEntity {

        @NotNull
        private String user;

        @NotNull
        private String password;

        public void setUser(String user) {
            this.user = user;
        }

        public String getUser() {
            return user;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPassword() {
            return password;
        }
    }
}
