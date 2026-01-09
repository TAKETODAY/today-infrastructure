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
