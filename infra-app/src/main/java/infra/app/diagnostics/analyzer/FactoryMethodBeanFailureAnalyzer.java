/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.app.diagnostics.analyzer;

import org.jspecify.annotations.Nullable;

import infra.app.diagnostics.FailureAnalysis;
import infra.beans.factory.InjectionPoint;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.FactoryMethodBeanException;

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
