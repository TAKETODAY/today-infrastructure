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

package cn.taketoday.jmx.export.naming;

import org.junit.jupiter.api.Test;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import cn.taketoday.jmx.JmxTestBean;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 */
public class IdentityNamingStrategyTests {

  @Test
  public void naming() throws MalformedObjectNameException {
    JmxTestBean bean = new JmxTestBean();
    IdentityNamingStrategy strategy = new IdentityNamingStrategy();
    ObjectName objectName = strategy.getObjectName(bean, "null");
    assertThat(objectName.getDomain()).as("Domain is incorrect").isEqualTo(bean.getClass().getPackage().getName());
    assertThat(objectName.getKeyProperty(IdentityNamingStrategy.TYPE_KEY)).as("Type property is incorrect").isEqualTo(ClassUtils.getShortName(bean.getClass()));
    assertThat(objectName.getKeyProperty(IdentityNamingStrategy.HASH_CODE_KEY)).as("HashCode property is incorrect").isEqualTo(ObjectUtils.getIdentityHexString(bean));
  }

}
