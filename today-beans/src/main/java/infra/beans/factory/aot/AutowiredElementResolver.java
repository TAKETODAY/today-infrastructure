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

package infra.beans.factory.aot;

import java.util.Set;

import javax.lang.model.element.Element;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.DependencyDescriptor;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Base class for resolvers that support autowiring related to an
 * {@link Element}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class AutowiredElementResolver {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  protected final void registerDependentBeans(ConfigurableBeanFactory beanFactory,
          String beanName, Set<String> autowiredBeanNames) {

    for (String autowiredBeanName : autowiredBeanNames) {
      if (beanFactory.containsBean(autowiredBeanName)) {
        beanFactory.registerDependentBean(autowiredBeanName, beanName);
      }
      logger.trace("Autowiring by type from bean name {}' to bean named '{}'",
              beanName, autowiredBeanName);
    }
  }

  /**
   * {@link DependencyDescriptor} that supports shortcut bean resolution.
   */
  @SuppressWarnings("serial")
  static class ShortcutDependencyDescriptor extends DependencyDescriptor {

    private final String shortcut;

    public ShortcutDependencyDescriptor(DependencyDescriptor original, String shortcut) {
      super(original);
      this.shortcut = shortcut;
    }

    @Override
    public Object resolveShortcut(BeanFactory beanFactory) {
      return beanFactory.getBean(this.shortcut, getDependencyType());
    }
  }

}
