package infra.samples;

import org.junit.jupiter.api.Test;

import infra.http.MediaType;
import infra.http.ServerSentEvent;
import infra.web.client.RestClient;
import infra.web.client.SseEventIterator;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2026/5/8 23:08
 */
class SSEClientTests {

  private final RestClient restClient = RestClient.create("http://localhost:8080");

  @Test
  void test() {
    try (SseEventIterator events = restClient.get()
            .uri("/sse/simple?times=10&sleep=5")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .eventStream()) {

      while (events.hasNext()) {
        ServerSentEvent<String> event = events.next();
        System.out.println(event);
      }
    }
  }
}
