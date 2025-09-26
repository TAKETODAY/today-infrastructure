/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.context.web.socket;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.beans.BeanUtils;
import infra.core.annotation.AnnotatedElementUtils;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.ContextCustomizer;
import infra.test.context.ContextCustomizerFactory;
import infra.util.ClassUtils;

/**
 * {@link ContextCustomizerFactory} which creates a {@link MockServerContainerContextCustomizer}
 * if WebSocket support is present in the classpath and the test class is annotated
 * with {@code @WebAppConfiguration}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class MockServerContainerContextCustomizerFactory implements ContextCustomizerFactory {

  private static final String WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME =
          "infra.test.context.web.WebAppConfiguration";

  private static final String MOCK_SERVER_CONTAINER_CONTEXT_CUSTOMIZER_CLASS_NAME =
          "infra.test.context.web.socket.MockServerContainerContextCustomizer";

  private static final boolean webSocketPresent = ClassUtils.isPresent("jakarta.websocket.server.ServerContainer",
          MockServerContainerContextCustomizerFactory.class.getClassLoader());

  @Override
  @Nullable
  public ContextCustomizer createContextCustomizer(Class<?> testClass,
          List<ContextConfigurationAttributes> configAttributes) {

    if (webSocketPresent && isAnnotatedWithWebAppConfiguration(testClass)) {
      try {
        Class<?> clazz = ClassUtils.forName(MOCK_SERVER_CONTAINER_CONTEXT_CUSTOMIZER_CLASS_NAME,
                getClass().getClassLoader());
        return (ContextCustomizer) BeanUtils.newInstance(clazz);
      }
      catch (Throwable ex) {
        throw new IllegalStateException("Failed to enable WebSocket test support; could not load class: " +
                MOCK_SERVER_CONTAINER_CONTEXT_CUSTOMIZER_CLASS_NAME, ex);
      }
    }

    // Else, nothing to customize
    return null;
  }

  private static boolean isAnnotatedWithWebAppConfiguration(Class<?> testClass) {
    return (AnnotatedElementUtils.findMergedAnnotationAttributes(testClass,
            WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME, false, false) != null);
  }

}
