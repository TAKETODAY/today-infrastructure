package cn.taketoday.util;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/22 17:24
 */
class ArrayIteratorTests {

  @Test
  void iterator() {
    String[] strings = { "1", "2", "3" };
    ArrayIterator<String> arrayIterator = new ArrayIterator<>(strings);

    assertThat(arrayIterator.hasNext()).isTrue();
    assertThat(arrayIterator.next()).isEqualTo("1");

    assertThat(arrayIterator.hasNext()).isTrue();
    assertThat(arrayIterator.next()).isEqualTo("2");

    assertThat(arrayIterator.hasNext()).isTrue();
    assertThat(arrayIterator.next()).isEqualTo("3");

    assertThat(arrayIterator.hasNext()).isFalse();

    assertThatThrownBy(arrayIterator::next).isInstanceOf(NoSuchElementException.class);

  }

  @Test
  void remove() {
    String[] strings = { "1", "2", "3" };
    ArrayIterator<String> arrayIterator = new ArrayIterator<>(strings);
    assertThatThrownBy(arrayIterator::remove).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void offset() {
    String[] strings = { "1", "2", "3" };
    ArrayIterator<String> arrayIterator = new ArrayIterator<>(strings, 1, 2);

    assertThat(arrayIterator.hasNext()).isTrue();
    assertThat(arrayIterator.next()).isEqualTo("2");

    assertThat(arrayIterator.hasNext()).isTrue();
    assertThat(arrayIterator.next()).isEqualTo("3");

    assertThat(arrayIterator.hasNext()).isFalse();
  }

}