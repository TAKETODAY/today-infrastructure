/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.context.annotation;

import java.util.Map;

import cn.taketoday.beans.factory.parsing.Location;
import cn.taketoday.beans.factory.parsing.Problem;
import cn.taketoday.beans.factory.parsing.ProblemReporter;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.lang.Nullable;
import cn.taketoday.stereotype.Component;

/**
 * Represents a {@link Configuration @Configuration} class method annotated with
 * {@link Component @Component}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConfigurationClass
 * @see ConfigurationClassParser
 * @see ConfigurationClassBeanDefinitionReader
 * @since 4.0
 */
final class ComponentMethod {

  public final MethodMetadata metadata;

  public final ConfigurationClass configurationClass;

  public ComponentMethod(MethodMetadata metadata, ConfigurationClass configurationClass) {
    this.metadata = metadata;
    this.configurationClass = configurationClass;
  }

  public Location getResourceLocation() {
    return new Location(this.configurationClass.resource, this.metadata);
  }

  public void validate(ProblemReporter problemReporter) {
    if ("void".equals(metadata.getReturnTypeName())) {
      // declared as void: potential misuse of @Bean, maybe meant as init method instead?
      problemReporter.error(new VoidDeclaredMethodError());
    }

    if (metadata.isStatic()) {
      // static @Component methods have no further constraints to validate -> return immediately
      return;
    }

    Map<String, Object> attributes = configurationClass.metadata.getAnnotationAttributes(Configuration.class.getName());
    if (attributes != null && (Boolean) attributes.get("proxyBeanMethods") && !metadata.isOverridable()) {
      // instance @Bean methods within @Configuration classes must be overridable to accommodate CGLIB
      problemReporter.error(new NonOverridableMethodError());
    }

  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof ComponentMethod that &&
            this.configurationClass.equals(that.configurationClass) &&
            getLocalMethodIdentifier(this.metadata).equals(getLocalMethodIdentifier(that.metadata))));
  }

  @Override
  public int hashCode() {
    return this.configurationClass.hashCode() * 31 + getLocalMethodIdentifier(this.metadata).hashCode();
  }

  private static String getLocalMethodIdentifier(MethodMetadata metadata) {
    String metadataString = metadata.toString();
    int index = metadataString.indexOf(metadata.getDeclaringClassName());
    return (index >= 0 ? metadataString.substring(index + metadata.getDeclaringClassName().length()) :
            metadataString);
  }

  @Override
  public String toString() {
    return "ComponentMethod: " + this.metadata;
  }

  private class VoidDeclaredMethodError extends Problem {

    VoidDeclaredMethodError() {
      super("@Bean method '%s' must not be declared as void; change the method's return type or its annotation."
              .formatted(metadata.getMethodName()), getResourceLocation());
    }
  }

  private class NonOverridableMethodError extends Problem {

    NonOverridableMethodError() {
      super(String.format("@Component method '%s' must not be private or final; change the method's modifiers to continue",
              metadata.getMethodName()), getResourceLocation());
    }
  }

}
