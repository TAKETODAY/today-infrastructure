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

package cn.taketoday.framework.diagnostics.analyzer;

import cn.taketoday.beans.factory.InjectionPoint;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.FactoryMethodBeanException;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.lang.Nullable;

/**
 * An {@link AbstractInjectionFailureAnalyzer} that performs analysis of failures caused
 * by a {@link FactoryMethodBeanException}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/31 22:54
 */
class FactoryMethodBeanFailureAnalyzer extends AbstractInjectionFailureAnalyzer<FactoryMethodBeanException> {

  @Nullable
  @Override
  protected FailureAnalysis analyze(Throwable rootFailure,
          FactoryMethodBeanException cause, @Nullable String description) {
    BeanDefinition definition = cause.getBeanDefinition();

    InjectionPoint injectionPoint = cause.getInjectionPoint();

    String factory = definition.getFactoryBeanName();
    if (factory == null) {
      factory = definition.getBeanClassName();
    }

    String desc = String.format("""
            Only one bean named '%s' which qualifies as autowire candidate.
            But factory method '%s' in '%s' returns null.
            """, cause.getBeanName(), definition.getFactoryMethodName(), factory);

    String action = String.format("Consider to make %s @Nullable or @Autowired(required = false) in your configuration.",
            getDescription(injectionPoint));
    return new FailureAnalysis(desc, action, cause);
  }

}
