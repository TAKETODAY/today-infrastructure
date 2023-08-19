/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.orm.jpa;

import java.util.Collections;

import cn.taketoday.aot.hint.ExecutableMode;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} implementation that makes sure that hints related to
 * {@link AbstractEntityManagerFactoryBean} and {@link SharedEntityManagerCreator} are registered.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/28 22:02
 */
class EntityManagerRuntimeHints implements RuntimeHintsRegistrar {

  private static final String HIBERNATE_SESSION_FACTORY_CLASS_NAME = "org.hibernate.SessionFactory";

  private static final String ENTITY_MANAGER_FACTORY_CLASS_NAME = "jakarta.persistence.EntityManagerFactory";

  private static final String QUERY_SQM_IMPL_CLASS_NAME = "org.hibernate.query.sqm.internal.QuerySqmImpl";

  private static final String NATIVE_QUERY_IMPL_CLASS_NAME = "org.hibernate.query.sql.internal.NativeQueryImpl";

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    if (ClassUtils.isPresent(HIBERNATE_SESSION_FACTORY_CLASS_NAME, classLoader)) {
      hints.proxies().registerJdkProxy(TypeReference.of(HIBERNATE_SESSION_FACTORY_CLASS_NAME),
              TypeReference.of(EntityManagerFactoryInfo.class));
      hints.proxies().registerJdkProxy(TypeReference.of("org.hibernate.Session"),
              TypeReference.of(EntityManagerProxy.class));
    }
    if (ClassUtils.isPresent(ENTITY_MANAGER_FACTORY_CLASS_NAME, classLoader)) {
      hints.reflection().registerType(TypeReference.of(ENTITY_MANAGER_FACTORY_CLASS_NAME), builder -> {
        builder.onReachableType(SharedEntityManagerCreator.class).withMethod("getCriteriaBuilder",
                Collections.emptyList(), ExecutableMode.INVOKE);
        builder.onReachableType(SharedEntityManagerCreator.class).withMethod("getMetamodel",
                Collections.emptyList(), ExecutableMode.INVOKE);
      });
    }
    try {
      Class<?> clazz = ClassUtils.forName(QUERY_SQM_IMPL_CLASS_NAME, classLoader);
      hints.proxies().registerJdkProxy(ClassUtils.getAllInterfacesForClass(clazz, classLoader));
    }
    catch (ClassNotFoundException ignored) {
    }
    try {
      Class<?> clazz = ClassUtils.forName(NATIVE_QUERY_IMPL_CLASS_NAME, classLoader);
      hints.proxies().registerJdkProxy(ClassUtils.getAllInterfacesForClass(clazz, classLoader));
    }
    catch (ClassNotFoundException ignored) {
    }
  }
}
