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

package infra.web.view;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

import infra.beans.BeansException;
import infra.context.ApplicationContext;
import infra.context.support.ApplicationObjectSupport;
import infra.core.Ordered;

/**
 * A simple implementation of {@link ViewResolver}
 * that interprets a view name as a bean name in the current application context,
 * i.e. typically in the XML file of the executing {@code DispatcherHandler}
 * or in a corresponding configuration class.
 *
 * <p>Note: This {@code ViewResolver} implements the {@link Ordered} interface
 * in order to allow for flexible participation in {@code ViewResolver} chaining.
 * For example, some special views could be defined via this {@code ViewResolver}
 * (giving it 0 as "order" value), while all remaining views could be resolved by
 * a {@link UrlBasedViewResolver}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see UrlBasedViewResolver
 * @since 4.0
 */
public class BeanNameViewResolver extends ApplicationObjectSupport implements ViewResolver, Ordered {

  private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

  /**
   * Specify the order value for this ViewResolver bean.
   * <p>The default value is {@code Ordered.LOWEST_PRECEDENCE}, meaning non-ordered.
   *
   * @see Ordered#getOrder()
   */
  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  @Override
  @Nullable
  public View resolveViewName(String viewName, Locale locale) throws BeansException {
    ApplicationContext context = applicationContext();
    if (!context.containsBean(viewName)) {
      // Allow for ViewResolver chaining...
      return null;
    }

    if (context.isTypeMatch(viewName, View.class)) {
      return context.getBean(viewName, View.class);
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Found bean named '{}' but it does not implement View", viewName);
    }
    // Since we're looking into the general ApplicationContext here,
    // let's accept this as a non-match and allow for chaining as well...
    return null;
  }

}
