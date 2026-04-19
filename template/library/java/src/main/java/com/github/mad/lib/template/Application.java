package com.github.mad.lib.template;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Slf4j
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void applicationReady() {
        log.trace("TEST TRACE");
        log.debug("TEST DEBUG");
        log.info("Application ready");
        log.warn("TEST WARN");
        log.error("TEST ERROR");
    }
}
