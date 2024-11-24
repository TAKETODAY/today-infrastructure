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

package infra.core.env;

import org.junit.jupiter.api.Test;

import java.util.Iterator;

import infra.core.testfixture.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Chris Beams
 * @author Juergen Hoeller
 */
class PropertySourcesTests {

  @Test
  void test() {
    PropertySources sources = new PropertySources();
    sources.addLast(new MockPropertySource("b").withProperty("p1", "bValue"));
    sources.addLast(new MockPropertySource("d").withProperty("p1", "dValue"));
    sources.addLast(new MockPropertySource("f").withProperty("p1", "fValue"));

    assertThat(sources.size()).isEqualTo(3);
    assertThat(sources.contains("a")).isFalse();
    assertThat(sources.contains("b")).isTrue();
    assertThat(sources.contains("c")).isFalse();
    assertThat(sources.contains("d")).isTrue();
    assertThat(sources.contains("e")).isFalse();
    assertThat(sources.contains("f")).isTrue();
    assertThat(sources.contains("g")).isFalse();

    assertThat(sources.get("b")).isNotNull();
    assertThat(sources.get("b").getProperty("p1")).isEqualTo("bValue");
    assertThat(sources.get("d")).isNotNull();
    assertThat(sources.get("d").getProperty("p1")).isEqualTo("dValue");

    sources.addBefore("b", new MockPropertySource("a"));
    sources.addAfter("b", new MockPropertySource("c"));

    assertThat(sources.size()).isEqualTo(5);
    assertThat(sources.precedenceOf(PropertySource.named("a"))).isEqualTo(0);
    assertThat(sources.precedenceOf(PropertySource.named("b"))).isEqualTo(1);
    assertThat(sources.precedenceOf(PropertySource.named("c"))).isEqualTo(2);
    assertThat(sources.precedenceOf(PropertySource.named("d"))).isEqualTo(3);
    assertThat(sources.precedenceOf(PropertySource.named("f"))).isEqualTo(4);

    sources.addBefore("f", new MockPropertySource("e"));
    sources.addAfter("f", new MockPropertySource("g"));

    assertThat(sources.size()).isEqualTo(7);
    assertThat(sources.precedenceOf(PropertySource.named("a"))).isEqualTo(0);
    assertThat(sources.precedenceOf(PropertySource.named("b"))).isEqualTo(1);
    assertThat(sources.precedenceOf(PropertySource.named("c"))).isEqualTo(2);
    assertThat(sources.precedenceOf(PropertySource.named("d"))).isEqualTo(3);
    assertThat(sources.precedenceOf(PropertySource.named("e"))).isEqualTo(4);
    assertThat(sources.precedenceOf(PropertySource.named("f"))).isEqualTo(5);
    assertThat(sources.precedenceOf(PropertySource.named("g"))).isEqualTo(6);

    sources.addLast(new MockPropertySource("a"));
    assertThat(sources.size()).isEqualTo(7);
    assertThat(sources.precedenceOf(PropertySource.named("b"))).isEqualTo(0);
    assertThat(sources.precedenceOf(PropertySource.named("c"))).isEqualTo(1);
    assertThat(sources.precedenceOf(PropertySource.named("d"))).isEqualTo(2);
    assertThat(sources.precedenceOf(PropertySource.named("e"))).isEqualTo(3);
    assertThat(sources.precedenceOf(PropertySource.named("f"))).isEqualTo(4);
    assertThat(sources.precedenceOf(PropertySource.named("g"))).isEqualTo(5);
    assertThat(sources.precedenceOf(PropertySource.named("a"))).isEqualTo(6);

    sources.addFirst(new MockPropertySource("a"));
    assertThat(sources.size()).isEqualTo(7);
    assertThat(sources.precedenceOf(PropertySource.named("a"))).isEqualTo(0);
    assertThat(sources.precedenceOf(PropertySource.named("b"))).isEqualTo(1);
    assertThat(sources.precedenceOf(PropertySource.named("c"))).isEqualTo(2);
    assertThat(sources.precedenceOf(PropertySource.named("d"))).isEqualTo(3);
    assertThat(sources.precedenceOf(PropertySource.named("e"))).isEqualTo(4);
    assertThat(sources.precedenceOf(PropertySource.named("f"))).isEqualTo(5);
    assertThat(sources.precedenceOf(PropertySource.named("g"))).isEqualTo(6);

    assertThat(PropertySource.named("a")).isEqualTo(sources.remove("a"));
    assertThat(sources.size()).isEqualTo(6);
    assertThat(sources.contains("a")).isFalse();

    assertThat((Object) sources.remove("a")).isNull();
    assertThat(sources.size()).isEqualTo(6);

    String bogusPS = "bogus";
    assertThatIllegalArgumentException().isThrownBy(() ->
                    sources.addAfter(bogusPS, new MockPropertySource("h")))
            .withMessageContaining("does not exist");

    sources.addFirst(new MockPropertySource("a"));
    assertThat(sources.size()).isEqualTo(7);
    assertThat(sources.precedenceOf(PropertySource.named("a"))).isEqualTo(0);
    assertThat(sources.precedenceOf(PropertySource.named("b"))).isEqualTo(1);
    assertThat(sources.precedenceOf(PropertySource.named("c"))).isEqualTo(2);

    sources.replace("a", new MockPropertySource("a-replaced"));
    assertThat(sources.size()).isEqualTo(7);
    assertThat(sources.precedenceOf(PropertySource.named("a-replaced"))).isEqualTo(0);
    assertThat(sources.precedenceOf(PropertySource.named("b"))).isEqualTo(1);
    assertThat(sources.precedenceOf(PropertySource.named("c"))).isEqualTo(2);

    sources.replace("a-replaced", new MockPropertySource("a"));

    assertThatIllegalArgumentException().isThrownBy(() ->
                    sources.replace(bogusPS, new MockPropertySource("bogus-replaced")))
            .withMessageContaining("does not exist");

    assertThatIllegalArgumentException().isThrownBy(() ->
                    sources.addBefore("b", new MockPropertySource("b")))
            .withMessageContaining("cannot be added relative to itself");

    assertThatIllegalArgumentException().isThrownBy(() ->
                    sources.addAfter("b", new MockPropertySource("b")))
            .withMessageContaining("cannot be added relative to itself");
  }

  @Test
  void getNonExistentPropertySourceReturnsNull() {
    PropertySources sources = new PropertySources();
    assertThat(sources.get("bogus")).isNull();
  }

  @Test
  void iteratorContainsPropertySource() {
    PropertySources sources = new PropertySources();
    sources.addLast(new MockPropertySource("test"));

    Iterator<PropertySource<?>> it = sources.iterator();
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next().getName()).isEqualTo("test");

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(
            it::remove);
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void iteratorIsEmptyForEmptySources() {
    PropertySources sources = new PropertySources();
    Iterator<PropertySource<?>> it = sources.iterator();
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  void streamContainsPropertySource() {
    PropertySources sources = new PropertySources();
    sources.addLast(new MockPropertySource("test"));

    assertThat(sources.stream()).isNotNull();
    assertThat(sources.stream().count()).isEqualTo(1L);
    assertThat(sources.stream().anyMatch(source -> "test".equals(source.getName()))).isTrue();
    assertThat(sources.stream().anyMatch(source -> "bogus".equals(source.getName()))).isFalse();
  }

  @Test
  void streamIsEmptyForEmptySources() {
    PropertySources sources = new PropertySources();
    assertThat(sources.stream()).isNotNull();
    assertThat(sources.stream().count()).isEqualTo(0L);
  }

}
