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

package infra.beans.factory;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.beans.BeanMetadata;
import infra.beans.BeanProperty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author TODAY 2021/6/1 21:05
 */
public class BeanMetadataTests {

  @Test
  public void beanMetadata() {
    final BeanMetadata beanMetadata = BeanMetadata.forClass(BeanMappingTestBean.class);
    final Object instance = beanMetadata.newInstance();

    assertThat(instance).isInstanceOf(BeanMappingTestBean.class);

    BeanMappingTestBean bean = (BeanMappingTestBean) instance;

    bean.setAnotherNested(bean);

    assertThat(bean.getDoubleProperty()).isEqualTo(321.0);

    beanMetadata.setPropertyValue(instance, "doubleProperty", 123.45);
    assertThat(bean.getDoubleProperty()).isEqualTo(123.45);

    beanMetadata.obtainBeanProperty("doubleProperty").setValue(instance, 321.0);
    assertThat(bean.getDoubleProperty()).isEqualTo(321.0);

    assertThatThrownBy(() -> {
      beanMetadata.obtainBeanProperty("1243");
    }).hasMessageStartingWith(String.format("Invalid property '1243' of bean class [%s]: Property not found", BeanMappingTestBean.class.getName()));

  }

  @Test
  void booleanPropertyDoesNotConflict() {
    final BeanMetadata beanMetadata = BeanMetadata.forClass(BooleanConflictBean.class);
    final Object instance = beanMetadata.newInstance();

    beanMetadata.setPropertyValue(instance, "enabled", true);
    assertThat((Boolean) beanMetadata.getPropertyValue(instance, "enabled")).isTrue();

    // The boolean field is read through its is* getter, and must not surface
    // a second, field-only property under the raw field name.
    assertThat(beanMetadata.containsProperty("enabled")).isTrue();
    assertThat(beanMetadata.containsProperty("isEnabled")).isFalse();
  }

  @Test
  void isBooleanGetterDerivesSamePropertyNameAsField() {
    BeanMetadata beanMetadata = BeanMetadata.forClass(BooleanConflictBean.class);

    BeanProperty property = beanMetadata.getProperty("enabled");
    assertThat(property).isNotNull();
    assertThat(property.getReadMethod()).isNotNull();
    assertThat(property.getReadMethod().getName()).isEqualTo("isEnabled");
    assertThat(property.getWriteMethod()).isNotNull();
    assertThat(property.getWriteMethod().getName()).isEqualTo("setEnabled");
  }

  @Test
  void forClassReturnsCachedInstance() {
    BeanMetadata first = BeanMetadata.forClass(RichBean.class);
    BeanMetadata second = BeanMetadata.forClass(RichBean.class);
    assertThat(first).isSameAs(second);

    BeanMetadata byInstance = BeanMetadata.forInstance(new RichBean());
    assertThat(byInstance).isSameAs(first);
  }

  @Test
  void equalsAndHashCodeAreTypeBased() {
    BeanMetadata a = BeanMetadata.forClass(RichBean.class);
    BeanMetadata b = BeanMetadata.forClass(RichBean.class);
    BeanMetadata other = BeanMetadata.forClass(BooleanConflictBean.class);

    assertThat(a).isEqualTo(b);
    assertThat(a).isNotEqualTo(other);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }

  @Test
  void getTypeReturnsBeanClass() {
    assertThat(BeanMetadata.forClass(RichBean.class).getType()).isEqualTo(RichBean.class);
  }

  @Test
  void collectsMethodBasedProperties() {
    BeanMetadata beanMetadata = BeanMetadata.forClass(RichBean.class);
    assertThat(beanMetadata.containsProperty("text")).isTrue();
    assertThat(beanMetadata.containsProperty("count")).isTrue();
    assertThat(beanMetadata.containsProperty("active")).isTrue();
    assertThat(beanMetadata.containsProperty("items")).isTrue();
  }

  @Test
  void booleanIsPrefixedFieldDoesNotConflictWithGetter() {
    BeanMetadata beanMetadata = BeanMetadata.forClass(RichBean.class);
    assertThat(beanMetadata.containsProperty("ready")).isTrue();
    assertThat(beanMetadata.containsProperty("isReady")).isFalse();
  }

  @Test
  void setterOnlyBooleanFieldUsesSetterPropertyName() {
    BeanMetadata beanMetadata = new BeanMetadata(SetterOnlyBooleanBean.class);

    assertThat(beanMetadata.containsProperty("enabled")).isTrue();
    assertThat(beanMetadata.containsProperty("isEnabled")).isFalse();
    beanMetadata.setPropertyValue(new SetterOnlyBooleanBean(), "enabled", true);
  }

  @Test
  void staticFieldsAreNotCollected() {
    BeanMetadata beanMetadata = BeanMetadata.forClass(RichBean.class);
    assertThat(beanMetadata.containsProperty("STATIC_FIELD")).isFalse();
  }

  @Test
  void readOnlyFinalFieldIsNotWriteable() {
    BeanMetadata beanMetadata = BeanMetadata.forClass(ReadOnlyBean.class);
    BeanProperty property = beanMetadata.getProperty("finalValue");
    assertThat(property).isNotNull();
    assertThat(property.isReadable()).isTrue();
    assertThat(property.isWriteable()).isFalse();
  }

  @Test
  void setReadOnlyPropertyThrows() {
    BeanMetadata beanMetadata = BeanMetadata.forClass(ReadOnlyBean.class);
    ReadOnlyBean bean = new ReadOnlyBean();
    assertThatThrownBy(() -> beanMetadata.setPropertyValue(bean, "finalValue", "x"))
            .isInstanceOf(Exception.class)
            .hasMessageStartingWith(
                    "Invalid property 'finalValue' of bean class [%s]: Bean property 'finalValue' is not writable"
                            .formatted(ReadOnlyBean.class.getName()));
  }

  @Test
  void getPropertyTypeResolvesFieldAndMethodTypes() {
    BeanMetadata beanMetadata = BeanMetadata.forClass(RichBean.class);
    assertThat(beanMetadata.getPropertyType("text")).isEqualTo(String.class);
    assertThat(beanMetadata.getPropertyType("count")).isEqualTo(int.class);
    assertThat(beanMetadata.getPropertyType("active")).isEqualTo(boolean.class);
  }

  @Test
  void propertySizeAndIterationReflectCollectedProperties() {
    BeanMetadata beanMetadata = BeanMetadata.forClass(RichBean.class);
    int size = beanMetadata.getPropertyCount();

    assertThat(size).isPositive();
    assertThat(beanMetadata.getBeanProperties()).hasSize(size);

    int iterated = 0;
    for (BeanProperty ignored : beanMetadata) {
      iterated++;
    }
    assertThat(iterated).isEqualTo(size);
  }

  @Test
  void obtainBeanPropertyThrowsForUnknown() {
    BeanMetadata beanMetadata = BeanMetadata.forClass(RichBean.class);
    assertThatThrownBy(() -> beanMetadata.obtainBeanProperty("missing"))
            .isInstanceOf(Exception.class)
            .hasMessageStartingWith("Invalid property 'missing' of bean class [%s]".formatted(RichBean.class.getName()));
  }

  @Test
  void setAndGetPropertyRoundTrip() {
    BeanMetadata beanMetadata = BeanMetadata.forClass(RichBean.class);
    RichBean bean = new RichBean();

    beanMetadata.setPropertyValue(bean, "text", "hello");
    beanMetadata.setPropertyValue(bean, "count", 42);
    beanMetadata.setPropertyValue(bean, "active", true);

    assertThat(beanMetadata.getPropertyValue(bean, "text")).isEqualTo("hello");
    assertThat(beanMetadata.getPropertyValue(bean, "count")).isEqualTo(42);
    assertThat(beanMetadata.getPropertyValue(bean, "active")).isEqualTo(true);

    assertThat(bean.getText()).isEqualTo("hello");
    assertThat(bean.getCount()).isEqualTo(42);
    assertThat(bean.isActive()).isTrue();
  }

  @Test
  void newInstanceCreatesNewBean() {
    BeanMetadata beanMetadata = BeanMetadata.forClass(RichBean.class);
    Object a = beanMetadata.newInstance();
    Object b = beanMetadata.newInstance();
    assertThat(a).isInstanceOf(RichBean.class);
    assertThat(b).isInstanceOf(RichBean.class);
    assertThat(a).isNotSameAs(b);
  }

  static class BooleanConflictBean {

    private boolean isEnabled;

    public boolean isEnabled() {
      return isEnabled;
    }

    public void setEnabled(boolean enabled) {
      this.isEnabled = enabled;
    }

  }

  static class RichBean {

    private String text;

    private int count;

    private boolean active;

    private boolean isReady;

    private List<String> items;

    private static String STATIC_FIELD = "static";

    public String getText() {
      return text;
    }

    public void setText(String text) {
      this.text = text;
    }

    public int getCount() {
      return count;
    }

    public void setCount(int count) {
      this.count = count;
    }

    public boolean isActive() {
      return active;
    }

    public void setActive(boolean active) {
      this.active = active;
    }

    public boolean isReady() {
      return isReady;
    }

    public void setReady(boolean ready) {
      this.isReady = ready;
    }

    public List<String> getItems() {
      return items;
    }

    public void setItems(List<String> items) {
      this.items = items;
    }

  }

  static class ReadOnlyBean {

    private final String finalValue = "fixed";

    public String getFinalValue() {
      return finalValue;
    }

  }

  static class SetterOnlyBooleanBean {

    private boolean isEnabled;

    public void setEnabled(boolean enabled) {
      this.isEnabled = enabled;
    }

  }
}
