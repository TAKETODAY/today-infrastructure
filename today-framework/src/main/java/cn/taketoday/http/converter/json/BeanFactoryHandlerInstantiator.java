/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.http.converter.json;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.Converter;

import cn.taketoday.beans.factory.config.AutowireCapableBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Assert;

/**
 * Allows for creating Jackson ({@link JsonSerializer}, {@link JsonDeserializer},
 * {@link KeyDeserializer}, {@link TypeResolverBuilder}, {@link TypeIdResolver})
 * beans with autowiring against a {@link ApplicationContext}.
 *
 * <p>This overrides all factory methods in {@link HandlerInstantiator},
 * including non-abstract ones and recently introduced ones from Jackson 2.4 and 2.5:
 * for {@link ValueInstantiator}, {@link ObjectIdGenerator}, {@link ObjectIdResolver},
 * {@link PropertyNamingStrategy}, {@link Converter}, {@link VirtualBeanPropertyWriter}.
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @see Jackson2ObjectMapperBuilder#handlerInstantiator(HandlerInstantiator)
 * @see ApplicationContext#getAutowireCapableBeanFactory()
 * @see HandlerInstantiator
 * @since 4.0
 */
public class BeanFactoryHandlerInstantiator extends HandlerInstantiator {

  private final AutowireCapableBeanFactory beanFactory;

  /**
   * Create a new HandlerInstantiator for the given BeanFactory.
   *
   * @param beanFactory the target BeanFactory
   */
  public BeanFactoryHandlerInstantiator(AutowireCapableBeanFactory beanFactory) {
    Assert.notNull(beanFactory, "BeanFactory must not be null");
    this.beanFactory = beanFactory;
  }

  @Override
  public JsonDeserializer<?> deserializerInstance(
          DeserializationConfig config, Annotated annotated, Class<?> implClass) {

    return (JsonDeserializer<?>) this.beanFactory.createBean(implClass);
  }

  @Override
  public KeyDeserializer keyDeserializerInstance(
          DeserializationConfig config, Annotated annotated, Class<?> implClass) {

    return (KeyDeserializer) this.beanFactory.createBean(implClass);
  }

  @Override
  public JsonSerializer<?> serializerInstance(
          SerializationConfig config, Annotated annotated, Class<?> implClass) {

    return (JsonSerializer<?>) this.beanFactory.createBean(implClass);
  }

  @Override
  public TypeResolverBuilder<?> typeResolverBuilderInstance(
          MapperConfig<?> config, Annotated annotated, Class<?> implClass) {

    return (TypeResolverBuilder<?>) this.beanFactory.createBean(implClass);
  }

  @Override
  public TypeIdResolver typeIdResolverInstance(
          MapperConfig<?> config, Annotated annotated, Class<?> implClass) {
    return (TypeIdResolver) this.beanFactory.createBean(implClass);
  }

  /** @since 4.0 */
  @Override
  public ValueInstantiator valueInstantiatorInstance(
          MapperConfig<?> config, Annotated annotated, Class<?> implClass) {

    return (ValueInstantiator) this.beanFactory.createBean(implClass);
  }

  /** @since 4.0 */
  @Override
  public ObjectIdGenerator<?> objectIdGeneratorInstance(
          MapperConfig<?> config, Annotated annotated, Class<?> implClass) {

    return (ObjectIdGenerator<?>) this.beanFactory.createBean(implClass);
  }

  /** @since 4.0 */
  @Override
  public ObjectIdResolver resolverIdGeneratorInstance(
          MapperConfig<?> config, Annotated annotated, Class<?> implClass) {

    return (ObjectIdResolver) this.beanFactory.createBean(implClass);
  }

  /** @since 4.0 */
  @Override
  public PropertyNamingStrategy namingStrategyInstance(
          MapperConfig<?> config, Annotated annotated, Class<?> implClass) {

    return (PropertyNamingStrategy) this.beanFactory.createBean(implClass);
  }

  /** @since 4.0 */
  @Override
  public Converter<?, ?> converterInstance(
          MapperConfig<?> config, Annotated annotated, Class<?> implClass) {

    return (Converter<?, ?>) this.beanFactory.createBean(implClass);
  }

  /** @since 4.0 */
  @Override
  public VirtualBeanPropertyWriter virtualPropertyWriterInstance(MapperConfig<?> config, Class<?> implClass) {
    return (VirtualBeanPropertyWriter) this.beanFactory.createBean(implClass);
  }

}
