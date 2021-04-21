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

package cn.taketoday.web.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import javax.validation.Configuration;
import javax.validation.Validation;
import javax.validation.spi.ValidationProvider;

import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.condition.ConditionalOnClass;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.web.resolver.ParameterResolvers;

/**
 * @author TODAY 2021/3/21 21:37
 * @since 3.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Import(BeanValidationConfig.class)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface EnableBeanValidation {

}

class BeanValidationConfig {

  @MissingBean
  WebValidator webValidator(List<Validator> validators) {
    return new WebValidator(validators);
  }

  @MissingBean
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @ConditionalOnClass("org.hibernate.validator.HibernateValidator")
  DefaultJavaxValidator hibernateValidator(Environment environment) {
    final Class<ValidationProvider> aClass = ClassUtils.loadClass("org.hibernate.validator.HibernateValidator");

    final Configuration hibernateValidatorConfig = Validation.byProvider(aClass)
            .configure()
            .messageInterpolator(new ContextMessageInterpolator(environment))
            .parameterNameProvider(new ContextParameterNameProvider());

    return new DefaultJavaxValidator(hibernateValidatorConfig);
  }

  @MissingBean
  @ConditionalOnClass("javax.validation.Valid")
  ValidationParameterResolver validationParameterResolver(WebValidator validator, ParameterResolvers resolvers) {
    return new ValidationParameterResolver(validator, resolvers);
  }

  @MissingBean
  ErrorsParameterResolver errorsParameterResolver() {
    return new ErrorsParameterResolver();
  }

}
