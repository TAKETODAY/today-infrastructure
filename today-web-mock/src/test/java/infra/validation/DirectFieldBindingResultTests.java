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

package infra.validation;

import org.junit.jupiter.api.Test;

import java.beans.PropertyEditor;
import java.util.Map;

import infra.beans.ConfigurablePropertyAccessor;
import infra.beans.PropertyEditorRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/28 22:07
 */
class DirectFieldBindingResultTests {

  @Test
  void propertyAccessorCreatedOnFirstAccess() {
    DirectFieldBindingResult result = new DirectFieldBindingResult(new TestBean(), "testBean");
    ConfigurablePropertyAccessor accessor = result.getPropertyAccessor();
    assertThat(accessor).isNotNull();
  }

  @Test
  void propertyAccessorReusesSameInstance() {
    DirectFieldBindingResult result = new DirectFieldBindingResult(new TestBean(), "testBean");
    ConfigurablePropertyAccessor first = result.getPropertyAccessor();
    ConfigurablePropertyAccessor second = result.getPropertyAccessor();
    assertThat(first).isSameAs(second);
  }

  @Test
  void createAccessorFailsForNullTarget() {
    DirectFieldBindingResult result = new DirectFieldBindingResult(null, "testBean");
    assertThatThrownBy(() -> result.getPropertyAccessor())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot access fields on null target instance 'testBean'");
  }

  @Test
  void accessorConfiguredForOldValueExtraction() {
    DirectFieldBindingResult result = new DirectFieldBindingResult(new TestBean(), "testBean");
    ConfigurablePropertyAccessor accessor = result.getPropertyAccessor();
    assertThat(accessor.isExtractOldValueForEditor()).isTrue();
  }

  @Test
  void autoGrowNestedPathsEnabledByDefault() {
    DirectFieldBindingResult result = new DirectFieldBindingResult(new TestBean(), "testBean");
    ConfigurablePropertyAccessor accessor = result.getPropertyAccessor();
    assertThat(accessor.isAutoGrowNestedPaths()).isTrue();
  }

  @Test
  void autoGrowNestedPathsCanBeDisabled() {
    DirectFieldBindingResult result = new DirectFieldBindingResult(new TestBean(), "testBean", false);
    ConfigurablePropertyAccessor accessor = result.getPropertyAccessor();
    assertThat(accessor.isAutoGrowNestedPaths()).isFalse();
  }

  @Test
  void targetObjectIsAccessible() {
    TestBean target = new TestBean();
    DirectFieldBindingResult result = new DirectFieldBindingResult(target, "testBean");
    assertThat(result.getTarget()).isSameAs(target);
  }

  @Test
  void nullTargetIsAllowed() {
    DirectFieldBindingResult result = new DirectFieldBindingResult(null, "testBean");
    assertThat(result.getTarget()).isNull();
  }

  @Test
  void getModelContainsTargetAndBindingResult() {
    TestBean target = new TestBean();
    DirectFieldBindingResult result = new DirectFieldBindingResult(target, "testBean");

    Map<String, Object> model = result.getModel();

    assertThat(model).hasSize(2);
    assertThat(model.get("testBean")).isSameAs(target);
    assertThat(model.get(BindingResult.MODEL_KEY_PREFIX + "testBean")).isSameAs(result);
  }

  @Test
  void getRawFieldValueReturnsFieldValue() {
    TestBean target = new TestBean();
    target.field = "test";
    DirectFieldBindingResult result = new DirectFieldBindingResult(target, "testBean");

    Object value = result.getRawFieldValue("field");

    assertThat(value).isEqualTo("test");
  }

  @Test
  void findEditorDelegatesToPropertyAccessor() {
    TestBean target = new TestBean();
    DirectFieldBindingResult result = new DirectFieldBindingResult(target, "testBean");

    PropertyEditor editor = result.findEditor("field", String.class);

    assertThat(editor).isNull();
  }

  @Test
  void getPropertyEditorRegistryReturnsSameAsPropertyAccessor() {
    DirectFieldBindingResult result = new DirectFieldBindingResult(new TestBean(), "testBean");

    PropertyEditorRegistry registry = result.getPropertyEditorRegistry();

    assertThat(registry).isSameAs(result.getPropertyAccessor());
  }

  @Test
  void resolveMessageCodesResolvesErrorCode() {
    DirectFieldBindingResult result = new DirectFieldBindingResult(new TestBean(), "testBean");

    String[] codes = result.resolveMessageCodes("error.code");

    assertThat(codes).contains("error.code.testBean", "error.code");
  }

  @Test
  void resolveMessageCodesForFieldResolvesErrorCodeAndField() {
    DirectFieldBindingResult result = new DirectFieldBindingResult(new TestBean(), "testBean");

    String[] codes = result.resolveMessageCodes("error.code", "field");

    assertThat(codes).contains(
            "error.code.testBean.field",
            "error.code.field",
            "error.code.java.lang.String",
            "error.code"
    );
  }

  private static class TestBean {
    private String field;
  }

}