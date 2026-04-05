package infra.core;

import org.junit.jupiter.api.Test;

import java.io.Console;
import java.io.Reader;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.SortedSet;
import java.util.concurrent.Future;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/4/5 22:36
 */
class JavaVersionHintsTests {

  @Test
  void shouldRegisterResourceHints() {
    RuntimeHints runtimeHints = new RuntimeHints();
    new JavaVersionHints().registerHints(runtimeHints, getClass().getClassLoader());
    assertThat(RuntimeHintsPredicates.reflection().onType(String.class)).accepts(runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection().onType(Reader.class)).accepts(runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection().onType(Class.class)).accepts(runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection().onType(NumberFormat.class)).accepts(runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection().onType(Console.class)).accepts(runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection().onType(Duration.class)).accepts(runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection().onType(Future.class)).accepts(runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection().onType(SortedSet.class)).accepts(runtimeHints);
  }

}