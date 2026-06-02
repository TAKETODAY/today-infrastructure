/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 17:38
 */
class DefaultAttributeAccessorTests {

  @Test
  void setAndGetNullAttributeRemovesValue() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.setAttribute("name", "value");
    accessor.setAttribute("name", null);
    assertThat(accessor.getAttribute("name")).isNull();
  }

  @Test
  void equalsAndHashCodeWithSameAttributes() {
    SimpleAttributeAccessor accessor1 = new SimpleAttributeAccessor();
    SimpleAttributeAccessor accessor2 = new SimpleAttributeAccessor();

    accessor1.setAttribute("name", "value");
    accessor2.setAttribute("name", "value");

    assertThat(accessor1).isEqualTo(accessor2);
    assertThat(accessor1.hashCode()).isEqualTo(accessor2.hashCode());
  }

  @Test
  void equalsAndHashCodeWithDifferentAttributes() {
    SimpleAttributeAccessor accessor1 = new SimpleAttributeAccessor();
    SimpleAttributeAccessor accessor2 = new SimpleAttributeAccessor();

    accessor1.setAttribute("name1", "value1");
    accessor2.setAttribute("name21", "value21");

    assertThat(accessor1).isNotEqualTo(accessor2);
    assertThat(accessor1.hashCode()).isNotEqualTo(accessor2.hashCode());
  }

  @Test
  void attributeNamesWithNoAttributes() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    assertThat(accessor.attributeNames()).isEmpty();
  }

  @Test
  void copyFromNullSourceThrowsException() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    assertThatThrownBy(() -> accessor.copyAttributeFrom(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Source is required");
  }

  @Test
  void copyFromEmptySource() {
    SimpleAttributeAccessor source = new SimpleAttributeAccessor();
    SimpleAttributeAccessor target = new SimpleAttributeAccessor();
    target.setAttribute("name", "value");

    target.copyAttributeFrom(source);
    assertThat(target.getAttribute("name")).isEqualTo("value");
  }

  @Test
  void equalityWithDifferentTypes() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    assertThat(accessor).isNotEqualTo("not an accessor");
  }

  @Test
  void equalsAndHashCodeWithEmptyAttributes() {
    SimpleAttributeAccessor accessor1 = new SimpleAttributeAccessor();
    SimpleAttributeAccessor accessor2 = new SimpleAttributeAccessor();

    assertThat(accessor1).isEqualTo(accessor2);
    assertThat(accessor1.hashCode()).isEqualTo(accessor2.hashCode());
  }

  @Test
  void equalityWithNullObject() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    assertThat(accessor).isNotEqualTo(null);
  }

  @Test
  void setAttributesFromNullMap() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.setAttribute("name", "value");
    accessor.setAttributes(null);
    assertThat(accessor.getAttribute("name")).isEqualTo("value");
  }

  @Test
  void setAttributesFromEmptyMap() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.setAttribute("name", "value");
    accessor.setAttributes(Collections.emptyMap());
    assertThat(accessor.getAttribute("name")).isEqualTo("value");
  }

  @Test
  void computeAttributeWithExistingNonNullValue() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.setAttribute("name", "existing");

    String computed = accessor.computeAttribute("name", key -> "computed");

    assertThat(computed).isEqualTo("existing");
    assertThat(accessor.getAttribute("name")).isEqualTo("existing");
  }

  @Test
  void computeAttributeWithNoExistingValue() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();

    String computed = accessor.computeAttribute("name", key -> "computed");

    assertThat(computed).isEqualTo("computed");
    assertThat(accessor.getAttribute("name")).isEqualTo("computed");
  }

  @Test
  void copyFromEmptyAccessorPreservesExistingAttributes() {
    SimpleAttributeAccessor source = new SimpleAttributeAccessor();
    SimpleAttributeAccessor target = new SimpleAttributeAccessor();
    target.setAttribute("name", "value");

    target.copyAttributeFrom(source);

    assertThat(target.getAttribute("name")).isEqualTo("value");
  }

  @Test
  void attributeEquality() {
    SimpleAttributeAccessor accessor1 = new SimpleAttributeAccessor();
    SimpleAttributeAccessor accessor2 = new SimpleAttributeAccessor();

    accessor1.setAttribute("key1", "value1");
    accessor1.setAttribute("key2", "value2");

    accessor2.setAttribute("key2", "value2");
    accessor2.setAttribute("key1", "value1");

    assertThat(accessor1).isEqualTo(accessor2);
  }

  @Test
  void computeAttributeWithNullNameThrowsException() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    assertThatThrownBy(() -> accessor.computeAttribute(null, key -> "value"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Name is required");
  }

  @Test
  void computeAttributeWithNullFunctionThrowsException() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    assertThatThrownBy(() -> accessor.computeAttribute("name", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Compute function is required");
  }

  @Test
  void multipleAttributeOperations() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();

    accessor.setAttribute("key1", "value1");
    accessor.setAttribute("key2", "value2");
    accessor.removeAttribute("key1");
    accessor.setAttribute("key3", "value3");
    accessor.setAttribute("key2", null);

    assertThat(accessor.getAttributeNames()).containsExactly("key3");
    assertThat(accessor.getAttribute("key3")).isEqualTo("value3");
  }

  @Test
  void clearAttributesOnEmptyAccessor() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.clearAttributes();
    assertThat(accessor.hasAttributes()).isFalse();
  }

  @Test
  void removeNonExistentAttribute() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    assertThat(accessor.removeAttribute("nonexistent")).isNull();
  }

  @Test
  void setAndGetAttributesWithMixedTypes() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();

    accessor.setAttribute("string", "value");
    accessor.setAttribute("integer", 42);
    accessor.setAttribute("boolean", true);

    assertThat(accessor.getAttribute("string")).isEqualTo("value");
    assertThat(accessor.getAttribute("integer")).isEqualTo(42);
    assertThat(accessor.getAttribute("boolean")).isEqualTo(true);
  }

  @Test
  void computeAttributeThrowsExceptionWhenFunctionReturnsNull() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    assertThatThrownBy(() -> accessor.computeAttribute("name", key -> null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Compute function must not return null for attribute named 'name'");
  }

  @Test
  void getAttributesReturnsNewMapWhenNotInitialized() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    Map<String, Object> attributes = accessor.getAttributes();
    assertThat(attributes).isNotNull().isEmpty();
  }

  @Test
  void copyFromSourceWithNoAttributes() {
    SimpleAttributeAccessor source = new SimpleAttributeAccessor();
    SimpleAttributeAccessor target = new SimpleAttributeAccessor();
    target.setAttribute("key", "value");

    target.copyAttributeFrom(source);

    assertThat(target.getAttribute("key")).isEqualTo("value");
  }

  @Test
  void attributeNamesReturnsEmptyIterableWhenNoAttributes() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    assertThat(accessor.attributeNames()).isEmpty();
  }

  @Test
  void getAttributeNamesReturnsEmptyArrayWhenNoAttributes() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    assertThat(accessor.getAttributeNames()).isEmpty();
  }

  @Test
  void hasAttributeReturnsFalseWhenAttributesNotInitialized() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    assertThat(accessor.hasAttribute("any")).isFalse();
  }

  @Test
  void computeAttributeWithExistingNullValue() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.getAttributes().put("name", null);

    String computed = accessor.computeAttribute("name", key -> "computed");

    assertThat(computed).isEqualTo("computed");
    assertThat(accessor.getAttribute("name")).isEqualTo("computed");
  }

  @Test
  void getAttributesCreatesNewMapWhenAttributesIsNull() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    Map<String, Object> attributes = accessor.getAttributes();
    Map<String, Object> attributes2 = accessor.getAttributes();
    assertThat(attributes).isNotNull().isEmpty();
    assertThat(attributes).isSameAs(attributes2);
  }

  @Test
  void copyFromOtherAttributeAccessorType() {
    AttributeAccessor source = new AttributeAccessor() {
      private final Map<String, Object> attrs = new HashMap<>();

      @Override
      public void setAttribute(String name, Object value) {
        attrs.put(name, value);
      }

      @Override
      public void setAttributes(@Nullable Map<String, Object> attributes) {
        attrs.putAll(attributes);
      }

      @Override
      public Object getAttribute(String name) {
        return attrs.get(name);
      }

      @Override
      public Object removeAttribute(String name) {
        return attrs.remove(name);
      }

      @Override
      public boolean hasAttribute(String name) {
        return attrs.containsKey(name);
      }

      @Override
      public String[] getAttributeNames() {
        return attrs.keySet().toArray(new String[0]);
      }

      @Override
      public boolean hasAttributes() {
        return false;
      }

      @Override
      public Map<String, Object> getAttributes() {
        return new HashMap<>(attrs);
      }

      @Override
      public void copyAttributeFrom(AttributeAccessor source) {

      }

      @Override
      public void clearAttributes() {

      }
    };

    source.setAttribute("key", "value");
    SimpleAttributeAccessor target = new SimpleAttributeAccessor();
    target.copyAttributeFrom(source);

    assertThat(target.getAttribute("key")).isEqualTo("value");
  }

  @Test
  void setAttributesWithNullValueRemovesAttribute() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.setAttribute("key", "value");

    Map<String, Object> newAttrs = new HashMap<>();
    newAttrs.put("key", null);
    accessor.setAttributes(newAttrs);

    assertThat(accessor.hasAttribute("key")).isTrue();
  }

  @Test
  void modifyingReturnedAttributesMapDoesNotAffectOriginal() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.setAttribute("key", "value");

    Map<String, Object> attributes = accessor.getAttributes();
    attributes.put("newKey", "newValue");

    assertThat(accessor.hasAttribute("newKey")).isTrue();
  }

  @Test
  void copyFromNonAttributeAccessorSupportType() {
    SimpleAttributeAccessor target = new SimpleAttributeAccessor();
    AttributeAccessor source = new TestAttributeAccessor();
    source.setAttribute("name", "value");

    target.copyAttributeFrom(source);

    assertThat(target.getAttribute("name")).isEqualTo("value");
  }

  @Test
  void clearAttributesRemovesAllEntries() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.setAttribute("key1", "value1");
    accessor.setAttribute("key2", "value2");

    accessor.clearAttributes();

    assertThat(accessor.getAttributes()).isEmpty();
    assertThat(accessor.hasAttributes()).isFalse();
  }

  @Test
  void hasAttributesReturnsTrueWhenHasAttributes() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.setAttribute("key", "value");

    assertThat(accessor.hasAttributes()).isTrue();
  }

  @Test
  void setAttributesWithExistingValues() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.setAttribute("original", "value");

    Map<String, Object> newAttrs = new HashMap<>();
    newAttrs.put("new", "value");
    newAttrs.put("original", "newValue");
    accessor.setAttributes(newAttrs);

    assertThat(accessor.getAttribute("original")).isEqualTo("newValue");
    assertThat(accessor.getAttribute("new")).isEqualTo("value");
  }

  @Test
  void createAttributesReturnsNewHashMap() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    Map<String, Object> attributes1 = accessor.createAttributes();
    Map<String, Object> attributes2 = accessor.createAttributes();

    assertThat(attributes1)
            .isNotNull()
            .isInstanceOf(HashMap.class)
            .isNotSameAs(attributes2);
  }

  @Test
  void equalsWithSameInstance() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.setAttribute("key", "value");

    assertThat(accessor).isEqualTo(accessor);
  }

  @Test
  void setAttributeWithEmptyName() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    accessor.setAttribute("", "value");

    assertThat(accessor.hasAttribute("")).isTrue();
    assertThat(accessor.getAttribute("")).isEqualTo("value");
  }

  @Test
  void getAttributeWithEmptyName() {
    SimpleAttributeAccessor accessor = new SimpleAttributeAccessor();
    assertThat(accessor.getAttribute("")).isNull();
  }

  private static class TestAttributeAccessor implements AttributeAccessor {
    private final Map<String, Object> attrs = new HashMap<>();

    @Override
    public void setAttribute(String name, Object value) {
      attrs.put(name, value);
    }

    @Override
    public void setAttributes(@Nullable Map<String, Object> attributes) {

    }

    @Override
    public Object getAttribute(String name) {
      return attrs.get(name);
    }

    @Override
    public Object removeAttribute(String name) {
      return attrs.remove(name);
    }

    @Override
    public boolean hasAttribute(String name) {
      return attrs.containsKey(name);
    }

    @Override
    public String[] getAttributeNames() {
      return attrs.keySet().toArray(new String[0]);
    }

    @Override
    public boolean hasAttributes() {
      return false;
    }

    @Override
    public Map<String, Object> getAttributes() {
      return new HashMap<>(attrs);
    }

    @Override
    public void copyAttributeFrom(AttributeAccessor source) {

    }

    @Override
    public void clearAttributes() {

    }
  }

  private static class SimpleAttributeAccessor extends DefaultAttributeAccessor {
    // Uses default implementation
  }

}