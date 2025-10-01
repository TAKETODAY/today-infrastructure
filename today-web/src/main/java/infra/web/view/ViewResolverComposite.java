/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.view;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import infra.beans.BeansException;
import infra.beans.factory.InitializingBean;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.core.Ordered;
import infra.core.OrderedSupport;
import infra.util.CollectionUtils;

/**
 * A {@link ViewResolver} that delegates to others.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class ViewResolverComposite extends OrderedSupport
        implements ViewResolver, Ordered, InitializingBean, ApplicationContextAware {

  protected final ArrayList<ViewResolver> viewResolvers = new ArrayList<>();

  /**
   * Set the list of view viewResolvers to delegate to.
   */
  public void setViewResolvers(List<ViewResolver> viewResolvers) {
    this.viewResolvers.clear();
    if (CollectionUtils.isNotEmpty(viewResolvers)) {
      this.viewResolvers.addAll(viewResolvers);
      this.viewResolvers.trimToSize();
    }
  }

  /**
   * Return the viewResolvers
   */
  public List<ViewResolver> getViewResolvers() {
    return viewResolvers;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    for (ViewResolver viewResolver : this.viewResolvers) {
      if (viewResolver instanceof ApplicationContextAware) {
        ((ApplicationContextAware) viewResolver).setApplicationContext(applicationContext);
      }
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    for (ViewResolver viewResolver : this.viewResolvers) {
      if (viewResolver instanceof InitializingBean) {
        ((InitializingBean) viewResolver).afterPropertiesSet();
      }
    }
  }

  @Override
  @Nullable
  public View resolveViewName(String viewName, Locale locale) throws Exception {
    for (ViewResolver viewResolver : this.viewResolvers) {
      View view = viewResolver.resolveViewName(viewName, locale);
      if (view != null) {
        return view;
      }
    }
    return null;
  }

}
