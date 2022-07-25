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

package cn.taketoday.context.annotation;

import cn.taketoday.beans.factory.parsing.Location;
import cn.taketoday.beans.factory.parsing.Problem;
import cn.taketoday.beans.factory.parsing.ProblemReporter;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.stereotype.Component;
import cn.taketoday.lang.Nullable;

/**
 * Represents a {@link Configuration @Configuration} class method annotated with
 * {@link Component @Component}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see ConfigurationClass
 * @see ConfigurationClassParser
 * @see ConfigurationClassBeanDefinitionReader
 * @since 4.0
 */
final class ComponentMethod {

  private final MethodMetadata metadata;
  private final ConfigurationClass configurationClass;

  public ComponentMethod(MethodMetadata metadata, ConfigurationClass configurationClass) {
    this.metadata = metadata;
    this.configurationClass = configurationClass;
  }

  public MethodMetadata getMetadata() {
    return this.metadata;
  }

  public ConfigurationClass getConfigurationClass() {
    return this.configurationClass;
  }

  public Location getResourceLocation() {
    return new Location(this.configurationClass.getResource(), this.metadata);
  }

  public void validate(ProblemReporter problemReporter) {
    if (getMetadata().isStatic()) {
      // static @Component methods have no constraints to validate -> return immediately
      return;
    }

    if (configurationClass.getMetadata().isAnnotated(Configuration.class.getName())) {
      if (!getMetadata().isOverridable()) {
        // instance @Component methods within @Configuration classes must be overridable to accommodate CGLIB
        problemReporter.error(new NonOverridableMethodError());
      }
    }
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return (this == obj) || ((obj instanceof ComponentMethod)
            && this.metadata.equals(((ComponentMethod) obj).metadata));
  }

  @Override
  public int hashCode() {
    return this.metadata.hashCode();
  }

  @Override
  public String toString() {
    return "ComponentMethod: " + this.metadata;
  }

  private class NonOverridableMethodError extends Problem {

    NonOverridableMethodError() {
      super(String.format("@Component method '%s' must not be private or final; change the method's modifiers to continue",
              getMetadata().getMethodName()), getResourceLocation());
    }
  }

}
