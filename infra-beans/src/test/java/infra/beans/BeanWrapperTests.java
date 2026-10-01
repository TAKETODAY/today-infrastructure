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

package infra.beans;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import infra.beans.testfixture.beans.TestBean;
import infra.core.conversion.support.DefaultConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Specific {@link BeanWrapperImpl} tests.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @author Arjen Poutsma
 * @author Chris Beams
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 22:02
 */
class BeanWrapperTests extends AbstractPropertyAccessorTests {

  @Override
  protected BeanWrapperImpl createAccessor(Object target) {
    return new BeanWrapperImpl(target);
  }

  @Test
  void setterDoesNotCallGetter() {
    GetterBean target = new GetterBean();
    BeanWrapper accessor = createAccessor(target);
    accessor.setPropertyValue("name", "tom");
    assertThat(target.getAliasedName()).isEqualTo("tom");
    assertThat(accessor.getPropertyValue("aliasedName")).isEqualTo("tom");
  }

  @Test
  void getterSilentlyFailWithOldValueExtraction() {
    GetterBean target = new GetterBean();
    BeanWrapper accessor = createAccessor(target);
    accessor.setExtractOldValueForEditor(true); // This will call the getter
    accessor.setPropertyValue("name", "tom");
    assertThat(target.getAliasedName()).isEqualTo("tom");
    assertThat(accessor.getPropertyValue("aliasedName")).isEqualTo("tom");
  }

  @Test
  void aliasedSetterThroughDefaultMethod() {
    GetterBean target = new GetterBean();
    BeanWrapper accessor = createAccessor(target);
    accessor.setPropertyValue("aliasedName", "tom");
    assertThat(target.getAliasedName()).isEqualTo("tom");
    assertThat(accessor.getPropertyValue("aliasedName")).isEqualTo("tom");
  }

  @Test
  void setValidAndInvalidPropertyValuesShouldContainExceptionDetails() {
    TestBean target = new TestBean();
    String newName = "tony";
    String invalidTouchy = ".valid";
    BeanWrapper accessor = createAccessor(target);
    PropertyValues pvs = new PropertyValues();
    pvs.add(new PropertyValue("age", "foobar"));
    pvs.add(new PropertyValue("name", newName));
    pvs.add(new PropertyValue("touchy", invalidTouchy));
    assertThatExceptionOfType(PropertyBatchUpdateException.class).isThrownBy(() ->
                    accessor.setPropertyValues(pvs))
            .satisfies(ex -> {
              assertThat(ex.getExceptionCount()).isEqualTo(2);
              assertThat(ex.getPropertyAccessException("touchy").getPropertyChangeEvent()
                      .getNewValue()).isEqualTo(invalidTouchy);
            });
    // Test validly set property matches
    assertThat(target.getName()).as("Valid set property must stick").isEqualTo(newName);
    assertThat(target.getAge()).as("Invalid set property must retain old value").isEqualTo(0);
  }

  @Test
  void checkNotWritablePropertyHoldPossibleMatches() {
    TestBean target = new TestBean();
    BeanWrapper accessor = createAccessor(target);
    assertThatExceptionOfType(NotWritablePropertyException.class).isThrownBy(() ->
                    accessor.setPropertyValue("ag", "foobar"))
            .satisfies(ex -> assertThat(ex.getPossibleMatches()).containsExactly("age"));
  }

  @Test
    // Can't be shared; there is no such thing as a read-only field
  void setReadOnlyMapProperty() {
    TypedReadOnlyMap map = new TypedReadOnlyMap(Collections.singletonMap("key", new TestBean()));
    TypedReadOnlyMapClient target = new TypedReadOnlyMapClient();
    BeanWrapper accessor = createAccessor(target);
    assertThatNoException().isThrownBy(() -> accessor.setPropertyValue("map", map));
  }

  @Test
  void notWritablePropertyExceptionContainsAlternativeMatch() {
    IntelliBean target = new IntelliBean();
    BeanWrapper bw = createAccessor(target);
    try {
      bw.setPropertyValue("names", "Alef");
    }
    catch (NotWritablePropertyException ex) {
      assertThat(ex.getPossibleMatches()).as("Possible matches not determined").isNotNull();
      assertThat(ex.getPossibleMatches()).as("Invalid amount of alternatives").hasSize(1);
    }
  }

  @Test
  void notWritablePropertyExceptionContainsAlternativeMatches() {
    IntelliBean target = new IntelliBean();
    BeanWrapper bw = createAccessor(target);
    try {
      bw.setPropertyValue("mystring", "Arjen");
    }
    catch (NotWritablePropertyException ex) {
      assertThat(ex.getPossibleMatches()).as("Possible matches not determined").isNotNull();
      assertThat(ex.getPossibleMatches()).as("Invalid amount of alternatives").hasSize(3);
    }
  }

  @Override
  @Test
    // Can't be shared: no type mismatch with a field
  void setPropertyTypeMismatch() {
    PropertyTypeMismatch target = new PropertyTypeMismatch();
    BeanWrapper accessor = createAccessor(target);
    accessor.setPropertyValue("object", "a String");
    assertThat(target.value).isEqualTo("a String");
    assertThat(target.getObject()).isEqualTo(8);
    assertThat(accessor.getPropertyValue("object")).isEqualTo(8);
  }

  @Test
  void setterOverload() {
    SetterOverload target = new SetterOverload();
    BeanWrapper accessor = createAccessor(target);

    accessor.setPropertyValue("object", "a String");
    assertThat(target.value).isEqualTo("a String");
    assertThat(target.getObject()).isEqualTo("a String");
    assertThat(accessor.getPropertyValue("object")).isEqualTo("a String");

    accessor.setPropertyValue("object", 1000);
    assertThat(target.value).isEqualTo("1000");
    assertThat(target.getObject()).isEqualTo("1000");
    assertThat(accessor.getPropertyValue("object")).isEqualTo("1000");

    accessor.setPropertyValue("value", 1000);
    assertThat(target.value).isEqualTo("1000i");
    assertThat(target.getObject()).isEqualTo("1000i");
    assertThat(accessor.getPropertyValue("object")).isEqualTo("1000i");

    accessor.setPropertyValue("value", Duration.ofSeconds(1000));
    assertThat(target.value).isEqualTo("1000s");
    assertThat(target.getObject()).isEqualTo("1000s");
    assertThat(accessor.getPropertyValue("object")).isEqualTo("1000s");
  }

  @Test
  void uniqueSetterFallbackConvertsOriginalValue() throws Exception {
    NumericSetterOverload target = new NumericSetterOverload();
    BeanWrapperImpl accessor = numericSetterAccessor(target);
    DefaultConversionService conversions = new DefaultConversionService();
    conversions.addConverter(String.class, Integer.class, value -> {
      throw new IllegalArgumentException("Primary conversion rejected");
    });
    accessor.setConversionService(conversions);

    accessor.setPropertyValue("number", "42");

    assertThat(target.value).isEqualTo(42L);
  }

  @Test
  void failedSetterFallbackPreservesOriginalTypeMismatch() throws Exception {
    NumericSetterOverload target = new NumericSetterOverload();
    BeanWrapperImpl accessor = numericSetterAccessor(target);

    assertThatExceptionOfType(TypeMismatchException.class)
            .isThrownBy(() -> accessor.setPropertyValue("number", "invalid"))
            .satisfies(ex -> {
              assertThat(ex.getRequiredType()).isEqualTo(Integer.class);
              assertThat(ex.getValue()).isEqualTo("invalid");
            });
    assertThat(target.value).isNull();
  }

  private BeanWrapperImpl numericSetterAccessor(NumericSetterOverload target) throws Exception {
    // Select the primary overload explicitly so this test is independent of
    // the JDK introspector's ordering of overloaded setters.
    BeanProperty property = new BeanProperty(new GenericTypeAwarePropertyDescriptor(
            NumericSetterOverload.class, "number", null,
            NumericSetterOverload.class.getMethod("setNumber", Integer.class), null), NumericSetterOverload.class);
    BeanMetadata metadata = mock(BeanMetadata.class);
    given(metadata.getProperty("number")).willReturn(property);
    return new BeanWrapperImpl(target, metadata);
  }

  @Test
  void propertyDescriptors() {
    TestBean target = new TestBean();
    target.setSpouse(new TestBean());
    BeanWrapper accessor = createAccessor(target);
    accessor.setPropertyValue("name", "a");
    accessor.setPropertyValue("spouse.name", "b");
    assertThat(target.getName()).isEqualTo("a");
    assertThat(target.getSpouse().getName()).isEqualTo("b");
    assertThat(accessor.getPropertyValue("name")).isEqualTo("a");
    assertThat(accessor.getPropertyValue("spouse.name")).isEqualTo("b");
    assertThat(accessor.getBeanProperty("name").getType()).isEqualTo(String.class);
    assertThat(accessor.getBeanProperty("spouse.name").getType()).isEqualTo(String.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void getPropertyWithOptional() {
    GetterWithOptional target = new GetterWithOptional();
    TestBean tb = new TestBean("x");
    BeanWrapper accessor = createAccessor(target);

    accessor.setPropertyValue("object", tb);
    assertThat(target.value).isSameAs(tb);
    assertThat(target.getObject().get()).isSameAs(tb);
    assertThat(((Optional<TestBean>) accessor.getPropertyValue("object")).get()).isSameAs(tb);
    assertThat(target.value.getName()).isEqualTo("x");
    assertThat(target.getObject().get().getName()).isEqualTo("x");
    assertThat(accessor.getPropertyValue("object.name")).isEqualTo("x");

    accessor.setPropertyValue("object.name", "y");
    assertThat(target.value).isSameAs(tb);
    assertThat(target.getObject().get()).isSameAs(tb);
    assertThat(((Optional<TestBean>) accessor.getPropertyValue("object")).get()).isSameAs(tb);
    assertThat(target.value.getName()).isEqualTo("y");
    assertThat(target.getObject().get().getName()).isEqualTo("y");
    assertThat(accessor.getPropertyValue("object.name")).isEqualTo("y");
  }

  @Test
  void getPropertyWithOptionalAndAutoGrow() {
    GetterWithOptional target = new GetterWithOptional();
    BeanWrapper accessor = createAccessor(target);
    accessor.setAutoGrowNestedPaths(true);

    accessor.setPropertyValue("object.name", "x");
    assertThat(target.value.getName()).isEqualTo("x");
    assertThat(target.getObject().get().getName()).isEqualTo("x");
    assertThat(accessor.getPropertyValue("object.name")).isEqualTo("x");
  }

  @Test
  void incompletelyQuotedKeyLeadsToPropertyException() {
    TestBean target = new TestBean();
    BeanWrapper accessor = createAccessor(target);
    assertThatExceptionOfType(InvalidPropertyPathException.class).isThrownBy(() ->
                    accessor.setPropertyValue("[']", "foobar"))
            .withMessageContaining("unterminated quote");
  }

  @Test
  void getBeanPropertyForMalformedPathThrowsInvalidPropertyException() {
    BeanWrapper accessor = createAccessor(new TestBean());
    assertThatExceptionOfType(InvalidPropertyException.class)
            .isThrownBy(() -> accessor.getBeanProperty("map[unterminated"))
            .withCauseInstanceOf(InvalidPropertyPathException.class);
  }

  private interface BaseProperty {

    default String getAliasedName() {
      return getName();
    }

    String getName();
  }

  @SuppressWarnings("unused")
  private interface AliasedProperty extends BaseProperty {

    default void setAliasedName(String name) {
      setName(name);
    }

    void setName(String name);
  }

  @SuppressWarnings("unused")
  private static class GetterBean implements AliasedProperty {

    private String name;

    @Override
    public void setName(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      if (this.name == null) {
        throw new RuntimeException("name property must be set");
      }
      return name;
    }
  }

  @SuppressWarnings("unused")
  private static class IntelliBean {

    public void setName(String name) {
    }

    public void setMyString(String string) {
    }

    public void setMyStrings(String string) {
    }

    public void setMyStriNg(String string) {
    }

    public void setMyStringss(String string) {
    }
  }

  @SuppressWarnings("serial")
  public static class TypedReadOnlyMap extends ReadOnlyMap<String, TestBean> {

    public TypedReadOnlyMap() {
    }

    public TypedReadOnlyMap(Map<? extends String, ? extends TestBean> map) {
      super(map);
    }
  }

  public static class TypedReadOnlyMapClient {

    public void setMap(TypedReadOnlyMap map) {
    }
  }

  public static class PropertyTypeMismatch {

    public String value;

    public void setObject(String object) {
      this.value = object;
    }

    public Integer getObject() {
      return (this.value != null ? this.value.length() : null);
    }
  }

  public static class NumericSetterOverload {

    public Long value;

    public void setNumber(Integer value) {
      this.value = value.longValue();
    }

    public void setNumber(Long value) {
      this.value = value;
    }
  }

  public static class SetterOverload {

    public String value;

    public void setObject(Integer length) {
      this.value = length + "i";
    }

    public void setObject(String object) {
      this.value = object;
    }

    public String getObject() {
      return this.value;
    }

    public void setValue(int length) {
      this.value = length + "i";
    }

    public void setValue(Duration duration) {
      this.value = duration.getSeconds() + "s";
    }
  }

  public static class GetterWithOptional {

    public TestBean value;

    public void setObject(TestBean object) {
      this.value = object;
    }

    public Optional<TestBean> getObject() {
      return Optional.ofNullable(this.value);
    }
  }

}
