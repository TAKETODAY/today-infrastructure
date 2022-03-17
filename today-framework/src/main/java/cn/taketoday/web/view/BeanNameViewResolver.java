/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.view;

import java.util.Locale;

import cn.taketoday.beans.BeansException;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.WebApplicationContextSupport;

/**
 * A simple implementation of {@link ViewResolver}
 * that interprets a view name as a bean name in the current application context,
 * i.e. typically in the XML file of the executing {@code DispatcherServlet}
 * or in a corresponding configuration class.
 *
 * <p>Note: This {@code ViewResolver} implements the {@link Ordered} interface
 * in order to allow for flexible participation in {@code ViewResolver} chaining.
 * For example, some special views could be defined via this {@code ViewResolver}
 * (giving it 0 as "order" value), while all remaining views could be resolved by
 * a {@link UrlBasedViewResolver}.
 *
 * @author Juergen Hoeller
 * @see UrlBasedViewResolver
 * @since 4.0
 */
public class BeanNameViewResolver extends WebApplicationContextSupport implements ViewResolver, Ordered {

  private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

  /**
   * Specify the order value for this ViewResolver bean.
   * <p>The default value is {@code Ordered.LOWEST_PRECEDENCE}, meaning non-ordered.
   *
   * @see cn.taketoday.core.Ordered#getOrder()
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
    ApplicationContext context = obtainApplicationContext();
    if (!context.containsBean(viewName)) {
      // Allow for ViewResolver chaining...
      return null;
    }
    if (!context.isTypeMatch(viewName, View.class)) {
      if (log.isDebugEnabled()) {
        log.debug("Found bean named '{}' but it does not implement View", viewName);
      }
      // Since we're looking into the general ApplicationContext here,
      // let's accept this as a non-match and allow for chaining as well...
      return null;
    }
    return context.getBean(viewName, View.class);
  }

}
