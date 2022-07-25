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

package cn.taketoday.jmx.export;

import org.junit.jupiter.api.Test;

import javax.management.ObjectName;

import cn.taketoday.jmx.AbstractJmxTests;
import cn.taketoday.jmx.IJmxTestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
class PropertyPlaceholderConfigurerTests extends AbstractJmxTests {

  @Override
  protected String getApplicationContextPath() {
    return "cn/taketoday/jmx/export/propertyPlaceholderConfigurer.xml";
  }

  @Test
  void propertiesReplacedInApplicationContext() {
    IJmxTestBean bean = getContext().getBean("testBean", IJmxTestBean.class);

    assertThat(bean.getName()).as("Name").isEqualTo("Rob Harrop");
    assertThat(bean.getAge()).as("Age").isEqualTo(100);
  }

  @Test
  void propertiesCorrectInJmx() throws Exception {
    ObjectName oname = new ObjectName("bean:name=proxyTestBean1");
    Object name = getServer().getAttribute(oname, "Name");
    Integer age = (Integer) getServer().getAttribute(oname, "Age");

    assertThat(name).as("Name is incorrect in JMX").isEqualTo("Rob Harrop");
    assertThat(age.intValue()).as("Age is incorrect in JMX").isEqualTo(100);
  }

}

