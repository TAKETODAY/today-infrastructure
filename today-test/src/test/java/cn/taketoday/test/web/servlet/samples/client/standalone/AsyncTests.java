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

package cn.taketoday.test.web.servlet.samples.client.standalone;

import org.junit.jupiter.api.Test;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.servlet.client.MockMvcWebTestClient;
import cn.taketoday.util.concurrent.ListenableFuture;
import cn.taketoday.util.concurrent.ListenableFutureTask;
import cn.taketoday.web.bind.annotation.ExceptionHandler;
import cn.taketoday.web.bind.annotation.GetMapping;
import cn.taketoday.web.bind.annotation.RequestMapping;
import cn.taketoday.web.bind.annotation.ResponseStatus;
import cn.taketoday.web.bind.annotation.RestController;
import cn.taketoday.web.context.request.async.DeferredResult;
import cn.taketoday.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import reactor.core.publisher.Mono;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link cn.taketoday.test.web.servlet.samples.standalone.AsyncTests}.
 *
 * @author Rossen Stoyanchev
 */
public class AsyncTests {

	private final WebTestClient testClient =
			MockMvcWebTestClient.bindToController(new AsyncController()).build();


	@Test
	public void callable() {
		this.testClient.get()
				.uri("/1?callable=true")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody().json("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}");
	}

	@Test
	public void streaming() {
		this.testClient.get()
				.uri("/1?streaming=true")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("name=Joe");
	}

	@Test
	public void streamingSlow() {
		this.testClient.get()
				.uri("/1?streamingSlow=true")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("name=Joe&someBoolean=true");
	}

	@Test
	public void streamingJson() {
		this.testClient.get()
				.uri("/1?streamingJson=true")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody().json("{\"name\":\"Joe\",\"someDouble\":0.5}");
	}

	@Test
	public void deferredResult() {
		this.testClient.get()
				.uri("/1?deferredResult=true")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody().json("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}");
	}

	@Test
	public void deferredResultWithImmediateValue() {
		this.testClient.get()
				.uri("/1?deferredResultWithImmediateValue=true")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody().json("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}");
	}

	@Test
	public void deferredResultWithDelayedError() {
		this.testClient.get()
				.uri("/1?deferredResultWithDelayedError=true")
				.exchange()
				.expectStatus().is5xxServerError()
				.expectBody(String.class).isEqualTo("Delayed Error");
	}

	@Test
	public void listenableFuture() {
		this.testClient.get()
				.uri("/1?listenableFuture=true")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody().json("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}");
	}

	@Test
	public void completableFutureWithImmediateValue() throws Exception {
		this.testClient.get()
				.uri("/1?completableFutureWithImmediateValue=true")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody().json("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}");
	}


	@RestController
	@RequestMapping(path = "/{id}", produces = "application/json")
	private static class AsyncController {

		@GetMapping(params = "callable")
		public Callable<Person> getCallable() {
			return () -> new Person("Joe");
		}

		@GetMapping(params = "streaming")
		public StreamingResponseBody getStreaming() {
			return os -> os.write("name=Joe".getBytes(StandardCharsets.UTF_8));
		}

		@GetMapping(params = "streamingSlow")
		public StreamingResponseBody getStreamingSlow() {
			return os -> {
				os.write("name=Joe".getBytes());
				try {
					Thread.sleep(200);
					os.write("&someBoolean=true".getBytes(StandardCharsets.UTF_8));
				}
				catch (InterruptedException e) {
					/* no-op */
				}
			};
		}

		@GetMapping(params = "streamingJson")
		public ResponseEntity<StreamingResponseBody> getStreamingJson() {
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
					.body(os -> os.write("{\"name\":\"Joe\",\"someDouble\":0.5}".getBytes(StandardCharsets.UTF_8)));
		}

		@GetMapping(params = "deferredResult")
		public DeferredResult<Person> getDeferredResult() {
			DeferredResult<Person> result = new DeferredResult<>();
			delay(100, () -> result.setResult(new Person("Joe")));
			return result;
		}

		@GetMapping(params = "deferredResultWithImmediateValue")
		public DeferredResult<Person> getDeferredResultWithImmediateValue() {
			DeferredResult<Person> result = new DeferredResult<>();
			result.setResult(new Person("Joe"));
			return result;
		}

		@GetMapping(params = "deferredResultWithDelayedError")
		public DeferredResult<Person> getDeferredResultWithDelayedError() {
			DeferredResult<Person> result = new DeferredResult<>();
			delay(100, () -> result.setErrorResult(new RuntimeException("Delayed Error")));
			return result;
		}

		@GetMapping(params = "listenableFuture")
		public ListenableFuture<Person> getListenableFuture() {
			ListenableFutureTask<Person> futureTask = new ListenableFutureTask<>(() -> new Person("Joe"));
			delay(100, futureTask);
			return futureTask;
		}

		@GetMapping(params = "completableFutureWithImmediateValue")
		public CompletableFuture<Person> getCompletableFutureWithImmediateValue() {
			CompletableFuture<Person> future = new CompletableFuture<>();
			future.complete(new Person("Joe"));
			return future;
		}

		@ExceptionHandler(Exception.class)
		@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
		public String errorHandler(Exception ex) {
			return ex.getMessage();
		}

		private void delay(long millis, Runnable task) {
			Mono.delay(Duration.ofMillis(millis)).doOnTerminate(task).subscribe();
		}
	}

}
