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

package infra.context.support;

import org.jspecify.annotations.Nullable;

import java.util.Set;

import infra.beans.factory.FactoryBean;
import infra.beans.factory.InitializingBean;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.core.conversion.ConversionService;
import infra.core.conversion.Converter;
import infra.core.conversion.ConverterFactory;
import infra.core.conversion.ConverterRegistry;
import infra.core.conversion.GenericConverter;
import infra.core.conversion.support.DefaultConversionService;
import infra.core.conversion.support.GenericConversionService;

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
 * {@link Configuration @Configuration}
 * classes, simply instantiate, configure and return the appropriate
 * {@code ConversionService} object from a {@link
 * Bean @Bean} method.
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
   * implementing {@link Converter},
   * {@link ConverterFactory},
   * or {@link GenericConverter}.
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
