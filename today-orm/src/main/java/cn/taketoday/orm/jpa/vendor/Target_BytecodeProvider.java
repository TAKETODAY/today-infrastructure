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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.orm.jpa.vendor;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.property.access.spi.PropertyAccess;

import java.util.Map;

/**
 * Hibernate 6.3+ substitution designed to leniently return {@code null}, as authorized by the API, to avoid throwing an
 * {@code HibernateException}.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://hibernate.atlassian.net/browse/HHH-17568">HHH-17568</a>
 * @since 4.0
 */
@TargetClass(className = "org.hibernate.bytecode.internal.none.BytecodeProviderImpl", onlyWith = SubstituteOnlyIfPresent.class)
final class Target_BytecodeProvider {

  @Substitute
  public ReflectionOptimizer getReflectionOptimizer(Class<?> clazz, Map<String, PropertyAccess> propertyAccessMap) {
    return null;
  }
}
