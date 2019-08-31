/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright ©  TODAY & 2017 - 2019 All Rights Reserved.
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
package cn.taketoday.context.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.OrderUtils;
import lombok.Getter;
import lombok.Setter;

/**
 * Standard implementation of {@link BeanDefinition}
 * 
 * @author TODAY <br>
 *         2019-02-01 12:29
 */
@Getter
@Setter
public class StandardBeanDefinition extends DefaultBeanDefinition implements BeanDefinition {

    /** Declaring name @since 2.1.2 */
    private String declaringName;

    private Method factoryMethod;

    public String getDeclaringName() {
        return declaringName;
    }

    public StandardBeanDefinition setDeclaringName(String declaringName) {
        this.declaringName = declaringName;
        return this;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
        return super.isAnnotationPresent(annotation) || ClassUtils.isAnnotationPresent(factoryMethod, annotation);
    }

    /**
     * {@link BeanDefinition}'s Order
     */
    @Override
    public int getOrder() {

        final int order = super.getOrder();
        if (LOWEST_PRECEDENCE == order) {
            return OrderUtils.getOrder(factoryMethod);
        }
        return order + OrderUtils.getOrder(factoryMethod);
    }

    @Override
    public String toString() {
        return new StringBuilder()//
                .append("{\n\t\"name\":\"").append(getName())//
                .append("\",\n\t\"declaringName\":\"").append(getDeclaringName())//
                .append("\",\n\t\"beanClass\":\"").append(getBeanClass())//
                .append("\",\n\t\"scope\":\"").append(getScope())//
                .append("\",\n\t\"factoryMethod\":\"").append(factoryMethod)//
                .append("\",\n\t\"initMethods\":\"").append(Arrays.toString(getInitMethods()))//
                .append("\",\n\t\"destroyMethods\":\"").append(Arrays.toString(getDestroyMethods()))//
                .append("\",\n\t\"propertyValues\":\"").append(Arrays.toString(getPropertyValues()))//
                .append("\",\n\t\"initialized\":\"").append(isInitialized())//
                .append("\",\n\t\"factoryBean\":\"").append(isFactoryBean())//
                .append("\",\n\t\"Abstract\":\"").append(isAbstract())//
                .append("\"\n}")//
                .toString();
    }

}
