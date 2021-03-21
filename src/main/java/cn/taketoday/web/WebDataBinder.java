/*
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

package cn.taketoday.web;

import java.util.List;
import java.util.Map;

import cn.taketoday.context.factory.BeanMetadata;
import cn.taketoday.context.factory.DataBinder;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.web.multipart.MultipartFile;

/**
 * @author TODAY 2021/3/21 12:06
 * @since 3.0
 */
public class WebDataBinder extends DataBinder {

  public WebDataBinder() { }

  public WebDataBinder(Class<?> beanClass) {
    super(beanClass);
  }

  public WebDataBinder(Object object) {
    super(BeanMetadata.ofClass(object.getClass()), object);
  }

  public WebDataBinder(BeanMetadata metadata, Object object) {
    super(metadata, object);
  }

  /**
   * @param context
   *         Current request context
   */
  public Object bind(RequestContext context) {
    return bind(getRootObject(), getMetadata(), context);
  }

  public Object bind(Object rootObject, BeanMetadata metadata, RequestContext context) {
    final Map<String, String[]> parameters = context.parameters();
    for (final Map.Entry<String, String[]> entry : parameters.entrySet()) {
      final String[] value = entry.getValue();
      if (ObjectUtils.isNotEmpty(value)) {
        addPropertyValue(entry.getKey(), value[0]);
      }
    }

    final Map<String, List<MultipartFile>> multipartFiles = context.multipartFiles();
    if (!CollectionUtils.isEmpty(multipartFiles)) {
      for (final Map.Entry<String, List<MultipartFile>> entry : multipartFiles.entrySet()) {
        final List<MultipartFile> files = entry.getValue();
        if (files.size() == 1) {
          addPropertyValue(entry.getKey(), files.get(0));
        }
        else {
          addPropertyValue(entry.getKey(), files);
        }
      }
    }
    return bind(rootObject, metadata, getPropertyValues());
  }
}
