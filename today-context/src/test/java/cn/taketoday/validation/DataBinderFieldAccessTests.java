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

package cn.taketoday.validation;

import org.junit.jupiter.api.Test;

import java.beans.PropertyEditorSupport;
import java.util.Map;

import cn.taketoday.beans.NotWritablePropertyException;
import cn.taketoday.beans.NullValueInNestedPathException;
import cn.taketoday.beans.PropertyValue;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.testfixture.beans.TestBean;

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
