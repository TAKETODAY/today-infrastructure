/**
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
package cn.taketoday.web.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextSupport;
import cn.taketoday.web.ui.RedirectModelManager;
import cn.taketoday.web.view.template.TemplateViewResolver;

/**
 * @author TODAY 2019-12-28 13:47
 */
@MissingBean
public class ResultHandlers extends WebApplicationContextSupport {
  private final LinkedList<ResultHandler> handlers = new LinkedList<>();

  public void addHandlers(ResultHandler... handlers) {
    Assert.notNull(handlers, "handler must not be null");
    Collections.addAll(this.handlers, handlers);
    OrderUtils.reversedSort(this.handlers);
  }

  public void addHandlers(List<ResultHandler> handlers) {
    Assert.notNull(handlers, "handler must not be null");
    this.handlers.addAll(handlers);
    OrderUtils.reversedSort(this.handlers);
  }

  public void setHandlers(List<ResultHandler> handlers) {
    Assert.notNull(handlers, "handler must not be null");
    this.handlers.clear();
    this.handlers.addAll(handlers);
    OrderUtils.reversedSort(this.handlers);
  }

  public List<ResultHandler> getHandlers() {
    return handlers;
  }

  public RuntimeResultHandler[] getRuntimeHandlers() {
    final ArrayList<RuntimeResultHandler> ret = new ArrayList<>();
    for (final ResultHandler handler : handlers) {
      if (handler instanceof RuntimeResultHandler) {
        ret.add((RuntimeResultHandler) handler);
      }
    }
    return ret.toArray(new RuntimeResultHandler[ret.size()]);
  }

  public ResultHandler getHandler(final Object handler) {
    Assert.notNull(handler, "handler must not be null");
    for (final ResultHandler resolver : getHandlers()) {
      if (resolver.supportsHandler(handler)) {
        return resolver;
      }
    }
    return null;
  }

  /**
   * Get correspond view resolver, If there isn't a suitable resolver will be
   * throw {@link IllegalArgumentException}
   *
   * @return A suitable {@link ResultHandler}
   */
  public ResultHandler obtainHandler(final Object handler) {
    final ResultHandler resultHandler = getHandler(handler);
    Assert.state(resultHandler != null, () -> "There isn't have a result resolver to resolve : [" + handler + "]");
    return resultHandler;
  }

  //

  /**
   * register default {@link ResultHandler}s
   *
   * @since 3.0
   */
  public void registerDefaultResultHandlers(TemplateViewResolver viewResolver) {
    final List<ResultHandler> handlers = getHandlers();
    final WebApplicationContext context = obtainApplicationContext();

    final Environment environment = context.getEnvironment();
    int bufferSize = Integer.parseInt(environment.getProperty(Constant.DOWNLOAD_BUFF_SIZE, "10240"));

    final MessageConverter messageConverter = context.getBean(MessageConverter.class);
    Assert.state(messageConverter != null, "No MessageConverter in this web application");

    final RedirectModelManager modelManager = context.getBean(RedirectModelManager.class);

    VoidResultHandler voidResultHandler
            = new VoidResultHandler(viewResolver, messageConverter, bufferSize);
    ObjectResultHandler objectResultHandler
            = new ObjectResultHandler(viewResolver, messageConverter, bufferSize);
    ModelAndViewResultHandler modelAndViewResultHandler
            = new ModelAndViewResultHandler(viewResolver, messageConverter, bufferSize);
    ResponseEntityResultHandler responseEntityResultHandler
            = new ResponseEntityResultHandler(viewResolver, messageConverter, bufferSize);
    TemplateResultHandler templateResultHandler = new TemplateResultHandler(viewResolver);

    if (modelManager != null) {
      voidResultHandler.setModelManager(modelManager);
      objectResultHandler.setModelManager(modelManager);
      templateResultHandler.setModelManager(modelManager);
      modelAndViewResultHandler.setModelManager(modelManager);
      responseEntityResultHandler.setModelManager(modelManager);
    }

    handlers.add(new ImageResultHandler());
    handlers.add(new ResourceResultHandler(bufferSize));
    handlers.add(templateResultHandler);

    handlers.add(voidResultHandler);
    handlers.add(objectResultHandler);
    handlers.add(modelAndViewResultHandler);
    handlers.add(responseEntityResultHandler);

    handlers.add(new ResponseBodyResultHandler(messageConverter));
    handlers.add(new HttpStatusResultHandler());
  }

}
