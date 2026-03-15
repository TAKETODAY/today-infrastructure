package infra.http;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/3/15 13:27
 */
class ServerSentEventTests {

  @ParameterizedTest(name = "{1}")
  @MethodSource("newLineCharacters")
  void rejectsInvalidId(String newLine, String description) {
    assertThatIllegalArgumentException().isThrownBy(() ->
            ServerSentEvent.<String>builder().id("first" + newLine + "second").build());
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("newLineCharacters")
  void rejectsInvalidEvent(String newLine, String description) {
    assertThatIllegalArgumentException().isThrownBy(() ->
            ServerSentEvent.<String>builder().event("first" + newLine + "second").build());
  }

  private static Stream<Arguments> newLineCharacters() {
    return Stream.of(
            Arguments.of("\n", "LF"),
            Arguments.of("\r", "CR"),
            Arguments.of("\r\n", "CRLF")
    );
  }

}