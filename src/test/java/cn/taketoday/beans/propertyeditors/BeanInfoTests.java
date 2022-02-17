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

package cn.taketoday.beans.propertyeditors;

import org.junit.jupiter.api.Test;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

import cn.taketoday.beans.BeanWrapper;
import cn.taketoday.beans.BeanWrapperImpl;
import cn.taketoday.beans.FatalBeanException;
import cn.taketoday.lang.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @since 06.03.2006
 */
public class BeanInfoTests {

  @Test
  public void testComplexObject() {
    ValueBean bean = new ValueBean();
    BeanWrapper bw = new BeanWrapperImpl(bean);
    Integer value = 1;

    bw.setPropertyValue("value", value);
    assertThat(value).as("value not set correctly").isEqualTo(bean.getValue());

    value = 2;
    bw.setPropertyValue("value", value.toString());
    assertThat(value).as("value not converted").isEqualTo(bean.getValue());

    bw.setPropertyValue("value", null);
    assertThat(bean.getValue()).as("value not null").isNull();

    bw.setPropertyValue("value", "");
    assertThat(bean.getValue()).as("value not converted to null").isNull();
  }

  public static class ValueBean {

    private Integer value;

    public Integer getValue() {
      return value;
    }

    public void setValue(Integer value) {
      this.value = value;
    }
  }

  public static class ValueBeanBeanInfo extends SimpleBeanInfo {

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
      try {
        PropertyDescriptor pd = new PropertyDescriptor("value", ValueBean.class);
        pd.setPropertyEditorClass(MyNumberEditor.class);
        return new PropertyDescriptor[] { pd };
      }
      catch (IntrospectionException ex) {
        throw new FatalBeanException("Couldn't create PropertyDescriptor", ex);
      }
    }
  }

  public static class MyNumberEditor extends CustomNumberEditor {

    private Object target;

    public MyNumberEditor() throws IllegalArgumentException {
      super(Integer.class, true);
    }

    public MyNumberEditor(Object target) throws IllegalArgumentException {
      super(Integer.class, true);
      this.target = target;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
      Assert.isTrue(this.target instanceof ValueBean, "Target must be available");
      super.setAsText(text);
    }

  }

}
