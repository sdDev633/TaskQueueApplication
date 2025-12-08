package com.taskqueue.www;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling

public class TaskqueueApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskqueueApplication.class, args);
	}

}
