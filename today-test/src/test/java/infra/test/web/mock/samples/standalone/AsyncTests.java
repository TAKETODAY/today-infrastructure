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

package infra.test.web.mock.samples.standalone;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.ResponseEntity;
import infra.test.web.Person;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.MvcResult;
import infra.util.concurrent.Future;
import infra.web.annotation.ExceptionHandler;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseStatus;
import infra.web.annotation.RestController;
import infra.web.async.DeferredResult;
import infra.web.handler.StreamingResponseBody;
import reactor.core.publisher.Mono;

import static infra.test.web.mock.request.MockMvcRequestBuilders.asyncDispatch;
import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultHandlers.print;
import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.request;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.standaloneSetup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * Tests with asynchronous request handling.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author Jacek Suchenia
 */
class AsyncTests {

  private final AsyncController asyncController = new AsyncController();

  private final MockMvc mockMvc = standaloneSetup(this.asyncController).build();

  @Test
  void callable() throws Exception {
    MvcResult mvcResult = this.mockMvc.perform(get("/1").param("callable", "true"))
            .andExpect(request().asyncStarted())
            .andExpect(request().asyncResult(equalTo(new Person("Joe"))))
            .andExpect(request().asyncResult(new Person("Joe")))
            .andReturn();

    this.mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"));
  }

  @Test
  void streaming() throws Exception {
    this.mockMvc.perform(get("/1").param("streaming", "true"))
            .andExpect(request().asyncStarted())
            .andDo(MvcResult::getAsyncResult) // fetch async result similar to "asyncDispatch" builder
            .andExpect(status().isOk())
            .andExpect(content().string("name=Joe"));
  }

  @Test
  void streamingSlow() throws Exception {
    this.mockMvc.perform(get("/1").param("streamingSlow", "true"))
            .andExpect(request().asyncStarted())
            .andDo(MvcResult::getAsyncResult)
            .andExpect(status().isOk())
            .andExpect(content().string("name=Joe&someBoolean=true"));
  }

  @Test
  void streamingJson() throws Exception {
    this.mockMvc.perform(get("/1").param("streamingJson", "true"))
            .andExpect(request().asyncStarted())
            .andDo(MvcResult::getAsyncResult)
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("{\"name\":\"Joe\",\"someDouble\":0.5}"));
  }

  @Test
  void deferredResult() throws Exception {
    MvcResult mvcResult = this.mockMvc.perform(get("/1").param("deferredResult", "true"))
            .andExpect(request().asyncStarted())
            .andReturn();

    this.mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"));
  }

  @Test
  void deferredResultWithImmediateValue() throws Exception {
    MvcResult mvcResult = this.mockMvc.perform(get("/1").param("deferredResultWithImmediateValue", "true"))
            .andExpect(request().asyncStarted())
            .andExpect(request().asyncResult(new Person("Joe")))
            .andReturn();

    this.mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"));
  }

  @Test

  void deferredResultWithDelayedError() throws Exception {
    MvcResult mvcResult = this.mockMvc.perform(get("/1").param("deferredResultWithDelayedError", "true"))
            .andExpect(request().asyncStarted())
            .andReturn();

    this.mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().is5xxServerError())
            .andExpect(content().string("Delayed Error"));
  }

  @Test
  void listenableFuture() throws Exception {
    MvcResult mvcResult = this.mockMvc.perform(get("/1").param("listenableFuture", "true"))
            .andExpect(request().asyncStarted())
            .andReturn();

    this.mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"));
  }

  @Test

  void completableFutureWithImmediateValue() throws Exception {
    MvcResult mvcResult = this.mockMvc.perform(get("/1").param("completableFutureWithImmediateValue", "true"))
            .andExpect(request().asyncStarted())
            .andReturn();

    this.mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"));
  }

  @Test

  void printAsyncResult() throws Exception {
    StringWriter writer = new StringWriter();

    MvcResult mvcResult = this.mockMvc.perform(get("/1").param("deferredResult", "true"))
            .andDo(print(writer))
            .andExpect(request().asyncStarted())
            .andReturn();

    assertThat(writer.toString().contains("Async started = true")).isTrue();
    writer = new StringWriter();

    this.mockMvc.perform(asyncDispatch(mvcResult))
            .andDo(print(writer))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"));

    assertThat(writer.toString().contains("Async started = false")).isTrue();
  }

  @RestController
  @RequestMapping(path = "/{id}", produces = "application/json")
  private static class AsyncController {

    @RequestMapping(params = "callable")
    Callable<Person> getCallable() {
      return () -> new Person("Joe");
    }

    @RequestMapping(params = "streaming")
    StreamingResponseBody getStreaming() {
      return os -> os.write("name=Joe".getBytes(StandardCharsets.UTF_8));
    }

    @RequestMapping(params = "streamingSlow")
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

    @RequestMapping(params = "streamingJson")
    ResponseEntity<StreamingResponseBody> getStreamingJson() {
      return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
              .body(os -> os.write("{\"name\":\"Joe\",\"someDouble\":0.5}".getBytes(StandardCharsets.UTF_8)));
    }

    @RequestMapping(params = "deferredResult")
    DeferredResult<Person> getDeferredResult() {
      DeferredResult<Person> result = new DeferredResult<>();
      delay(100, () -> result.setResult(new Person("Joe")));
      return result;
    }

    @RequestMapping(params = "deferredResultWithImmediateValue")
    DeferredResult<Person> getDeferredResultWithImmediateValue() {
      DeferredResult<Person> deferredResult = new DeferredResult<>();
      deferredResult.setResult(new Person("Joe"));
      return deferredResult;
    }

    @RequestMapping(params = "deferredResultWithDelayedError")
    DeferredResult<Person> getDeferredResultWithDelayedError() {
      DeferredResult<Person> result = new DeferredResult<>();
      delay(100, () -> result.setErrorResult(new RuntimeException("Delayed Error")));
      return result;
    }

    @RequestMapping(params = "listenableFuture")
    Future<Person> getListenableFuture() {
      var futureTask = Future.forFutureTask(() -> new Person("Joe"));
      delay(100, futureTask);
      return futureTask;
    }

    @RequestMapping(params = "completableFutureWithImmediateValue")
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
