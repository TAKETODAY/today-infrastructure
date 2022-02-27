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

package cn.taketoday.context.expression;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.SimpleTypeConverter;
import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.TypeConverter;
import cn.taketoday.format.support.ApplicationConversionService;

/**
 * Default implementation of the {@link TypeConverter} interface,
 * delegating to a {@link SimpleTypeConverter}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/27 22:47
 */
public class StandardTypeConverter extends TypeConverter implements BeanFactoryAware {
  private SimpleTypeConverter delegate = new SimpleTypeConverter();

  private final ConversionService conversionService;

  public StandardTypeConverter() {
    this(ApplicationConversionService.getSharedInstance());
  }

  public StandardTypeConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
    delegate.setConversionService(conversionService);
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    if (beanFactory instanceof ConfigurableBeanFactory cbf) {
      Object typeConverter = cbf.getTypeConverter();
      if (typeConverter instanceof SimpleTypeConverter) {
        delegate = (SimpleTypeConverter) typeConverter;
        delegate.setConversionService(conversionService);
      }
    }
  }

  @Override
  public Object convertToType(ExpressionContext context, Object obj, Class<?> requiredType) {
    try {
      context.setPropertyResolved(true);
      return delegate.convertIfNecessary(obj, requiredType);
    }
    catch (TypeMismatchException e) {
      context.setPropertyResolved(false);
      return null;
    }
  }

}
