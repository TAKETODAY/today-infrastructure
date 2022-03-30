/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.web.servlet.samples.client.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.http.MediaType;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextHierarchy;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.servlet.client.MockMvcWebTestClient;
import cn.taketoday.web.bind.annotation.GetMapping;
import cn.taketoday.web.bind.annotation.RestController;
import cn.taketoday.web.context.WebApplicationContext;
import cn.taketoday.web.context.request.async.CallableProcessingInterceptor;
import cn.taketoday.web.servlet.config.annotation.AsyncSupportConfigurer;
import cn.taketoday.web.servlet.config.annotation.EnableWebMvc;
import cn.taketoday.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.mockito.ArgumentMatchers.any;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link cn.taketoday.test.web.servlet.samples.context.AsyncControllerJavaConfigTests}.
 *
 * @author Rossen Stoyanchev
 */
@ExtendWith(ApplicationExtension.class)
@WebAppConfiguration
@ContextHierarchy(@ContextConfiguration(classes = AsyncControllerJavaConfigTests.WebConfig.class))
public class AsyncControllerJavaConfigTests {

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private CallableProcessingInterceptor callableInterceptor;

	private WebTestClient testClient;


	@BeforeEach
	public void setup() {
		this.testClient = MockMvcWebTestClient.bindToApplicationContext(this.wac).build();
	}

	@Test
	public void callableInterceptor() throws Exception {
		testClient.get().uri("/callable")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectBody().json("{\"key\":\"value\"}");

		Mockito.verify(this.callableInterceptor).beforeConcurrentHandling(any(), any());
		Mockito.verify(this.callableInterceptor).preProcess(any(), any());
		Mockito.verify(this.callableInterceptor).postProcess(any(), any(), any());
		Mockito.verify(this.callableInterceptor).afterCompletion(any(), any());
		Mockito.verifyNoMoreInteractions(this.callableInterceptor);
	}


	@Configuration
	@EnableWebMvc
	static class WebConfig implements WebMvcConfigurer {

		@Override
		public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
			configurer.registerCallableInterceptors(callableInterceptor());
		}

		@Bean
		public CallableProcessingInterceptor callableInterceptor() {
			return Mockito.mock(CallableProcessingInterceptor.class);
		}

		@Bean
		public AsyncController asyncController() {
			return new AsyncController();
		}

	}

	@RestController
	static class AsyncController {

		@GetMapping("/callable")
		public Callable<Map<String, String>> getCallable() {
			return () -> Collections.singletonMap("key", "value");
		}
	}

}
