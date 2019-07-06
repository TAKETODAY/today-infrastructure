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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.loader;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map.Entry;

import javax.annotation.Resource;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.Constant;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Order;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.BeanReference;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.StringUtils;

/**
 * This class supports field that annotated {@link Autowired},
 * {@link javax.inject.Inject} or {@link javax.inject.Named}
 * 
 * @author TODAY <br>
 *         2018-08-04 15:56
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AutowiredPropertyResolver implements PropertyValueResolver {

    private static final Class<? extends Annotation> NAMED_CLASS = ClassUtils.loadClass("javax.inject.Named");
    private static final Class<? extends Annotation> INJECT_CLASS = ClassUtils.loadClass("javax.inject.Inject");

    @Override
    public boolean supports(ApplicationContext applicationContext, Field field) {

        return field.isAnnotationPresent(Autowired.class) //
                || field.isAnnotationPresent(Resource.class) //
                || (NAMED_CLASS != null && field.isAnnotationPresent(NAMED_CLASS))//
                || (INJECT_CLASS != null && field.isAnnotationPresent(INJECT_CLASS));
    }

    @Override
    public PropertyValue resolveProperty(ApplicationContext applicationContext, Field field) {

        final BeanNameCreator beanNameCreator = applicationContext.getEnvironment().getBeanNameCreator();

        final Autowired autowired = field.getAnnotation(Autowired.class); // auto wired

        String name = null;
        boolean required = true;
        final Class<?> propertyClass = field.getType();

        if (autowired != null) {
            name = autowired.value();
            if (StringUtils.isEmpty(name)) {
                name = byType(applicationContext, propertyClass, beanNameCreator);
            }
            required = autowired.required();
        }
        else if (field.isAnnotationPresent(Resource.class)) {
            // Resource.class
            final Resource resource = field.getAnnotation(Resource.class);
            name = resource.name();
            if (StringUtils.isEmpty(name)) { // fix resource.type() != Object.class) {
                name = byType(applicationContext, propertyClass, beanNameCreator);
            }
        }
        else if (NAMED_CLASS != null) {// @Named
            final Collection<AnnotationAttributes> annotationAttributes = //
                    ClassUtils.getAnnotationAttributes(field, NAMED_CLASS); // @Named

            if (annotationAttributes.isEmpty()) {
                name = byType(applicationContext, propertyClass, beanNameCreator);
            }
            else {
                name = annotationAttributes.iterator().next().getString(Constant.VALUE); // name attr
            }
        }
        else {// @Inject
            name = byType(applicationContext, propertyClass, beanNameCreator);
        }
        return new PropertyValue(new BeanReference(name, required, propertyClass), field);
    }

    /**
     * Create bean name by type
     * 
     * @param applicationContext
     *            {@link BeanFactory}
     * @param targetClass
     *            target property class
     * @return a bean name none null
     */
    protected String byType(ApplicationContext applicationContext, Class<?> targetClass, //
            final BeanNameCreator beanNameCreator) //
    {
        if (applicationContext.hasStarted()) {
            final String name = findName(applicationContext, targetClass);
            if (StringUtils.isNotEmpty(name)) {
                return name;
            }
        }
        return beanNameCreator.create(targetClass);
    }

    /**
     * Find bean name in the {@link BeanFactory}
     * 
     * @param applicationContext
     *            factory
     * @param propertyClass
     *            property class
     * @return a name found in {@link BeanFactory} if not found will returns null
     */
    protected String findName(ApplicationContext applicationContext, Class<?> propertyClass) {
        for (Entry<String, BeanDefinition> entry : applicationContext.getBeanDefinitions().entrySet()) {
            if (propertyClass.isAssignableFrom(entry.getValue().getBeanClass())) {
                return entry.getKey();
            }
        }
        return null;
    }

}
