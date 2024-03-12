/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.beans.factory.aot;

import java.util.Set;

import javax.lang.model.element.Element;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

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
