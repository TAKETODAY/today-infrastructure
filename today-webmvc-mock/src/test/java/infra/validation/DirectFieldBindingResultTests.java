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

  @Test
  void addErrorAddsObjectError() {
    DirectFieldBindingResult result = new DirectFieldBindingResult(new TestBean(), "testBean");
    ObjectError error = new ObjectError("testBean", "message");

    result.addError(error);

    assertThat(result.getAllErrors()).containsExactly(error);
  }

  @Test
  void addErrorAddsFieldError() {
    DirectFieldBindingResult result = new DirectFieldBindingResult(new TestBean(), "testBean");
    FieldError error = new FieldError("testBean", "field", "message");

    result.addError(error);

    assertThat(result.getFieldErrors()).containsExactly(error);
  }

  @Test
  void recordSuppressedFieldAddsToSuppressedFields() {
    DirectFieldBindingResult result = new DirectFieldBindingResult(new TestBean(), "testBean");
    result.recordSuppressedField("field1");
    result.recordSuppressedField("field2");

    assertThat(result.getSuppressedFields()).containsExactly("field1", "field2");
  }

  @Test
  void getSuppressedFieldsReturnsCopyOfList() {
    DirectFieldBindingResult result = new DirectFieldBindingResult(new TestBean(), "testBean");
    result.recordSuppressedField("field1");

    String[] fields1 = result.getSuppressedFields();
    String[] fields2 = result.getSuppressedFields();

    assertThat(fields1).isNotSameAs(fields2);
    assertThat(fields1).containsExactly("field1");
  }

  @Test
  void resolveMessageCodesForNullFieldReturnsGeneralCodes() {
    DirectFieldBindingResult result = new DirectFieldBindingResult(new TestBean(), "testBean");

    String[] codes = result.resolveMessageCodes("error.code", null);

    assertThat(codes).containsExactly("error.code.testBean", "error.code");
  }

  private static class TestBean {
    private String field;
  }

}