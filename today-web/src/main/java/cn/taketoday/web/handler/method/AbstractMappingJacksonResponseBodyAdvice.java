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

import cn.taketoday.core.MethodParameter;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.json.AbstractJackson2HttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJacksonValue;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * A convenient base class for {@code ResponseBodyAdvice} implementations
 * that customize the response before JSON serialization with
 * {@link AbstractJackson2HttpMessageConverter}'s concrete subclasses.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/11 20:59
 */
public abstract class AbstractMappingJacksonResponseBodyAdvice implements ResponseBodyAdvice<Object> {

  @Override
  public boolean supports(@Nullable Object body,
          @Nullable MethodParameter returnType, HttpMessageConverter<?> converter) {
    return converter instanceof AbstractJackson2HttpMessageConverter;
  }

  @Nullable
  @Override
  public Object beforeBodyWrite(
          @Nullable Object body, @Nullable MethodParameter returnType, MediaType contentType,
          HttpMessageConverter<?> converter, RequestContext context) {

    if (body == null) {
      return null;
    }
    MappingJacksonValue container = getOrCreateContainer(body);
    beforeBodyWriteInternal(container, contentType, returnType, context);
    return container;
  }

  /**
   * Wrap the body in a {@link MappingJacksonValue} value container (for providing
   * additional serialization instructions) or simply cast it if already wrapped.
   */
  protected MappingJacksonValue getOrCreateContainer(Object body) {
    return body instanceof MappingJacksonValue ? (MappingJacksonValue) body : new MappingJacksonValue(body);
  }

  /**
   * Invoked only if the converter type is {@code MappingJackson2HttpMessageConverter}.
   */
  protected abstract void beforeBodyWriteInternal(MappingJacksonValue bodyContainer,
          MediaType contentType, @Nullable MethodParameter returnType, RequestContext request);

}
