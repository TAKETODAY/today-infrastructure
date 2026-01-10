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

package infra.validation;

import org.junit.jupiter.api.Test;

import java.beans.PropertyEditorSupport;
import java.util.Map;

import infra.beans.NotWritablePropertyException;
import infra.beans.NullValueInNestedPathException;
import infra.beans.PropertyValue;
import infra.beans.PropertyValues;
import infra.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 07.03.2006
 */
class DataBinderFieldAccessTests {

  @Test
  void bindingNoErrors() throws Exception {
    FieldAccessBean rod = new FieldAccessBean();
    DataBinder binder = new DataBinder(rod, "person");
    assertThat(binder.isIgnoreUnknownFields()).isTrue();
    binder.initDirectFieldAccess();
    PropertyValues pvs = new PropertyValues();
    pvs.add(new PropertyValue("name", "Rod"));
    pvs.add(new PropertyValue("age", 32));
    pvs.add(new PropertyValue("nonExisting", "someValue"));

    binder.bind(pvs);
    binder.close();

    assertThat(rod.getName().equals("Rod")).as("changed name correctly").isTrue();
    assertThat(rod.getAge() == 32).as("changed age correctly").isTrue();

    Map<?, ?> m = binder.getBindingResult().getModel();
    assertThat(m.size() == 2).as("There is one element in map").isTrue();
    FieldAccessBean tb = (FieldAccessBean) m.get("person");
    assertThat(tb.equals(rod)).as("Same object").isTrue();
  }

  @Test
  void bindingNoErrorsNotIgnoreUnknown() throws Exception {
    FieldAccessBean rod = new FieldAccessBean();
    DataBinder binder = new DataBinder(rod, "person");
    binder.initDirectFieldAccess();
    binder.setIgnoreUnknownFields(false);
    PropertyValues pvs = new PropertyValues();
    pvs.add(new PropertyValue("name", "Rod"));
    pvs.add(new PropertyValue("age", 32));
    pvs.add(new PropertyValue("nonExisting", "someValue"));
    assertThatExceptionOfType(NotWritablePropertyException.class).isThrownBy(() ->
            binder.bind(pvs));
  }

  @Test
  void bindingWithErrors() throws Exception {
    FieldAccessBean rod = new FieldAccessBean();
    DataBinder binder = new DataBinder(rod, "person");
    binder.initDirectFieldAccess();
    PropertyValues pvs = new PropertyValues();
    pvs.add(new PropertyValue("name", "Rod"));
    pvs.add(new PropertyValue("age", "32x"));
    binder.bind(pvs);
    assertThatExceptionOfType(BindException.class).isThrownBy(
                    binder::close)
            .satisfies(ex -> {
              assertThat(rod.getName()).isEqualTo("Rod");
              Map<?, ?> map = binder.getBindingResult().getModel();
              FieldAccessBean tb = (FieldAccessBean) map.get("person");
              assertThat(tb).isEqualTo(rod);

              BindingResult br = (BindingResult) map.get(BindingResult.MODEL_KEY_PREFIX + "person");
              assertThat(br).isSameAs(binder.getBindingResult());
              assertThat(br.hasErrors()).isTrue();
              assertThat(br.getErrorCount()).isEqualTo(1);
              assertThat(br.hasFieldErrors()).isTrue();
              assertThat(br.getFieldErrorCount("age")).isEqualTo(1);
              assertThat(binder.getBindingResult().getFieldValue("age")).isEqualTo("32x");
              assertThat(binder.getBindingResult().getFieldError("age").getRejectedValue()).isEqualTo("32x");
              assertThat(tb.getAge()).isEqualTo(0);
            });
  }

  @Test
  void nestedBindingWithDefaultConversionNoErrors() throws Exception {
    FieldAccessBean rod = new FieldAccessBean();
    DataBinder binder = new DataBinder(rod, "person");
    assertThat(binder.isIgnoreUnknownFields()).isTrue();
    binder.initDirectFieldAccess();
    PropertyValues pvs = new PropertyValues();
    pvs.add(new PropertyValue("spouse.name", "Kerry"));
    pvs.add(new PropertyValue("spouse.jedi", "on"));

    binder.bind(pvs);
    binder.close();

    assertThat(rod.getSpouse().getName()).isEqualTo("Kerry");
    assertThat((rod.getSpouse()).isJedi()).isTrue();
  }

  @Test
  void nestedBindingWithDisabledAutoGrow() throws Exception {
    FieldAccessBean rod = new FieldAccessBean();
    DataBinder binder = new DataBinder(rod, "person");
    binder.setAutoGrowNestedPaths(false);
    binder.initDirectFieldAccess();
    PropertyValues pvs = new PropertyValues();
    pvs.add(new PropertyValue("spouse.name", "Kerry"));

    assertThatExceptionOfType(NullValueInNestedPathException.class).isThrownBy(() ->
            binder.bind(pvs));
  }

  @Test
  void bindingWithErrorsAndCustomEditors() throws Exception {
    FieldAccessBean rod = new FieldAccessBean();
    DataBinder binder = new DataBinder(rod, "person");
    binder.initDirectFieldAccess();
    binder.registerCustomEditor(TestBean.class, "spouse", new PropertyEditorSupport() {
      @Override
      public void setAsText(String text) throws IllegalArgumentException {
        setValue(new TestBean(text, 0));
      }

      @Override
      public String getAsText() {
        return ((TestBean) getValue()).getName();
      }
    });
    PropertyValues pvs = new PropertyValues();
    pvs.add(new PropertyValue("name", "Rod"));
    pvs.add(new PropertyValue("age", "32x"));
    pvs.add(new PropertyValue("spouse", "Kerry"));
    binder.bind(pvs);

    assertThatExceptionOfType(BindException.class).isThrownBy(
                    binder::close)
            .satisfies(ex -> {
              assertThat(rod.getName()).isEqualTo("Rod");
              Map<?, ?> model = binder.getBindingResult().getModel();
              FieldAccessBean tb = (FieldAccessBean) model.get("person");
              assertThat(tb).isEqualTo(rod);
              BindingResult br = (BindingResult) model.get(BindingResult.MODEL_KEY_PREFIX + "person");
              assertThat(br).isSameAs(binder.getBindingResult());
              assertThat(br.hasErrors()).isTrue();
              assertThat(br.getErrorCount()).isEqualTo(1);
              assertThat(br.hasFieldErrors("age")).isTrue();
              assertThat(br.getFieldErrorCount("age")).isEqualTo(1);
              assertThat(binder.getBindingResult().getFieldValue("age")).isEqualTo("32x");
              assertThat(binder.getBindingResult().getFieldError("age").getRejectedValue()).isEqualTo("32x");
              assertThat(tb.getAge()).isEqualTo(0);
              assertThat(br.hasFieldErrors("spouse")).isFalse();
              assertThat(binder.getBindingResult().getFieldValue("spouse")).isEqualTo("Kerry");
              assertThat(tb.getSpouse()).isNotNull();
            });
  }
}
