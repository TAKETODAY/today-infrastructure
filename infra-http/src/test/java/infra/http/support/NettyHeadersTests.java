/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.http.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.netty.handler.codec.CharSequenceValueConverter;
import io.netty.handler.codec.DefaultHeadersImpl;
import io.netty.handler.codec.Headers;
import io.netty.handler.codec.http.DefaultHttpHeadersFactory;
import io.netty.util.AsciiString;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class NettyHeadersTests {

  private NettyHeaders nettyHeaders;

  private Headers<CharSequence, CharSequence, ?> underlyingHeaders;

  @BeforeEach
  void setUp() {
    underlyingHeaders = newHeaders();
    nettyHeaders = new NettyHeaders(underlyingHeaders);
  }

  private static Headers<CharSequence, CharSequence, ?> newHeaders() {
    DefaultHttpHeadersFactory factory = DefaultHttpHeadersFactory.headersFactory();
    return new DefaultHeadersImpl<>(
            AsciiString.CASE_INSENSITIVE_HASHER,
            CharSequenceValueConverter.INSTANCE,
            factory.getNameValidator(),
            16,
            factory.getValueValidator());
  }

  @Test
  void getFirst_shouldReturnFirstHeaderValue() {
    underlyingHeaders.add("Content-Type", "application/json");
    underlyingHeaders.add("Content-Type", "text/plain");

    assertThat(nettyHeaders.getFirst("Content-Type")).isEqualTo("application/json");
  }

  @Test
  void getFirst_withNonExistentHeader_shouldReturnNull() {
    assertThat(nettyHeaders.getFirst("Non-Existent")).isNull();
  }

  @Test
  void add_shouldAddHeader() {
    nettyHeaders.add("Custom-Header", "value1");

    assertThat(underlyingHeaders.get("Custom-Header")).isEqualTo("value1");
  }

  @Test
  void add_withNullValue_shouldNotAddHeader() {
    nettyHeaders.add("Custom-Header", (String) null);

    assertThat(underlyingHeaders.contains("Custom-Header")).isFalse();
  }

  @Test
  void addAll_shouldAddMultipleValues() {
    nettyHeaders.add("Custom-Header", Arrays.asList("value1", "value2", "value3"));

    assertThat(underlyingHeaders.getAll("Custom-Header"))
            .containsExactly("value1", "value2", "value3");
  }

  @Test
  void setHeader_shouldReplaceExistingHeader() {
    underlyingHeaders.add("Custom-Header", "old-value");

    nettyHeaders.setHeader("Custom-Header", "new-value");

    assertThat(underlyingHeaders.getAll("Custom-Header")).containsExactly("new-value");
  }

  @Test
  void setHeader_withCollection_shouldReplaceExistingHeader() {
    underlyingHeaders.add("Custom-Header", "old-value");

    nettyHeaders.setHeader("Custom-Header", Arrays.asList("value1", "value2"));

    assertThat(underlyingHeaders.getAll("Custom-Header"))
            .containsExactly("value1", "value2");
  }

  @Test
  void toSingleValueMap_shouldReturnMapWithSingleValues() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header1", "value2");
    underlyingHeaders.add("Header2", "value3");

    Map<String, String> result = nettyHeaders.toSingleValueMap();
    assertThat(result).containsEntry("Header1", "value1");
    assertThat(result).containsEntry("Header2", "value3");
    assertThat(result).hasSize(2);
  }

  @Test
  void size_shouldReturnNumberOfUniqueHeaderNames() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header1", "value2");
    underlyingHeaders.add("Header2", "value3");

    assertThat(nettyHeaders.size()).isEqualTo(2);
  }

  @Test
  void isEmpty_whenNoHeaders_shouldReturnTrue() {
    assertThat(nettyHeaders.isEmpty()).isTrue();
  }

  @Test
  void isEmpty_whenHeadersPresent_shouldReturnFalse() {
    underlyingHeaders.add("Header1", "value1");

    assertThat(nettyHeaders.isEmpty()).isFalse();
  }

  @Test
  void containsHeader_withExisting_shouldReturnTrue() {
    underlyingHeaders.add("Header1", "value1");

    assertThat(nettyHeaders.contains("Header1")).isTrue();
    assertThat(nettyHeaders.contains("header1")).isTrue();
  }

  @Test
  void containsHeader_withNonExistent_shouldReturnFalse() {
    assertThat(nettyHeaders.contains("Non-Existent")).isFalse();
  }

  @Test
  void containsHeaderValue() {
    nettyHeaders.add("Test-Header", "value");

    assertThat(nettyHeaders.contains("test-header", "value")).isTrue();
    assertThat(nettyHeaders.contains("test-header", "value1")).isFalse();
    assertThat(nettyHeaders.contains("test-header1", "value")).isFalse();
  }

  @Test
  void get_withExistingHeader_shouldReturnAllValues() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header1", "value2");

    assertThat(nettyHeaders.get("Header1")).containsExactly("value1", "value2");
  }

  @Test
  void valueLists_shouldBeUnmodifiableSnapshotsAcrossAccessPaths() {
    underlyingHeaders.add("Header1", "first");
    underlyingHeaders.add("Header1", "second");
    List<List<String>> snapshots = new ArrayList<>();
    List<String> values = nettyHeaders.get("Header1");
    assertThat(values).isNotNull();
    snapshots.add(values);
    Map.Entry<String, List<String>> entry = nettyHeaders.entries().iterator().next();
    snapshots.add(entry.getValue());
    nettyHeaders.forEach((name, list) -> snapshots.add(list));
    snapshots.add(nettyHeaders.set("Header1", "replacement"));
    snapshots.add(nettyHeaders.set("Header1", List.of("next")));
    snapshots.add(entry.setValue(List.of("last")));
    snapshots.add(nettyHeaders.remove("Header1"));

    assertThat(snapshots).containsExactly(
            List.of("first", "second"), List.of("first", "second"),
            List.of("first", "second"), List.of("first", "second"),
            List.of("replacement"), List.of("next"), List.of("last"));
    for (List<String> snapshot : snapshots) {
      assertThatExceptionOfType(UnsupportedOperationException.class)
              .isThrownBy(() -> snapshot.add("changed"));
      assertThatExceptionOfType(UnsupportedOperationException.class)
              .isThrownBy(() -> snapshot.set(0, "changed"));
      assertThatExceptionOfType(UnsupportedOperationException.class)
              .isThrownBy(() -> snapshot.remove(0));
      assertThatExceptionOfType(UnsupportedOperationException.class)
              .isThrownBy(snapshot::clear);
    }
    assertThat(nettyHeaders.get("Header1")).isNull();
    assertThat(entry.getValue()).isEmpty();
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> entry.getValue().add("value"));
  }

  @Test
  void valueSnapshot_shouldConvertMutableCharSequencesLazily() {
    StringBuilder value = new StringBuilder("first");
    underlyingHeaders.add("Header1", value);
    List<String> snapshot = nettyHeaders.get("Header1");

    assertThat(snapshot).containsExactly("first");
    value.append("-changed");
    underlyingHeaders.remove("Header1");

    assertThat(snapshot).containsExactly("first-changed");
  }

  @Test
  void entryValue_shouldReadCurrentValuesWithoutChangingPreviousSnapshots() {
    nettyHeaders.add("Header1", "first");
    Map.Entry<String, List<String>> entry = nettyHeaders.entries().iterator().next();
    List<String> snapshot = entry.getValue();

    underlyingHeaders.add("Header1", "second");
    assertThat(snapshot).containsExactly("first");
    assertThat(entry.getValue()).containsExactly("first", "second");

    underlyingHeaders.clear();
    assertThat(snapshot).containsExactly("first");
    assertThat(entry.getValue()).isEmpty();
  }

  @Test
  void get_withNonExistentHeader_shouldReturnNull() {
    assertThat(nettyHeaders.get("Non-Existent")).isNull();
  }

  @Test
  void set_shouldReplaceHeaderValues() {
    underlyingHeaders.add("Header1", "old-value");

    List<String> previous = nettyHeaders.set("Header1", Arrays.asList("new-value1", "new-value2"));

    assertThat(previous).containsExactly("old-value");
    assertThat(underlyingHeaders.getAll("Header1")).containsExactly("new-value1", "new-value2");
  }

  @Test
  void remove_withExistingHeader_shouldRemoveAndReturnValues() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header1", "value2");

    List<String> removed = nettyHeaders.remove("Header1");

    assertThat(removed).containsExactly("value1", "value2");
    assertThat(underlyingHeaders.contains("Header1")).isFalse();
  }

  @Test
  void remove_withNonExistentHeader_shouldReturnNull() {
    assertThat(nettyHeaders.remove("Non-Existent")).isNull();
  }

  @Test
  void setAll_shouldSetAllHeaders() {
    Map<String, List<String>> headersToSet = new HashMap<>();
    headersToSet.put("Header1", Arrays.asList("value1", "value2"));
    headersToSet.put("Header2", List.of("value3"));

    nettyHeaders.setAll(headersToSet);

    assertThat(underlyingHeaders.getAll("Header1")).containsExactly("value1", "value2");
    assertThat(underlyingHeaders.getAll("Header2")).containsExactly("value3");
  }

  @Test
  void clear_shouldRemoveAllHeaders() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header2", "value2");

    nettyHeaders.clear();

    assertThat(underlyingHeaders.isEmpty()).isTrue();
  }

  @Test
  void names_shouldReturnHeaderNames() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header2", "value2");

    assertThat(nettyHeaders.names()).containsExactlyInAnyOrder("Header1", "Header2");
  }

  @Test
  void entries_shouldReturnHeaderEntries() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header1", "value2");
    underlyingHeaders.add("Header2", "value3");

    Set<Map.Entry<String, List<String>>> entrySet = new HashSet<>(nettyHeaders.entries());
    assertThat(entrySet).hasSize(2);

    boolean foundHeader1 = false;
    boolean foundHeader2 = false;

    for (Map.Entry<String, List<String>> entry : entrySet) {
      if ("Header1".equals(entry.getKey())) {
        assertThat(entry.getValue()).containsExactly("value1", "value2");
        foundHeader1 = true;
      }
      else if ("Header2".equals(entry.getKey())) {
        assertThat(entry.getValue()).containsExactly("value3");
        foundHeader2 = true;
      }
    }

    assertThat(foundHeader1).isTrue();
    assertThat(foundHeader2).isTrue();
  }

  @Test
  void entries_sizeShouldCountUniqueHeaderNamesRatherThanValues() {
    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header1", "value2");
    underlyingHeaders.add("Header2", "value3");

    Set<Map.Entry<String, List<String>>> entries = nettyHeaders.entries();

    assertThat(entries.size()).isEqualTo(2);
    assertThat(entries.size()).isEqualTo(nettyHeaders.size());
    assertThat(entries.stream().count()).isEqualTo(2);
    assertThat(entries.toArray()).hasSize(2);
  }

  @Test
  void entries_sizeShouldReflectUnderlyingHeaderChanges() {
    Set<Map.Entry<String, List<String>>> entries = nettyHeaders.entries();
    assertThat(entries.size()).isZero();

    underlyingHeaders.add("Header1", "value1");
    underlyingHeaders.add("Header1", "value2");
    assertThat(entries.size()).isEqualTo(1);

    underlyingHeaders.add("Header2", "value3");
    assertThat(entries.size()).isEqualTo(2);

    underlyingHeaders.remove("Header1");
    assertThat(entries.size()).isEqualTo(1);

    underlyingHeaders.clear();
    assertThat(entries.size()).isZero();
  }

  @Test
  void entriesValue_shouldUpdateUnderlyingHeaders() {
    underlyingHeaders.add("Header1", "old-value");

    Iterator<Map.Entry<String, List<String>>> iterator = nettyHeaders.entries().iterator();
    Map.Entry<String, List<String>> entry = iterator.next();
    List<String> oldValue = entry.setValue(List.of("new-value"));

    assertThat(oldValue).containsExactly("old-value");
    assertThat(underlyingHeaders.getAll("Header1")).containsExactly("new-value");
  }

  @Test
  void equals_shouldReturnTrueForEqualHeaders() {
    NettyHeaders headers1 = new NettyHeaders(newHeaders());
    NettyHeaders headers2 = new NettyHeaders(newHeaders());

    assertThat(headers1).isEqualTo(headers1);
    assertThat(headers1).isEqualTo(headers2);

    headers1.add("Content-Type", "application/json");
    headers1.add("X-Custom", "value");
    headers2.add("Content-Type", "application/json");
    headers2.add("X-Custom", "value");
    assertThat(headers1).isEqualTo(headers2);

    headers2.add("X-Custom", "different-value");
    assertThat(headers1).isNotEqualTo(headers2);

    assertThat(headers1).isNotEqualTo(null);
    assertThat(headers1).isNotEqualTo("not a header");
  }

  @Test
  void hashCode_shouldBeConsistentWithEquals() {
    NettyHeaders headers1 = new NettyHeaders(newHeaders());
    NettyHeaders headers2 = new NettyHeaders(newHeaders());

    assertThat(headers1.hashCode()).isEqualTo(headers2.hashCode());

    headers1.add("Content-Type", "application/json");
    headers1.add("X-Custom", "value");
    headers2.add("Content-Type", "application/json");
    headers2.add("X-Custom", "value");
    assertThat(headers1.hashCode()).isEqualTo(headers2.hashCode());

    headers2.add("X-Custom", "different-value");
    assertThat(headers1).isNotEqualTo(headers2);
  }

  @Test
  void forEach() {
    NettyHeaders headers1 = new NettyHeaders(newHeaders());
    NettyHeaders headers2 = new NettyHeaders(newHeaders());
    headers1.add("Content-Type", "application/json");
    headers1.add("X-Custom", "value");

    headers1.forEach(headers2::add);

    assertThat(headers1).isEqualTo(headers2);
  }

  @Test
  void shouldBeLiveViewOfUnderlyingHeaders() {
    underlyingHeaders.add("X-Before", "value1");
    assertThat(nettyHeaders.getFirst("X-Before")).isEqualTo("value1");

    nettyHeaders.add("X-After", "value2");
    assertThat(underlyingHeaders.get("X-After")).isEqualTo("value2");

    underlyingHeaders.clear();
    assertThat(nettyHeaders.isEmpty()).isTrue();
  }

  @Test
  void lowercaseNames_shouldNormalizeHeaderNames() {
    Headers<CharSequence, CharSequence, ?> underlying = new DefaultHeadersImpl<>(
            AsciiString.CASE_SENSITIVE_HASHER,
            CharSequenceValueConverter.INSTANCE,
            DefaultHttpHeadersFactory.headersFactory().getNameValidator());

    NettyHeaders headers = new NettyHeaders(underlying, true);
    headers.add("Content-Type", "application/json");

    assertThat(headers.names()).containsExactly("content-type");
    assertThat(headers.getFirst("Content-Type")).isEqualTo("application/json");
    assertThat(headers.contains("CONTENT-TYPE")).isTrue();
    assertThat(headers.remove("Content-Type")).containsExactly("application/json");
    assertThat(underlying.isEmpty()).isTrue();
  }

}
