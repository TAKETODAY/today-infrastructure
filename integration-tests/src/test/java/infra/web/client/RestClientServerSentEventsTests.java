package infra.web.client;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import infra.app.InfraApplication;
import infra.app.test.context.InfraTest;
import infra.app.test.context.InfraTest.WebEnvironment;
import infra.app.test.web.server.LocalServerPort;
import infra.core.ParameterizedTypeReference;
import infra.http.ServerSentEvent;
import infra.test.context.TestPropertySource;
import infra.util.concurrent.Future;
import infra.web.annotation.GET;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RequestParam;
import infra.web.annotation.RestController;
import infra.web.handler.result.ResponseBodyEmitter;
import infra.web.handler.result.SseEmitter;

import static infra.util.ExceptionUtils.sneaky;
import static infra.web.handler.result.SseEmitter.event;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/5/10 16:16
 */
@TestPropertySource(properties = "logging.level.web=debug")
@InfraTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class RestClientServerSentEventsTests {

  private final RestClient restClient;

  public RestClientServerSentEventsTests(@LocalServerPort int port) {
    this.restClient = RestClient.create("http://localhost:" + port);
  }

  @Test
  void events() {
    try (var events = restClient.get()
            .uri("/sse?times=10&sleep=100")
            .retrieve()
            .events(Body.class)) {

      int times = 0;
      for (ServerSentEvent<Body> event : events) {
        assertEvent(event);
        times++;
      }
      assertThat(times).isEqualTo(10);
    }
  }

  @Test
  void asyncEvents() throws InterruptedException {
    restClient.get()
            .uri("/sse?times=10&sleep=1000")
            .async()
            .events(Body.class)
            .mapNull(events -> {
              try (events) {
                int times = 0;
                for (ServerSentEvent<Body> event : events) {
                  assertEvent(event);
                  times++;
                }
                assertThat(times).isEqualTo(10);
              }
            })
            .onFailure(ex -> fail())
            .sync()
    ;
  }

  @Test
  void consume() {
    AtomicInteger times = new AtomicInteger();
    restClient.get().uri("/sse?times=10&sleep=100").retrieve().events(Body.class, event -> {
      times.getAndIncrement();
      assertEvent(event);
    });
    assertThat(times.get()).isEqualTo(10);
  }

  @Test
  void consumeString() {
    AtomicInteger times = new AtomicInteger();
    restClient.get().uri("/sse?times=10&sleep=100").retrieve().<String>events(event -> {
      assertThat(event.data().contains("today")).isTrue();
      assertEvent(event);
      times.getAndIncrement();
    });
    assertThat(times.get()).isEqualTo(10);
  }

  @Test
  void string() {
    int times = 0;
    try (ServerSentEvents<String> events = restClient.get().uri("/sse?times=10&sleep=100").retrieve().events()) {
      for (ServerSentEvent<String> event : events) {
        assertThat(event.data().contains("today")).isTrue();
        assertEvent(event);
        times++;
      }
    }
    assertThat(times).isEqualTo(10);
  }

  @Test
  void parameterizedTypeReference() {
    int times = 0;
    try (ServerSentEvents<Body> events = restClient.get().uri("/sse?times=10&sleep=100").retrieve().events(
            new ParameterizedTypeReference<>() {

            })) {

      for (ServerSentEvent<Body> event : events) {
        assertEvent(event);
        times++;
      }
    }
    assertThat(times).isEqualTo(10);
  }

  private static void assertEvent(ServerSentEvent<?> event) {
    System.err.println(event);
    Object data = event.data();
    if (data instanceof Body body) {
      assertThat(body.name).isEqualTo("today");
    }
    else {
      assertThat(data.toString()).startsWith("{\"name\":\"today\"")
              .contains("age");
    }
    assertThat(event.event()).startsWith("event-name-");
    assertThat(event.id()).startsWith("id-");
    assertThat(event.comment()).isNull();
    assertThat(event.retry()).isNull();
  }

  record Body(String name, int age) {
  }

  @RestController
  @InfraApplication
  static class ServerSentEventsApp {

    @GET
    @RequestMapping("/sse")
    public SseEmitter sseEmitter(@Nullable Integer times, @RequestParam(defaultValue = "200") int sleep) {
      SseEmitter sseEmitter = ResponseBodyEmitter.forServerSentEvents();
      Future.run(sneaky(() -> {
        int events = times == null ? 5 : times;
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < events; i++) {
          sseEmitter.send(event()
                  .id("id-" + i)
                  .name("event-name-" + i)
                  .data(new Body("today", random.nextInt())));

          int timeout = random.nextInt(0, sleep);
          TimeUnit.MILLISECONDS.sleep(timeout);
        }
        sseEmitter.complete();
      }));

      return sseEmitter;
    }

  }

}

