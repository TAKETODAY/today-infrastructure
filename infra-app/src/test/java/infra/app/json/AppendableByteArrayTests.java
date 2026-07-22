package infra.app.json;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import infra.util.function.ThrowingConsumer;

import static org.assertj.core.api.Assertions.*;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/7/22 16:34
 */
class AppendableByteArrayTests {

  private static final String string = """
			This is a long(ish) string.
			At least it's longer that the initial size and the overflow size.
			We can write it out and test if the bytes match.
			""";

  @Test
  void writesLargeStringWithExpandingBuffer() throws Exception {
    assertByteArray(StandardCharsets.UTF_8, (appendable) -> appendable.append(string));
    assertByteArray(StandardCharsets.UTF_16, (appendable) -> appendable.append(string));
  }

  @Test
  void writesLargeStringWithLargeBuffer() throws Exception {
    assertByteArray(string.length() * 10, 10, StandardCharsets.UTF_8, (appendable) -> appendable.append(string));
    assertByteArray(string.length() * 10, 10, StandardCharsets.UTF_16, (appendable) -> appendable.append(string));
  }

  @Test
  void writesMultipleSmallStrings() throws Exception {
    assertByteArray(StandardCharsets.UTF_8, (appendable) -> appendable.append("{").append("hello").append("}"));
  }

  @Test
  void writeUsingCache() throws IOException {
    assertByteArray(StandardCharsets.UTF_8, AppendableByteArray::get, (appendable) -> appendable.append(string));
    assertByteArray(StandardCharsets.UTF_8, AppendableByteArray::get, (appendable) -> appendable.append(string));
    assertByteArray(StandardCharsets.UTF_16, AppendableByteArray::get, (appendable) -> appendable.append(string));
    assertByteArray(StandardCharsets.UTF_16, AppendableByteArray::get, (appendable) -> appendable.append(string));
    assertByteArray(StandardCharsets.UTF_8, AppendableByteArray::get, (appendable) -> appendable.append(string));
  }

  private void assertByteArray(Charset charset, ThrowingConsumer<Appendable> action) throws Exception {
    assertByteArray(4, 4, charset, action);
  }

  private void assertByteArray(int initialSize, int expansionSize, Charset charset,
          ThrowingConsumer<Appendable> action) throws IOException {
    assertByteArray(charset, (cs) -> new AppendableByteArray(charset, initialSize, expansionSize), action);
  }

  private void assertByteArray(Charset charset, Function<Charset, AppendableByteArray> factory,
          ThrowingConsumer<Appendable> action) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (OutputStreamWriter writer = new OutputStreamWriter(out, charset)) {
      action.accept(writer);
    }
    AppendableByteArray appendableByteArray = factory.apply(charset);
    action.accept(appendableByteArray);
    assertThat(appendableByteArray.toByteArray()).isEqualTo(out.toByteArray());
  }

}