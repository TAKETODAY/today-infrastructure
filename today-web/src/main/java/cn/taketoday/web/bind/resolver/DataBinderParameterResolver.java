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
package cn.taketoday.web.bind.resolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.BeanMetadata;
import cn.taketoday.beans.PropertyValue;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.ConversionServiceAware;
import cn.taketoday.format.support.ApplicationConversionService;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.bind.RequestContextDataBinder;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * Resolve Bean
 *
 * <p>
 * supports annotated-property-resolvers if set the ParameterResolvers,
 * this feature is that request-params Bean property is annotated with meta-annotation RequestParam
 * just like this:
 * <pre>
 *
 * &#64Data
 * static class RequestParams {
 *   &#64RequestHeader("X-Header")
 *   private String header;
 *
 *   &#64RequestHeader("Accept-Encoding")
 *   private String acceptEncoding;
 *
 *   private String name;
 *
 *   &#64SessionAttribute("session")
 *   private String session;
 *
 *   &#64CookieValue
 *   private String cookie;
 *
 *   &#64CookieValue("Authorization")
 *   private String authorization;
 *
 * }
 *
 * &#64GET("/binder")
 * public RequestParams binder(RequestParams params, WebSession session) {
 *   session.setAttribute("session", params.toString());
 *   return params;
 * }
 *
 * // result:
 * {
 *     "header": "test",
 *     "acceptEncoding": "gzip, deflate, br",
 *     "name": "TODAY",
 *     "session": "TestController.RequestParams(header=test, acceptEncoding=gzip, deflate, br, name=TODAY, session=null, cookie=TODAY, authorization=bb79c2b9-3b18-4947-af58-257d0ff89c3d)",
 *     "cookie": "TODAY",
 *     "authorization": "2549afee-f385-4f6e-876b-e99facc8e4ce"
 * }
 * </pre>
 * </p>
 *
 * @author TODAY 2019-07-13 01:11
 */
@Deprecated
public class DataBinderParameterResolver
        implements ParameterResolvingStrategy, ConversionServiceAware {
  public static final String ANNOTATED_RESOLVERS_KEY = AnnotatedPropertyResolver.class.getName() + "-annotated-property-resolvers";

  private ConversionService conversionService = ApplicationConversionService.getSharedInstance();

  private ParameterResolvingRegistry registry;

  public DataBinderParameterResolver() { }

  public DataBinderParameterResolver(ParameterResolvingRegistry resolvers) {
    this();
    this.registry = resolvers;
  }

  public DataBinderParameterResolver(ConversionService conversionService) {
    this();
    setConversionService(conversionService);
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter parameter) {
    if (!parameter.isInterface()
            && !parameter.hasParameterAnnotation(RequestBody.class) // @since 3.0.3 #17
            && !ClassUtils.isSimpleType(parameter.getParameterType())) {
      setAttribute(parameter, registry);
      return true;
    }
    return false;
  }

  /**
   * @since 4.0
   */
  static void setAttribute(ResolvableMethodParameter parameter, ParameterResolvingRegistry registry) {
    if (registry != null) {
      // supports annotated-property-resolvers
      ArrayList<AnnotatedPropertyResolver> resolverList = new ArrayList<>();
      Class<?> parameterClass = parameter.getParameterType();

      ReflectionUtils.doWithFields(parameterClass, field -> {
        if (AnnotationUtils.isPresent(field, RequestParam.class)) {
          resolverList.add(new AnnotatedPropertyResolver(parameter, field, registry));
        }
      });
      parameter.setAttribute(ANNOTATED_RESOLVERS_KEY, resolverList);
    }
  }

  /**
   * @return Pojo parameter
   */
  @Override
  public Object resolveArgument(
          final RequestContext context, final ResolvableMethodParameter resolvable) throws Throwable {
    final Class<?> parameterClass = resolvable.getParameterType();

    BeanMetadata beanMetadata = BeanMetadata.from(parameterClass);
    Object target = beanMetadata.newInstance();
    RequestContextDataBinder dataBinder = new RequestContextDataBinder(target, resolvable.getName());
    dataBinder.setConversionService(conversionService);
    dataBinder.bind(context);

    // #30 Support annotation-supported in the form of DataBinder
    resolveAnnotatedProperty(context, resolvable, dataBinder);
    // todo dataBinder.validate();
    return target;
  }

  /**
   * @since 4.0
   */
  static void resolveAnnotatedProperty(
          RequestContext context, ResolvableMethodParameter parameter, RequestContextDataBinder dataBinder) throws Throwable {
    Object attribute = parameter.getAttribute(ANNOTATED_RESOLVERS_KEY);
    if (attribute instanceof List) {
      PropertyValues propertyValues = new PropertyValues();
      @SuppressWarnings("unchecked")
      List<AnnotatedPropertyResolver> resolvers = (List<AnnotatedPropertyResolver>) attribute;
      for (final AnnotatedPropertyResolver resolver : resolvers) {
        PropertyValue propertyValue = resolver.resolve(context);
        propertyValues.add(propertyValue);
      }

      if (!propertyValues.isEmpty()) {
        dataBinder.bind(propertyValues);
      }
    }
  }

  /**
   * @since 4.0
   */
  public void setRegistry(ParameterResolvingRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void setConversionService(ConversionService conversionService) {
    Assert.notNull(conversionService, "conversionService must not be null");
    this.conversionService = conversionService;
  }

  public ConversionService getConversionService() {
    return conversionService;
  }

  /**
   * @since 4.0
   */
  static final class AnnotatedPropertyResolver {

    final String propertyName;
    final ParameterResolvingStrategy resolver;
    final AnnotationBinderParameter parameter;

    /**
     * @throws IllegalStateException If there isn't a suitable resolver
     */
    AnnotatedPropertyResolver(ResolvableMethodParameter other, Field field, ParameterResolvingRegistry registry) {
      this.propertyName = field.getName();// TODO BeanMetadata#getPropertyName
      this.parameter = new AnnotationBinderParameter(other, field);
      this.resolver = registry.obtainStrategy(this.parameter);
    }

    public PropertyValue resolve(RequestContext context) throws Throwable {
      Object value = resolver.resolveArgument(context, parameter);
      return new PropertyValue(propertyName, value);
    }

  }

  /**
   * @since 4.0
   */
  static final class AnnotationBinderParameter extends ResolvableMethodParameter {
    private final Field field;
    private final Class<?> parameterClass;

    public AnnotationBinderParameter(ResolvableMethodParameter other, Field field) {
      super(other);
      this.parameterClass = field.getType();
      this.field = field;
    }

    @Override
    public Class<?> getParameterType() {
      return parameterClass;
    }

    @Override
    public boolean hasParameterAnnotation(Class<? extends Annotation> annotationClass) {
      return field.isAnnotationPresent(annotationClass);
    }

    @Override
    public <A extends Annotation> A getParameterAnnotation(Class<A> annotationType) {
      return field.getAnnotation(annotationType);
    }

    @Override
    protected TypeDescriptor createTypeDescriptor() {
      return TypeDescriptor.fromField(field);
    }

  }
}
