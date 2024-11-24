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

package infra.context.properties.bind;

import java.io.Serial;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import infra.context.properties.source.ConfigurationProperty;
import infra.context.properties.source.ConfigurationPropertySource;

/**
 * {@link BindException} thrown when {@link ConfigurationPropertySource} elements were
 * left unbound.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class UnboundConfigurationPropertiesException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Set<ConfigurationProperty> unboundProperties;

  public UnboundConfigurationPropertiesException(Set<ConfigurationProperty> unboundProperties) {
    super(buildMessage(unboundProperties));
    this.unboundProperties = Collections.unmodifiableSet(unboundProperties);
  }

  public Set<ConfigurationProperty> getUnboundProperties() {
    return this.unboundProperties;
  }

  private static String buildMessage(Set<ConfigurationProperty> unboundProperties) {
    StringBuilder builder = new StringBuilder();
    builder.append("The elements [");
    String message = unboundProperties.stream().map((p) -> p.getName().toString()).collect(Collectors.joining(","));
    builder.append(message).append("] were left unbound.");
    return builder.toString();
  }

}
