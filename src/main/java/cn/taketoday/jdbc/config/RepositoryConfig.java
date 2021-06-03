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

package cn.taketoday.jdbc.config;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.Constant;
import cn.taketoday.context.annotation.Repository;
import cn.taketoday.context.aware.ApplicationContextSupport;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.context.event.LoadingMissingBeanEvent;
import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.factory.DefaultBeanDefinition;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.GenericTypeResolver;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.jdbc.DefaultSession;
import cn.taketoday.jdbc.SqlType;
import cn.taketoday.jdbc.annotation.Query;

/**
 * @author TODAY 2021/3/17 18:24
 */
public class RepositoryConfig
        extends ApplicationContextSupport implements ApplicationListener<LoadingMissingBeanEvent> {

  @Override
  public void onApplicationEvent(LoadingMissingBeanEvent event) {
    final Collection<Class<?>> candidates = event.getCandidates();
    final ApplicationContext context = event.getApplicationContext();
    final BeanNameCreator beanNameCreator = context.getEnvironment().getBeanNameCreator();

    setApplicationContext(context);

    for (final Class<?> candidate : candidates) {
      if (candidate.isInterface() && ClassUtils.isAnnotationPresent(candidate, Repository.class)) {
        final Repository repository = ClassUtils.getAnnotation(Repository.class, candidate);
        final String[] names = repository.value();
        final String name = ObjectUtils.isNotEmpty(names) ? names[0] : beanNameCreator.create(candidate);

        context.registerBeanDefinition(name, createBeanDefinition(candidate, name));
      }
    }

  }

  protected BeanDefinition createBeanDefinition(final Class<?> beanClass, final String name) {
    final DefaultBeanDefinition ret = new DefaultBeanDefinition(name, beanClass) {

      @Override
      public Object newInstance(BeanFactory factory) {
        return Proxy.newProxyInstance(beanClass.getClassLoader(), new Class[] { beanClass }, new InvocationHandler0(beanClass));
      }

    };
    return ret;
  }

  class InvocationHandler0 implements InvocationHandler {
    final Class<?> beanClass;

    InvocationHandler0(Class<?> beanClass) {
      this.beanClass = beanClass;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      final AnnotationAttributes attributes = ClassUtils.getAnnotationAttributes(Query.class, method);
      if (attributes == null) {
        return null;
      }
      final ApplicationContext context = obtainApplicationContext();
      final DefaultSession session = new DefaultSession(context.getBean(DataSource.class));

      final String sql = attributes.getString(Constant.VALUE);
      final SqlType sqlType = attributes.getEnum("sqlType");
      switch (sqlType) {
        case DELETE:
          break;
        case INSERT:
          break;
        case SELECT:
          try (cn.taketoday.jdbc.Query query = session.createQuery(sql)) {
//            query.bind(args[0]);
            final Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 0) {

            }
            else {
              final Map<String, List<Integer>> paramNameToIdxMap = query.getParamNameToIdxMap();
              final Set<String> strings = paramNameToIdxMap.keySet();


            }

            final Class<?> returnType = method.getReturnType();
            if (CollectionUtils.isCollection(returnType)) {
              final Class<?> typeArgument = GenericTypeResolver.resolveReturnTypeArgument(method, List.class);
              return query.executeAndFetch(typeArgument);
            }
            else {
              return query.executeAndFetchFirst(returnType);
            }
          }
        case UPDATE:
          break;
      }

      return null;
    }
  }

}
