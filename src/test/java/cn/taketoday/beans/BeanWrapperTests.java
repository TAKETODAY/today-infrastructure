/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.beans;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import cn.taketoday.beans.factory.support.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

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
    assertThatExceptionOfType(NotWritablePropertyException.class).isThrownBy(() ->
                    accessor.setPropertyValue("[']", "foobar"))
            .satisfies(ex -> assertThat(ex.getPossibleMatches()).isNull());
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
