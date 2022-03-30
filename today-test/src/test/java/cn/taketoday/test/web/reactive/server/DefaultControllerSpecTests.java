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

package cn.taketoday.test.web.reactive.server;

import org.junit.jupiter.api.Test;
import cn.taketoday.format.FormatterRegistry;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.codec.ServerCodecConfigurer;
import cn.taketoday.web.bind.annotation.ControllerAdvice;
import cn.taketoday.web.bind.annotation.ExceptionHandler;
import cn.taketoday.web.bind.annotation.GetMapping;
import cn.taketoday.web.bind.annotation.RestController;
import cn.taketoday.web.reactive.accept.RequestedContentTypeResolverBuilder;
import cn.taketoday.web.reactive.config.CorsRegistry;
import cn.taketoday.web.reactive.config.PathMatchConfigurer;
import cn.taketoday.web.reactive.config.ViewResolverRegistry;
import cn.taketoday.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

import java.util.function.Consumer;

/**
 * Unit tests for {@link DefaultControllerSpec}.
 * @author Rossen Stoyanchev
 */
public class DefaultControllerSpecTests {

	@Test
	public void controller() {
		new DefaultControllerSpec(new MyController()).build()
				.get().uri("/")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("Success");
	}

	@Test
	public void controllerAdvice() {
		new DefaultControllerSpec(new MyController())
				.controllerAdvice(new MyControllerAdvice())
				.build()
				.get().uri("/exception")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody(String.class).isEqualTo("Handled exception");
	}

	@Test
	public void controllerAdviceWithClassArgument() {
		new DefaultControllerSpec(MyController.class)
				.controllerAdvice(MyControllerAdvice.class)
				.build()
				.get().uri("/exception")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody(String.class).isEqualTo("Handled exception");
	}

	@Test
	public void configurerConsumers() {
		TestConsumer<ArgumentResolverConfigurer> argumentResolverConsumer = new TestConsumer<>();
		TestConsumer<RequestedContentTypeResolverBuilder> contenTypeResolverConsumer = new TestConsumer<>();
		TestConsumer<CorsRegistry> corsRegistryConsumer = new TestConsumer<>();
		TestConsumer<FormatterRegistry> formatterConsumer = new TestConsumer<>();
		TestConsumer<ServerCodecConfigurer> codecsConsumer = new TestConsumer<>();
		TestConsumer<PathMatchConfigurer> pathMatchingConsumer = new TestConsumer<>();
		TestConsumer<ViewResolverRegistry> viewResolverConsumer = new TestConsumer<>();

		new DefaultControllerSpec(new MyController())
				.argumentResolvers(argumentResolverConsumer)
				.contentTypeResolver(contenTypeResolverConsumer)
				.corsMappings(corsRegistryConsumer)
				.formatters(formatterConsumer)
				.httpMessageCodecs(codecsConsumer)
				.pathMatching(pathMatchingConsumer)
				.viewResolvers(viewResolverConsumer)
				.build();

		assertThat(argumentResolverConsumer.getValue()).isNotNull();
		assertThat(contenTypeResolverConsumer.getValue()).isNotNull();
		assertThat(corsRegistryConsumer.getValue()).isNotNull();
		assertThat(formatterConsumer.getValue()).isNotNull();
		assertThat(codecsConsumer.getValue()).isNotNull();
		assertThat(pathMatchingConsumer.getValue()).isNotNull();
		assertThat(viewResolverConsumer.getValue()).isNotNull();
	}

	@Test // gh-25854
	public void uriTemplate() {
		new DefaultControllerSpec(new MyController()).build()
				.get().uri("/")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("Success")
				.consumeWith(result -> assertThat(result.getUriTemplate()).isEqualTo("/"));
	}


	@RestController
	private static class MyController {

		@GetMapping("/")
		public String handle() {
			return "Success";
		}

		@GetMapping("/exception")
		public void handleWithError() {
			throw new IllegalStateException();
		}

	}


	@ControllerAdvice
	private static class MyControllerAdvice {

		@ExceptionHandler
		public ResponseEntity<String> handle(IllegalStateException ex) {
			return ResponseEntity.status(400).body("Handled exception");
		}
	}


	private static class TestConsumer<T> implements Consumer<T> {

		private T value;

		public T getValue() {
			return this.value;
		}

		@Override
		public void accept(T t) {
			this.value = t;
		}
	}

}
