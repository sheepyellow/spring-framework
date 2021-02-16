package com.shy;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class AppTest {

	@Test
	public void test1 () {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring-${username}.xml");
	}
}
