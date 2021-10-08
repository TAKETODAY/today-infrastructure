/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.env.Environment;

/**
 * for ConditionEvaluator Evaluation
 *
 * @author TODAY 2021/10/1 21:13
 * @since 4.0
 */
public class ConditionEvaluationContext {

  private final Environment environment;
  private final ApplicationContext context;
  private final BeanDefinitionRegistry registry;

  public ConditionEvaluationContext(ApplicationContext context, BeanDefinitionRegistry registry) {
    this.context = context;
    this.registry = registry;
    this.environment = context.getEnvironment();
  }

  public ApplicationContext getContext() {
    return context;
  }

  public Environment getEnvironment() {
    return environment;
  }

  public BeanDefinitionRegistry getRegistry() {
    return registry;
  }

}
