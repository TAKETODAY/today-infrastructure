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
package cn.taketoday.web.mapping;

import java.lang.reflect.Method;
import java.util.List;

import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.web.interceptor.HandlerInterceptor;

/**
 * Mapping handler to a request
 * 
 * @author TODAY <br>
 *         2018-06-25 19:59:13
 */
@SuppressWarnings("serial")
public class HandlerMapping extends HandlerMethod implements WebMapping {

    //	private String				action;
    private final Object bean;
    /** 拦截器 @off*/
    private final HandlerInterceptor[] interceptors;
    
    public HandlerMapping(Object bean, 
                          Method method,
                          List<HandlerInterceptor> interceptors, List<MethodParameter> parameters) {
        
        this(bean, method, interceptors, parameters.toArray(new MethodParameter[parameters.size()]));
    }

    public HandlerMapping(Object bean, 
                          Method method, 
                          List<HandlerInterceptor> interceptors, MethodParameter... parameters) {
        super(method, parameters);
        
        this.bean = bean;
        this.interceptors = ObjectUtils.isNotEmpty(interceptors)
                            ? interceptors.toArray(new HandlerInterceptor[interceptors.size()])
                            : null;
    }//@on

    public final boolean hasInterceptor() {
        return interceptors != null;
    }

    public final Object getBean() {
        return bean;
    }

    public final HandlerInterceptor[] getInterceptors() {
        return interceptors;
    }

    public static HandlerMapping create(final Object bean,
                                        final Method method,
                                        final List<HandlerInterceptor> interceptors,
                                        final List<MethodParameter> methodParameters) {

        return new HandlerMapping(bean, method, interceptors, methodParameters);
    }

    public static HandlerMapping create(final Object bean,
                                        final Method method,
                                        final List<HandlerInterceptor> interceptors,
                                        final MethodParameter... methodParameters) {

        return new HandlerMapping(bean, method, interceptors, methodParameters);
    }

    @Override
    public Object getObject() {
        return bean;
    }

}
