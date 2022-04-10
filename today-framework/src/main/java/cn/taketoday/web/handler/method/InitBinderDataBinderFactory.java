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

package cn.taketoday.web.handler.method;

import java.util.Collections;
import java.util.List;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.WebDataBinder;
import cn.taketoday.web.bind.annotation.InitBinder;
import cn.taketoday.web.bind.support.DefaultDataBinderFactory;
import cn.taketoday.web.bind.support.WebBindingInitializer;

/**
 * Adds initialization to a WebDataBinder via {@code @InitBinder} methods.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 23:09
 */
public class InitBinderDataBinderFactory extends DefaultDataBinderFactory {

  private final List<InvocableHandlerMethod> binderMethods;

  /**
   * Create a new InitBinderDataBinderFactory instance.
   *
   * @param binderMethods {@code @InitBinder} methods
   * @param initializer for global data binder initialization
   */
  public InitBinderDataBinderFactory(@Nullable List<InvocableHandlerMethod> binderMethods,
          @Nullable WebBindingInitializer initializer) {

    super(initializer);
    this.binderMethods = (binderMethods != null ? binderMethods : Collections.emptyList());
  }

  /**
   * Initialize a WebDataBinder with {@code @InitBinder} methods.
   * <p>If the {@code @InitBinder} annotation specifies attributes names,
   * it is invoked only if the names include the target object name.
   *
   * @throws Exception if one of the invoked @{@link InitBinder} methods fails
   * @see #isBinderMethodApplicable
   */
  @Override
  public void initBinder(WebDataBinder dataBinder, RequestContext request) throws Throwable {
    for (InvocableHandlerMethod binderMethod : binderMethods) {
      if (isBinderMethodApplicable(binderMethod, dataBinder)) {
        Object returnValue = binderMethod.invokeForRequest(request, dataBinder);
        if (returnValue != null) {
          throw new IllegalStateException(
                  "@InitBinder methods must not return a value (should be void): " + binderMethod);
        }
      }
    }
  }

  /**
   * Determine whether the given {@code @InitBinder} method should be used
   * to initialize the given {@link WebDataBinder} instance. By default we
   * check the specified attribute names in the annotation value, if any.
   */
  protected boolean isBinderMethodApplicable(HandlerMethod initBinderMethod, WebDataBinder dataBinder) {
    InitBinder ann = initBinderMethod.getMethodAnnotation(InitBinder.class);
    Assert.state(ann != null, "No InitBinder annotation");
    String[] names = ann.value();
    return ObjectUtils.isEmpty(names) || ObjectUtils.containsElement(names, dataBinder.getObjectName());
  }

}

