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

import cn.taketoday.aot.generate.GenerationContext;

/**
 * AOT contribution from a {@link BeanFactoryInitializationAotProcessor} used to
 * initialize a bean factory.
 *
 * <p>Note: Beans implementing this interface will not have registration methods
 * generated during AOT processing unless they also implement
 * {@link cn.taketoday.beans.factory.aot.BeanRegistrationExcludeFilter}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanFactoryInitializationAotProcessor
 * @since 4.0
 */
@FunctionalInterface
public interface BeanFactoryInitializationAotContribution {

  /**
   * Apply this contribution to the given {@link BeanFactoryInitializationCode}.
   *
   * @param generationContext the active generation context
   * @param beanFactoryInitializationCode the bean factory initialization code
   */
  void applyTo(GenerationContext generationContext,
          BeanFactoryInitializationCode beanFactoryInitializationCode);

}
