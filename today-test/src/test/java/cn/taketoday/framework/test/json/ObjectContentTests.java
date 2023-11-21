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

package cn.taketoday.framework.test.json;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ObjectContent}.
 *
 * @author Phillip Webb
 */
class ObjectContentTests {

  private static final ExampleObject OBJECT = new ExampleObject();

  private static final ResolvableType TYPE = ResolvableType.forClass(ExampleObject.class);

  @Test
  void createWhenObjectIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ObjectContent<ExampleObject>(TYPE, null))
            .withMessageContaining("Object is required");
  }

  @Test
  void createWhenTypeIsNullShouldCreateContent() {
    ObjectContent<ExampleObject> content = new ObjectContent<>(null, OBJECT);
    assertThat(content).isNotNull();
  }

  @Test
  void assertThatShouldReturnObjectContentAssert() {
    ObjectContent<ExampleObject> content = new ObjectContent<>(TYPE, OBJECT);
    assertThat(content.assertThat()).isInstanceOf(ObjectContentAssert.class);
  }

  @Test
  void getObjectShouldReturnObject() {
    ObjectContent<ExampleObject> content = new ObjectContent<>(TYPE, OBJECT);
    assertThat(content.getObject()).isEqualTo(OBJECT);
  }

  @Test
  void toStringWhenHasTypeShouldReturnString() {
    ObjectContent<ExampleObject> content = new ObjectContent<>(TYPE, OBJECT);
    assertThat(content.toString()).isEqualTo("ObjectContent " + OBJECT + " created from " + TYPE);
  }

  @Test
  void toStringWhenHasNoTypeShouldReturnString() {
    ObjectContent<ExampleObject> content = new ObjectContent<>(null, OBJECT);
    assertThat(content.toString()).isEqualTo("ObjectContent " + OBJECT);
  }

}
