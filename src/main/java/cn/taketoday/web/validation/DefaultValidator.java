/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.ElementKind;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import org.hibernate.validator.HibernateValidator;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;

/**
 * @author TODAY <br>
 *         2019-07-21 19:44
 */
@MissingBean(type = Validator.class)
public class DefaultValidator implements Validator {

    private javax.validation.Validator validator;

    @Autowired
    public DefaultValidator(Environment environment) {
        this(Validation.byProvider(HibernateValidator.class)//
                .configure()//
                .messageInterpolator(new ContextMessageInterpolator(environment))//
                .parameterNameProvider(new ContextParameterNameProvider())//
        );
    }

    public DefaultValidator(Configuration<?> configuration) {
        this(configuration.buildValidatorFactory());
    }

    public DefaultValidator(ValidatorFactory validatorFactory) {
        this(validatorFactory.getValidator());
    }

    public DefaultValidator(javax.validation.Validator validator) {
        this.validator = validator;
    }

    @Override
    public Errors validate(final Object object) {

        final Set<ConstraintViolation<Object>> violations = validator.validate(object);

        if (violations.isEmpty()) {
            return null;
        }
        return buildErrors(violations);
    }

    protected ValidationException buildErrors(final Set<ConstraintViolation<Object>> violations) {

        final ValidationException errors = new ValidationException();
        for (final ConstraintViolation<Object> violation : violations) {
            errors.addError(buildError(violation));
        }
        return errors;
    }

    /**
     * Build an error object
     * 
     * @param violation
     *            ConstraintViolation
     * @return A {@link ObjectError}
     */
    protected ObjectError buildError(final ConstraintViolation<Object> violation) {
        return new ObjectError(violation.getMessage(), getField(violation));
    }

    protected String getField(ConstraintViolation<Object> violation) {
        Path path = violation.getPropertyPath();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Path.Node node : path) {
            if (node.isInIterable()) {
                sb.append('[');
                Object index = node.getIndex();
                if (index == null) {
                    index = node.getKey();
                }
                if (index != null) {
                    sb.append(index);
                }
                sb.append(']');
            }
            String name = node.getName();
            if (name != null && node.getKind() == ElementKind.PROPERTY && !name.startsWith("<")) {
                if (!first) {
                    sb.append('.');
                }
                first = false;
                sb.append(name);
            }
        }
        return sb.toString();
    }

    /**
     * @author TODAY <br>
     *         2019-07-21 20:17
     */
    public static class ContextMessageInterpolator implements MessageInterpolator {

        private final Properties variables;

        public ContextMessageInterpolator(Environment environment) {
            this.variables = environment.getProperties();
        }

        @Override
        public String interpolate(String messageTemplate, Context context) {
            return ContextUtils.resolveValue(messageTemplate, String.class, variables);
        }

        @Override
        public String interpolate(String messageTemplate, Context context, Locale locale) {
            return ContextUtils.resolveValue(messageTemplate, String.class, variables);
        }

    }

    /**
     * @author TODAY <br>
     *         2019-07-21 20:26
     */
    public static class ContextParameterNameProvider implements ParameterNameProvider {

        @Override
        public List<String> getParameterNames(Constructor<?> constructor) {

            final List<String> parameterNames = new ArrayList<>(constructor.getParameterCount());

            for (final Parameter parameter : constructor.getParameters()) {
                parameterNames.add(parameter.getName());
            }

            return Collections.unmodifiableList(parameterNames);
        }

        @Override
        public List<String> getParameterNames(Method method) {
            return Arrays.asList(ClassUtils.getMethodArgsNames(method));
        }

    }

}
