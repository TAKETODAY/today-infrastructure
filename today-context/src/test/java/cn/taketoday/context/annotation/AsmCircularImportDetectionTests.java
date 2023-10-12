/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.BeforeEach;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.BootstrapContext;

/**
 * Unit test proving that ASM-based {@link ConfigurationClassParser} correctly detects
 * circular use of the {@link Import @Import} annotation.
 *
 * @author Chris Beams
 */
public class AsmCircularImportDetectionTests extends AbstractCircularImportDetectionTests {
  private StandardBeanFactory beanFactory;

  private BootstrapContext bootstrapContext;

  @BeforeEach
  void setup() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    beanFactory = context.getBeanFactory();
    bootstrapContext = new BootstrapContext(beanFactory, context);
  }

  @Override
  protected ConfigurationClassParser newParser() {
    return new ConfigurationClassParser(bootstrapContext);
  }

  @Override
  protected String loadAsConfigurationSource(Class<?> clazz) throws Exception {
    return clazz.getName();
  }

}
