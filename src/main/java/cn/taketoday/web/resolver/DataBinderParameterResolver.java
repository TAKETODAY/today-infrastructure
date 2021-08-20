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
package cn.taketoday.web.resolver;

import java.util.List;
import java.util.Map;

import cn.taketoday.beans.DataBinder;
import cn.taketoday.core.Assert;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.core.utils.ClassUtils;
import cn.taketoday.core.utils.CollectionUtils;
import cn.taketoday.core.utils.ObjectUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.WebUtils;

/**
 * Resolve Bean
 *
 * @author TODAY <br>
 * 2019-07-13 01:11
 */
public class DataBinderParameterResolver
        extends OrderedSupport implements ParameterResolver {

  private ConversionService conversionService = DefaultConversionService.getSharedInstance();

  public DataBinderParameterResolver() {
    this(LOWEST_PRECEDENCE - HIGHEST_PRECEDENCE - 200);
  }

  public DataBinderParameterResolver(final int order) {
    super(order);
  }

  public DataBinderParameterResolver(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public boolean supports(MethodParameter parameter) {
    return !parameter.isAnnotationPresent(RequestBody.class) // @since 3.0.3 #17
            && !ClassUtils.isSimpleType(parameter.getParameterClass());
  }

  /**
   * @return Pojo parameter
   */
  @Override
  public Object resolveParameter(final RequestContext context, final MethodParameter parameter) {
    final Class<?> parameterClass = parameter.getParameterClass();
    final DataBinder dataBinder = new DataBinder(parameterClass, conversionService);

    final Map<String, String[]> parameters = context.getParameters();
    for (final Map.Entry<String, String[]> entry : parameters.entrySet()) {
      final String[] value = entry.getValue();
      if (ObjectUtils.isNotEmpty(value)) {
        if (value.length == 1) {
          dataBinder.addPropertyValue(entry.getKey(), value[0]);
        }
        else {
          dataBinder.addPropertyValue(entry.getKey(), value);
        }
      }
    }

    if (WebUtils.isMultipart(context)) {
      // Multipart
      final MultiValueMap<String, MultipartFile> multipartFiles = context.multipartFiles();
      if (!CollectionUtils.isEmpty(multipartFiles)) {
        for (final Map.Entry<String, List<MultipartFile>> entry : multipartFiles.entrySet()) {
          final List<MultipartFile> files = entry.getValue();
          if (files.size() == 1) {
            dataBinder.addPropertyValue(entry.getKey(), files.get(0));
          }
          else {
            dataBinder.addPropertyValue(entry.getKey(), files);
          }
        }
      }
    }

    return dataBinder.bind();
  }

  public void setConversionService(ConversionService conversionService) {
    Assert.notNull(conversionService, "conversionService must not be null");
    this.conversionService = conversionService;
  }

  public ConversionService getConversionService() {
    return conversionService;
  }
}
