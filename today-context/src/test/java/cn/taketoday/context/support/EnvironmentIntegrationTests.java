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

package cn.taketoday.context.support;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests covering the integration of the {@link Environment} into
 * {@link ApplicationContext} hierarchies.
 *
 * @author Chris Beams
 * @see cn.taketoday.core.env.EnvironmentSystemIntegrationTests
 */
public class EnvironmentIntegrationTests {

  @Test
  public void repro() {
    ConfigurableApplicationContext parent = new GenericApplicationContext();
    parent.refresh();

    AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
    child.setParent(parent);
    child.refresh();

    ConfigurableEnvironment env = child.getBean(ConfigurableEnvironment.class);
    assertThat(env).isSameAs(child.getEnvironment());

    child.close();
    parent.close();
  }

}
