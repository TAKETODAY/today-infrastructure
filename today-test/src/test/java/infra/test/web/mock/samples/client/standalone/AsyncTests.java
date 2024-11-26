/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.web.mock.samples.client.standalone;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.ResponseEntity;
import infra.test.web.Person;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.reactive.server.WebTestClient;
import infra.util.concurrent.Future;
import infra.util.concurrent.ListenableFutureTask;
import infra.web.annotation.ExceptionHandler;
import infra.web.annotation.GetMapping;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseStatus;
import infra.web.annotation.RestController;
import infra.web.async.DeferredResult;
import infra.web.handler.StreamingResponseBody;
import reactor.core.publisher.Mono;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link infra.test.web.mock.samples.standalone.AsyncTests}.
 *
 * @author Rossen Stoyanchev
 */
class AsyncTests {

  private final WebTestClient testClient =
          MockMvcWebTestClient.bindToController(new AsyncController()).build();

  @Test
  void callable() {
    this.testClient.get()
            .uri("/1?callable=true")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .json("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}");
  }

  @Test
  void streaming() {
    this.testClient.get()
            .uri("/1?streaming=true")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class).isEqualTo("name=Joe");
  }

  @Test
  void streamingSlow() {
    this.testClient.get()
            .uri("/1?streamingSlow=true")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class).isEqualTo("name=Joe&someBoolean=true");
  }

  @Test
  void streamingJson() {
    this.testClient.get()
            .uri("/1?streamingJson=true")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json("{\"name\":\"Joe\",\"someDouble\":0.5}");
  }

  @Test
  void deferredResult() {
    this.testClient.get()
            .uri("/1?deferredResult=true")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}");
  }

  @Test
  void deferredResultWithImmediateValue() {
    this.testClient.get()
            .uri("/1?deferredResultWithImmediateValue=true")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}");
  }

  @Test
  void deferredResultWithDelayedError() {
    this.testClient.get()
            .uri("/1?deferredResultWithDelayedError=true")
            .exchange()
            .expectStatus().is5xxServerError()
            .expectBody(String.class).isEqualTo("Delayed Error");
  }

  @Test
  void listenableFuture() {
    this.testClient.get()
            .uri("/1?listenableFuture=true")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}");
  }

  @Test
  void completableFutureWithImmediateValue() throws Exception {
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
    Callable<Person> getCallable() {
      return () -> new Person("Joe");
    }

    @GetMapping(params = "streaming")
    StreamingResponseBody getStreaming() {
      return os -> os.write("name=Joe".getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping(params = "streamingSlow")
    StreamingResponseBody getStreamingSlow() {
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
    ResponseEntity<StreamingResponseBody> getStreamingJson() {
      return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
              .body(os -> os.write("{\"name\":\"Joe\",\"someDouble\":0.5}".getBytes(StandardCharsets.UTF_8)));
    }

    @GetMapping(params = "deferredResult")
    DeferredResult<Person> getDeferredResult() {
      DeferredResult<Person> result = new DeferredResult<>();
      delay(100, () -> result.setResult(new Person("Joe")));
      return result;
    }

    @GetMapping(params = "deferredResultWithImmediateValue")
    DeferredResult<Person> getDeferredResultWithImmediateValue() {
      DeferredResult<Person> result = new DeferredResult<>();
      result.setResult(new Person("Joe"));
      return result;
    }

    @GetMapping(params = "deferredResultWithDelayedError")
    DeferredResult<Person> getDeferredResultWithDelayedError() {
      DeferredResult<Person> result = new DeferredResult<>();
      delay(100, () -> result.setErrorResult(new RuntimeException("Delayed Error")));
      return result;
    }

    @GetMapping(params = "listenableFuture")
    Future<Person> getListenableFuture() {
      ListenableFutureTask<Person> futureTask = Future.forFutureTask(() -> new Person("Joe"));
      delay(100, futureTask);
      return futureTask;
    }

    @GetMapping(params = "completableFutureWithImmediateValue")
    CompletableFuture<Person> getCompletableFutureWithImmediateValue() {
      CompletableFuture<Person> future = new CompletableFuture<>();
      future.complete(new Person("Joe"));
      return future;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    String errorHandler(Exception ex) {
      return ex.getMessage();
    }

    private void delay(long millis, Runnable task) {
      Mono.delay(Duration.ofMillis(millis)).doOnTerminate(task).subscribe();
    }
  }

}
