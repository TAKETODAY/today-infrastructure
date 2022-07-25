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
package cn.taketoday.beans.factory.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.config.BeanPostProcessor;
import cn.taketoday.beans.factory.support.AutowireCandidateResolver;
import cn.taketoday.beans.factory.support.StandardDependenciesBeanPostProcessor;

/**
 * Annotation used at the field or method/constructor parameter level
 * that indicates a default value expression for the annotated element.
 *
 * <p>Typically used for expression-driven or property-driven dependency injection.
 * Also supported for dynamic resolution of handler method arguments &mdash; for
 * example, in Web MVC.
 *
 * <p>A common use case is to inject values using
 * <code>#{systemProperties.myProp}</code> style EL (SpEL)
 * expressions. Alternatively, values may be injected using
 * <code>${my.app.myProp}</code> style property placeholders.
 *
 * <p>Note that actual processing of the {@code @Value} annotation is performed
 * by a {@link BeanPostProcessor BeanPostProcessor} which in turn means that you
 * <em>cannot</em> use {@code @Value} within {@link BeanPostProcessor BeanPostProcessor} or
 * {@link BeanFactoryPostProcessor BeanFactoryPostProcessor}
 * types. Please consult the javadoc for the {@link
 * StandardDependenciesBeanPostProcessor}
 * class (which, by default, checks for the presence of this annotation).
 *
 * @author TODAY 2018-08-04 15:57
 * @see Autowired
 * @see StandardDependenciesBeanPostProcessor
 * @see AutowireCandidateResolver#getSuggestedValue
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
public @interface Value {

  /**
   * The actual value expression such as <code>#{systemProperties.myProp}</code>
   * or property placeholder such as <code>${my.app.myProp}</code>.
   */
  String value();

}
