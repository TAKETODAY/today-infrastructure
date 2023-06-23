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

package cn.taketoday.beans.factory.aot;

import java.util.Set;

import javax.lang.model.element.Element;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.logging.LogMessage;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Base class for resolvers that support autowiring related to an
 * {@link Element}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
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
      logger.trace(LogMessage.format(
              "Autowiring by type from bean name %s' to bean named '%s'", beanName,
              autowiredBeanName));
    }
  }

  /**
   * {@link DependencyDescriptor} that supports shortcut bean resolution.
   */
  @SuppressWarnings("serial")
  static class ShortcutDependencyDescriptor extends DependencyDescriptor {

    private final String shortcut;

    private final Class<?> requiredType;

    public ShortcutDependencyDescriptor(DependencyDescriptor original,
            String shortcut, Class<?> requiredType) {
      super(original);
      this.shortcut = shortcut;
      this.requiredType = requiredType;
    }

    @Override
    public Object resolveShortcut(BeanFactory beanFactory) {
      return beanFactory.getBean(this.shortcut, this.requiredType);
    }
  }

}
