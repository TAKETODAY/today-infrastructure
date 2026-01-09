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
