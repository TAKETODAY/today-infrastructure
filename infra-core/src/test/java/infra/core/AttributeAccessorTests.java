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