package infra.samples;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import infra.http.MediaType;
import infra.http.ServerSentEvent;
import infra.web.client.RestClient;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2026/5/8 23:08
 */
class SSEClientTests {

  private final RestClient restClient = RestClient.create("http://localhost:8080");

  @Test
  void test() throws IOException {
    try (InputStream inputStream = restClient.get().uri("/sse/simple?times=10&sleep=5")
            .accept(MediaType.TEXT_EVENT_STREAM).execute(false).getBody()) {

      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
      reader.lines().forEach(System.out::println);
    }
  }

  @Test
  void eventStream() {
    try (var events = restClient.get()
            .uri("/sse/simple?times=10&sleep=5")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .events().iterator()) {

      while (events.hasNext()) {
        ServerSentEvent<String> event = events.next();
        System.out.println(event);
      }
    }

  }

}
