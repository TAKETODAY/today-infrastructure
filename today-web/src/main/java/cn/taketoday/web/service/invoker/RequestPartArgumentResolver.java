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

package cn.taketoday.web.service.invoker;

import org.reactivestreams.Publisher;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ReactiveAdapter;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.Resource;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.codec.multipart.Part;
import cn.taketoday.lang.Assert;
import cn.taketoday.web.annotation.RequestPart;

/**
 * {@link HttpServiceArgumentResolver} for {@link RequestPart @RequestPart}
 * annotated arguments.
 *
 * <p>The argument may be:
 * <ul>
 * <li>String -- form field
 * <li>{@link Resource Resource} -- file part
 * <li>Object -- content to be encoded (e.g. to JSON)
 * <li>{@link HttpEntity} -- part content and headers although generally it's
 * easier to add headers through the returned builder
 * <li>{@link Part} -- a part from a server request
 * <li>{@link Publisher} of any of the above
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/3 14:20
 */
public class RequestPartArgumentResolver extends AbstractNamedValueArgumentResolver {

  private final ReactiveAdapterRegistry reactiveAdapterRegistry;

  public RequestPartArgumentResolver(ReactiveAdapterRegistry reactiveAdapterRegistry) {
    this.reactiveAdapterRegistry = reactiveAdapterRegistry;
  }

  @Override
  protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
    RequestPart annot = parameter.getParameterAnnotation(RequestPart.class);
    return (annot == null ? null :
            new NamedValueInfo(annot.name(), annot.required(), null, "request part", true));
  }

  @Override
  protected void addRequestValue(
          String name, Object value, MethodParameter parameter, HttpRequestValues.Builder requestValues) {

    Class<?> type = parameter.getParameterType();
    ReactiveAdapter adapter = this.reactiveAdapterRegistry.getAdapter(type);
    if (adapter != null) {
      Assert.isTrue(!adapter.isNoValue(), "Expected publisher that produces a value");
      Publisher<?> publisher = adapter.toPublisher(value);
      requestValues.addRequestPart(name, publisher, ResolvableType.forMethodParameter(parameter.nested()));
    }
    else {
      requestValues.addRequestPart(name, value);
    }
  }

}