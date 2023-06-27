/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.test.web.reactive.server;

import cn.taketoday.framework.test.context.InfraTest;
import cn.taketoday.test.context.ContextConfigurationAttributes;
import cn.taketoday.test.context.ContextCustomizer;
import cn.taketoday.test.context.ContextCustomizerFactory;
import cn.taketoday.test.context.TestContextAnnotationUtils;
import cn.taketoday.util.ClassUtils;

import java.util.List;

/**
 * {@link ContextCustomizerFactory} for {@code WebTestClient}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Anugrah Singhal
 */
class WebTestClientContextCustomizerFactory implements ContextCustomizerFactory {

  private static final boolean webClientPresent;

  static {
    ClassLoader loader = WebTestClientContextCustomizerFactory.class.getClassLoader();
    webClientPresent = ClassUtils.isPresent("cn.taketoday.web.reactive.function.client.WebClient", loader);
  }

  @Override
  public ContextCustomizer createContextCustomizer(Class<?> testClass,
          List<ContextConfigurationAttributes> configAttributes) {
    InfraTest springBootTest = TestContextAnnotationUtils.findMergedAnnotation(testClass,
            InfraTest.class);
    return (springBootTest != null && webClientPresent) ? new WebTestClientContextCustomizer() : null;
  }

}
