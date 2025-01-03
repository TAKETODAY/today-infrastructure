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

package infra.scripting.config;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.RootBeanDefinition;
import infra.scripting.support.ScriptFactoryPostProcessor;

/**
 * Utilities for use with {@link LangNamespaceHandler}.
 *
 * @author Rob Harrop
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 21:48
 */
public abstract class LangNamespaceUtils {

  /**
   * The unique name under which the internally managed {@link ScriptFactoryPostProcessor} is
   * registered in the {@link BeanDefinitionRegistry}.
   */
  private static final String SCRIPT_FACTORY_POST_PROCESSOR_BEAN_NAME =
          "infra.scripting.config.scriptFactoryPostProcessor";

  /**
   * Register a {@link ScriptFactoryPostProcessor} bean definition in the supplied
   * {@link BeanDefinitionRegistry} if the {@link ScriptFactoryPostProcessor} hasn't
   * already been registered.
   *
   * @param registry the {@link BeanDefinitionRegistry} to register the script processor with
   * @return the {@link ScriptFactoryPostProcessor} bean definition (new or already registered)
   */
  public static BeanDefinition registerScriptFactoryPostProcessorIfNecessary(BeanDefinitionRegistry registry) {
    BeanDefinition beanDefinition;
    if (registry.containsBeanDefinition(SCRIPT_FACTORY_POST_PROCESSOR_BEAN_NAME)) {
      beanDefinition = registry.getBeanDefinition(SCRIPT_FACTORY_POST_PROCESSOR_BEAN_NAME);
    }
    else {
      beanDefinition = new RootBeanDefinition(ScriptFactoryPostProcessor.class);
      beanDefinition.setEnableDependencyInjection(false);
      registry.registerBeanDefinition(SCRIPT_FACTORY_POST_PROCESSOR_BEAN_NAME, beanDefinition);
    }
    return beanDefinition;
  }

}
