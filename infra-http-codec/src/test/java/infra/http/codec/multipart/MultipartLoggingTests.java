/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.http.codec.multipart;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.codec.Hints;
import infra.http.HttpLogging;
import infra.http.MediaType;
import infra.util.MultiValueMap;
import infra.web.testfixture.http.server.reactive.MockServerHttpRequest;
import infra.web.testfixture.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@Isolated
class MultipartLoggingTests {

  @Test
  void masksPartsByDefaultAndLogsDetailsOnlyWhenEnabled() {
    var type = ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class);
    var reader = new MultipartHttpMessageReader(new DefaultPartHttpMessageReader());
    var writer = new MultipartHttpMessageWriter();
    String body = "--B\r\nContent-Disposition: form-data; name=\"note\"\r\n\r\nprivate-value\r\n--B--\r\n";
    MultiValueMap<String, String> form = MultiValueMap.forLinkedHashMap();
    form.add("note", "private-value");
    try (var readLogs = new LogCapture(MultipartHttpMessageReader.class);
            var writeLogs = new LogCapture(MultipartHttpMessageWriter.class);
            var fallback = new LogCapture(HttpLogging.class)) {
      for (var level : List.of(Level.DEBUG, Level.OFF, Level.TRACE)) {
        readLogs.logger.setLevel(level);
        writeLogs.logger.setLevel(level);
        fallback.logger.setLevel(level);
        reader.setEnableLoggingRequestDetails(level == Level.TRACE);
        writer.setEnableLoggingRequestDetails(level == Level.TRACE);
        var hints = Map.<String, Object>of(Hints.LOG_PREFIX_HINT, "[" + level + "] ");
        var request = MockServerHttpRequest.post("/")
                .contentType(MediaType.parseMediaType("multipart/form-data;boundary=B")).body(body);
        StepVerifier.create(reader.readMono(type, request, hints))
                .assertNext(parts -> assertThat(parts.getFirst("note")).isInstanceOfSatisfying(FormFieldPart.class,
                        part -> assertThat(part.value()).isEqualTo("private-value")))
                .expectComplete().verify(Duration.ofSeconds(5));
        var response = new MockServerHttpResponse();
        StepVerifier.create(writer.write(Mono.just(form), type, MediaType.MULTIPART_FORM_DATA, response, hints))
                .expectComplete().verify(Duration.ofSeconds(5));
        assertThat(response.getBodyAsString().block(Duration.ofSeconds(5))).contains("private-value");
      }
      assertThat(readLogs.messages()).containsExactly("[DEBUG] Parsed parts [note] (content masked)",
              "[TRACE] Parsed {note=[DefaultFormFieldPart{note}]}");
      assertThat(writeLogs.messages()).anyMatch(message -> message.contains("[DEBUG] Encoding parts [note] (content masked)"))
              .anyMatch(message -> message.contains("[TRACE]") && message.contains("private-value"))
              .noneMatch(message -> message.contains("[OFF]"))
              .noneMatch(message -> message.contains("[DEBUG]") && message.contains("private-value"));
    }
  }

  @Test
  void tracesParserAndDiskBackedPartTransitions(@TempDir Path storage) throws Exception {
    var reader = new DefaultPartHttpMessageReader();
    reader.setBlockingOperationScheduler(Schedulers.immediate());
    reader.setFileStorageDirectory(storage);
    reader.setMaxInMemorySize(4);
    var request = MockServerHttpRequest.post("/").contentType(MediaType.parseMediaType("multipart/form-data;boundary=B"))
            .body("--B\r\nContent-Disposition: form-data; name=\"note\"\r\n\r\nok\r\n"
                    + "--B\r\nContent-Disposition: form-data; name=\"upload\"; filename=\"sample.txt\"\r\n\r\nexample body\r\n--B--\r\n");
    try (var parser = new LogCapture(MultipartParser.class); var generator = new LogCapture(PartGenerator.class)) {
      parser.logger.setLevel(Level.TRACE);
      generator.logger.setLevel(Level.TRACE);
      StepVerifier.create(reader.read(ResolvableType.forClass(Part.class), request, Map.of())
                      .concatMap(part -> part.delete().thenReturn(part.name())))
              .expectNext("note", "upload").expectComplete().verify(Duration.ofSeconds(5));
      assertThat(parser.messages()).anyMatch(message -> message.contains("First boundary found"))
              .anyMatch(message -> message.contains("Last boundary found"));
      assertThat(generator.messages()).anyMatch(message -> message.contains("Storing multipart data in file"));
    }
    assertThat(storage).isEmptyDirectory();
  }

  private static final class LogCapture implements AutoCloseable {

    private final Logger logger;

    private final Level previous;

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    LogCapture(Class<?> type) {
      this.logger = (Logger) LoggerFactory.getLogger(type);
      this.previous = this.logger.getLevel();
      this.appender.start();
      this.logger.addAppender(this.appender);
    }

    List<String> messages() {
      return this.appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Override
    public void close() {
      this.logger.setLevel(this.previous);
      this.logger.detachAppender(this.appender);
      this.appender.stop();
    }
  }
}
