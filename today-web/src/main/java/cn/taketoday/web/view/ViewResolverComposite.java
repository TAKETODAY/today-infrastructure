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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

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
