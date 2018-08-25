package com.swym.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.swym.dashboard.service.InMemoryUserService;
import com.swym.dashboard.service.UserService;

@SpringBootApplication
public class TwitterDashboardApplication {

	public static void main(String[] args) {
		SpringApplication.run(TwitterDashboardApplication.class, args);
	}

	@Bean
	public UserService getUserService() {
		return new InMemoryUserService();
	}
}
