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

package infra.context.annotation;

import org.junit.jupiter.api.BeforeEach;

import infra.beans.factory.support.StandardBeanFactory;
import infra.context.BootstrapContext;

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
