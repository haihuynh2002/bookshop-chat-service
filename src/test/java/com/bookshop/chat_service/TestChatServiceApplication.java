package com.bookshop.chat_service;

import org.springframework.boot.SpringApplication;

public class TestChatServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(TestChatServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
