/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.expression.spel.support;

import java.beans.PropertyEditor;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.SimpleTypeConverter;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.expression.TypeConverter;

/**
 * Copied from Infra Integration for purposes of reproducing
 * {@link Spr7538Tests}.
 */
class BeanFactoryTypeConverter implements TypeConverter, BeanFactoryAware {

  private SimpleTypeConverter delegate = new SimpleTypeConverter();

  private static ConversionService defaultConversionService;

  private ConversionService conversionService;

  public BeanFactoryTypeConverter() {
    synchronized(this) {
      if (defaultConversionService == null) {
        defaultConversionService = new DefaultConversionService();
      }
    }
    this.conversionService = defaultConversionService;
  }

  public BeanFactoryTypeConverter(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  public void setConversionService(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    if (beanFactory instanceof ConfigurableBeanFactory) {
      Object typeConverter = ((ConfigurableBeanFactory) beanFactory).getTypeConverter();
      if (typeConverter instanceof SimpleTypeConverter) {
        delegate = (SimpleTypeConverter) typeConverter;
      }
    }
  }

  public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
    if (conversionService.canConvert(sourceType, targetType)) {
      return true;
    }
    if (!String.class.isAssignableFrom(sourceType) && !String.class.isAssignableFrom(targetType)) {
      // PropertyEditor cannot convert non-Strings
      return false;
    }
    if (!String.class.isAssignableFrom(sourceType)) {
      return delegate.findCustomEditor(sourceType, null) != null || delegate.getDefaultEditor(sourceType) != null;
    }
    return delegate.findCustomEditor(targetType, null) != null || delegate.getDefaultEditor(targetType) != null;
  }

  @Override
  public boolean canConvert(TypeDescriptor sourceTypeDescriptor, TypeDescriptor targetTypeDescriptor) {
    if (conversionService.canConvert(sourceTypeDescriptor, targetTypeDescriptor)) {
      return true;
    }
    // TODO: what does this mean? This method is not used in SpEL so probably ignorable?
    Class<?> sourceType = sourceTypeDescriptor.getObjectType();
    Class<?> targetType = targetTypeDescriptor.getObjectType();
    return canConvert(sourceType, targetType);
  }

  @Override
  public Object convertValue(Object value, TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (targetType.getType() == Void.class || targetType.getType() == Void.TYPE) {
      return null;
    }
    if (conversionService.canConvert(sourceType, targetType)) {
      return conversionService.convert(value, sourceType, targetType);
    }
    if (!String.class.isAssignableFrom(sourceType.getType())) {
      PropertyEditor editor = delegate.findCustomEditor(sourceType.getType(), null);
      editor.setValue(value);
      return editor.getAsText();
    }
    return delegate.convertIfNecessary(value, targetType.getType());
  }

}
