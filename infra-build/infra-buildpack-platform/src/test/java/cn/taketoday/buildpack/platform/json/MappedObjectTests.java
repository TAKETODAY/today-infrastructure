/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.buildpack.platform.json;

import com.fasterxml.jackson.databind.JsonNode;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;

import cn.taketoday.buildpack.platform.json.MappedObjectTests.TestMappedObject.Person;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MappedObject}.
 *
 * @author Phillip Webb
 */
class MappedObjectTests extends AbstractJsonTests {

  private final TestMappedObject mapped;

  MappedObjectTests() throws IOException {
    this.mapped = TestMappedObject.of(getContent("test-mapped-object.json"));
  }

  @Test
  void ofReadsJson() {
    assertThat(this.mapped.getNode()).isNotNull();
  }

  @Test
  void valueAtWhenStringReturnsValue() {
    assertThat(this.mapped.valueAt("/string", String.class)).isEqualTo("stringvalue");
  }

  @Test
  void valueAtWhenStringArrayReturnsValue() {
    assertThat(this.mapped.valueAt("/stringarray", String[].class)).containsExactly("a", "b");
  }

  @Test
  void valueAtWhenMissingReturnsNull() {
    assertThat(this.mapped.valueAt("/missing", String.class)).isNull();
  }

  @Test
  void valueAtWhenInterfaceReturnsProxy() {
    Person person = this.mapped.valueAt("/person", Person.class);
    Assertions.assertThat(person.getName().getFirst()).isEqualTo("spring");
    Assertions.assertThat(person.getName().getLast()).isEqualTo("boot");
  }

  @Test
  void valueAtWhenInterfaceAndMissingReturnsProxy() {
    Person person = this.mapped.valueAt("/missing", Person.class);
    Assertions.assertThat(person.getName().getFirst()).isNull();
    Assertions.assertThat(person.getName().getLast()).isNull();
  }

  @Test
  void valueAtWhenActualPropertyStartsWithUppercaseReturnsValue() {
    assertThat(this.mapped.valueAt("/startsWithUppercase", String.class)).isEqualTo("value");
  }

  @Test
  void valueAtWhenDefaultMethodReturnsValue() {
    Person person = this.mapped.valueAt("/person", Person.class);
    Assertions.assertThat(person.getName().getFullName()).isEqualTo("dr spring boot");
  }

  /**
   * {@link MappedObject} for testing.
   */
  static class TestMappedObject extends MappedObject {

    TestMappedObject(JsonNode node) {
      super(node, MethodHandles.lookup());
    }

    static TestMappedObject of(InputStream content) throws IOException {
      return of(content, TestMappedObject::new);
    }

    interface Person {

      Name getName();

      interface Name {

        String getFirst();

        String getLast();

        default String getFullName() {
          String title = valueAt(this, "/title", String.class);
          return title + " " + getFirst() + " " + getLast();
        }

      }

    }

  }

}
