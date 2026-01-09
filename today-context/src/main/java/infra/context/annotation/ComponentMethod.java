/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.annotation;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.parsing.Location;
import infra.beans.factory.parsing.Problem;
import infra.beans.factory.parsing.ProblemReporter;
import infra.core.type.MethodMetadata;
import infra.stereotype.Component;

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

  @SuppressWarnings("NullAway")
  public void validate(ProblemReporter problemReporter) {
    if (metadata.isAnnotated(Autowired.class)) {
      // declared as @Autowired: semantic mismatch since @Component method arguments are autowired
      // in any case whereas @Autowired methods are setter-like methods on the containing class
      problemReporter.error(new AutowiredDeclaredMethodError());
    }

    if ("void".equals(metadata.getReturnTypeName())) {
      // declared as void: potential misuse of @Bean, maybe meant as init method instead?
      problemReporter.error(new VoidDeclaredMethodError());
    }

    if (metadata.isStatic()) {
      // static @Component methods have no further constraints to validate -> return immediately
      return;
    }

    var annotation = configurationClass.metadata.getAnnotation(Configuration.class);
    if (annotation.isPresent() && annotation.getBoolean("proxyBeanMethods") && !metadata.isOverridable()) {
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

  private class AutowiredDeclaredMethodError extends Problem {

    AutowiredDeclaredMethodError() {
      super("@Component method '%s' must not be declared as autowired; remove the method-level @Autowired annotation."
              .formatted(metadata.getMethodName()), getResourceLocation());
    }
  }

  private class VoidDeclaredMethodError extends Problem {

    VoidDeclaredMethodError() {
      super("@Component method '%s' must not be declared as void; change the method's return type or its annotation."
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
