/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 17:32
 */
class AttributeAccessorTests {

  @Test
  void setAndGetAttribute() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.setAttribute("name", "value");
    assertThat(accessor.getAttribute("name")).isEqualTo("value");
  }

  @Test
  void removeAttribute() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.setAttribute("name", "value");
    Object removed = accessor.removeAttribute("name");
    assertThat(removed).isEqualTo("value");
    assertThat(accessor.getAttribute("name")).isNull();
  }

  @Test
  void hasAttribute() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    assertThat(accessor.hasAttribute("name")).isFalse();
    accessor.setAttribute("name", "value");
    assertThat(accessor.hasAttribute("name")).isTrue();
  }

  @Test
  void getAttributeNames() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.setAttribute("name1", "value1");
    accessor.setAttribute("name2", "value2");
    assertThat(accessor.getAttributeNames()).containsExactlyInAnyOrder("name1", "name2");
  }

  @Test
  void attributeNames() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.setAttribute("name1", "value1");
    accessor.setAttribute("name2", "value2");
    assertThat(accessor.attributeNames()).containsExactlyInAnyOrder("name1", "name2");
  }

  @Test
  void hasAttributes() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    assertThat(accessor.hasAttributes()).isFalse();
    accessor.setAttribute("name", "value");
    assertThat(accessor.hasAttributes()).isTrue();
  }

  @Test
  void getAttributes() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.setAttribute("name1", "value1");
    accessor.setAttribute("name2", "value2");

    Map<String, Object> attributes = accessor.getAttributes();
    assertThat(attributes)
            .containsEntry("name1", "value1")
            .containsEntry("name2", "value2")
            .hasSize(2);
  }

  @Test
  void computeAttributeForNewValue() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    String value = accessor.computeAttribute("name", name -> "computed");
    assertThat(value).isEqualTo("computed");
    assertThat(accessor.getAttribute("name")).isEqualTo("computed");
  }

  @Test
  void computeAttributeForExistingValue() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.setAttribute("name", "existing");
    String value = accessor.computeAttribute("name", name -> "computed");
    assertThat(value).isEqualTo("existing");
  }

  @Test
  void computeAttributeWithNullValueThrowsException() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    assertThatThrownBy(() -> accessor.computeAttribute("name", name -> null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("name");
  }

  @Test
  void setAttributesFromMap() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    Map<String, Object> attributes = Map.of("name1", "value1", "name2", "value2");
    accessor.setAttributes(attributes);
    assertThat(accessor.getAttributes()).isEqualTo(attributes);
  }

  @Test
  void copyFromAnotherAccessor() {
    SimpleAttributeAccessor source = new SimpleAttributeAccessor();
    source.setAttribute("name1", "value1");
    source.setAttribute("name2", "value2");

    SimpleAttributeAccessor target = new SimpleAttributeAccessor();
    target.copyFrom(source);
    assertThat(target.getAttributes()).isEqualTo(source.getAttributes());
  }

  @Test
  void clearAttributes() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.setAttribute("name1", "value1");
    accessor.setAttribute("name2", "value2");
    accessor.clearAttributes();
    assertThat(accessor.hasAttributes()).isFalse();
  }

  private static class SimpleAttributeAccessor implements AttributeAccessor {
    private final Map<String, Object> attributes = new HashMap<>();

    @Override
    public void setAttribute(String name, Object value) {
      if (value != null) {
        attributes.put(name, value);
      }
      else {
        removeAttribute(name);
      }
    }

    @Override
    public void setAttributes(Map<String, Object> attributes) {
      if (attributes != null) {
        this.attributes.putAll(attributes);
      }
    }

    @Override
    public Object getAttribute(String name) {
      return attributes.get(name);
    }

    @Override
    public Object removeAttribute(String name) {
      return attributes.remove(name);
    }

    @Override
    public boolean hasAttribute(String name) {
      return attributes.containsKey(name);
    }

    @Override
    public String[] getAttributeNames() {
      return attributes.keySet().toArray(new String[0]);
    }

    @Override
    public boolean hasAttributes() {
      return !attributes.isEmpty();
    }

    @Override
    public Map<String, Object> getAttributes() {
      return new HashMap<>(attributes);
    }

    @Override
    public void copyFrom(AttributeAccessor source) {
      setAttributes(source.getAttributes());
    }

    @Override
    public void clearAttributes() {
      attributes.clear();
    }
  }

}