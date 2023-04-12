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

package cn.taketoday.context.support;

import java.util.Set;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.ConverterRegistry;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.core.conversion.support.GenericConversionService;
import cn.taketoday.lang.Nullable;

/**
 * A factory providing convenient access to a ConversionService configured with
 * converters appropriate for most environments. Set the
 * {@link #setConverters "converters"} property to supplement the default converters.
 *
 * <p>This implementation creates a {@link DefaultConversionService}.
 * Subclasses may override {@link #createConversionService()} in order to return
 * a {@link GenericConversionService} instance of their choosing.
 *
 * <p>Like all {@code FactoryBean} implementations, this class is suitable for
 * use when configuring a Infra application context using Infra {@code <beans>}
 * XML. When configuring the container with
 * {@link cn.taketoday.context.annotation.Configuration @Configuration}
 * classes, simply instantiate, configure and return the appropriate
 * {@code ConversionService} object from a {@link
 * cn.taketoday.context.annotation.Bean @Bean} method.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/13 10:53
 */
public class ConversionServiceFactoryBean implements FactoryBean<ConversionService>, InitializingBean {

  @Nullable
  private Set<?> converters;

  @Nullable
  private GenericConversionService conversionService;

  /**
   * Configure the set of custom converter objects that should be added:
   * implementing {@link cn.taketoday.core.conversion.Converter},
   * {@link cn.taketoday.core.conversion.ConverterFactory},
   * or {@link cn.taketoday.core.conversion.GenericConverter}.
   */
  public void setConverters(Set<?> converters) {
    this.converters = converters;
  }

  @Override
  public void afterPropertiesSet() {
    this.conversionService = createConversionService();
    ConverterRegistry.registerConverters(this.converters, this.conversionService);
  }

  /**
   * Create the ConversionService instance returned by this factory bean.
   * <p>Creates a simple {@link GenericConversionService} instance by default.
   * Subclasses may override to customize the ConversionService instance that
   * gets created.
   */
  protected GenericConversionService createConversionService() {
    return new DefaultConversionService();
  }

  // implementing FactoryBean

  @Override
  @Nullable
  public ConversionService getObject() {
    return this.conversionService;
  }

  @Override
  public Class<? extends ConversionService> getObjectType() {
    return GenericConversionService.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
