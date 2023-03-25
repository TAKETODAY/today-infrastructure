/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.core.env;

import java.util.Map;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Specialization of {@link MapPropertySource} designed for use with
 * {@linkplain AbstractEnvironment#getSystemEnvironment() system environment variables}.
 * Compensates for constraints in Bash and other shells that do not allow for variables
 * containing the period character and/or hyphen character; also allows for uppercase
 * variations on property names for more idiomatic shell use.
 *
 * <p>For example, a call to {@code getProperty("foo.bar")} will attempt to find a value
 * for the original property or any 'equivalent' property, returning the first found:
 * <ul>
 * <li>{@code foo.bar} - the original name</li>
 * <li>{@code foo_bar} - with underscores for periods (if any)</li>
 * <li>{@code FOO.BAR} - original, with upper case</li>
 * <li>{@code FOO_BAR} - with underscores and upper case</li>
 * </ul>
 * Any hyphen variant of the above would work as well, or even mix dot/hyphen variants.
 *
 * <p>The same applies for calls to {@link #containsProperty(String)}, which returns
 * {@code true} if any of the above properties are present, otherwise {@code false}.
 *
 * <p>This feature is particularly useful when specifying active or default profiles as
 * environment variables. The following is not allowable under Bash:
 *
 * <pre class="code">infra.profiles.active=p1 java -classpath ... MyApp</pre>
 *
 * However, the following syntax is permitted and is also more conventional:
 *
 * <pre class="code">CONTEXT_PROFILES_ACTIVE=p1 java -classpath ... MyApp</pre>
 *
 * <p>Enable debug- or trace-level logging for this class (or package) for messages
 * explaining when these 'property name resolutions' occur.
 *
 * <p>This property source is included by default in {@link StandardEnvironment}
 * and all its subclasses.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @see StandardEnvironment
 * @see AbstractEnvironment#getSystemEnvironment()
 * @see AbstractEnvironment#KEY_ACTIVE_PROFILES
 * @since 4.0
 */
public class SystemEnvironmentPropertySource extends MapPropertySource {
  private static final Logger log = LoggerFactory.getLogger(SystemEnvironmentPropertySource.class);

  /**
   * Create a new {@code SystemEnvironmentPropertySource} with the given name and
   * delegating to the given {@code MapPropertySource}.
   */
  public SystemEnvironmentPropertySource(String name, Map<String, Object> source) {
    super(name, source);
  }

  /**
   * Return {@code true} if a property with the given name or any underscore/uppercase variant
   * thereof exists in this property source.
   */
  @Override
  public boolean containsProperty(String name) {
    return getProperty(name) != null;
  }

  /**
   * This implementation returns {@code true} if a property with the given name or
   * any underscore/uppercase variant thereof exists in this property source.
   */
  @Override
  @Nullable
  public Object getProperty(String name) {
    String actualName = resolvePropertyName(name);
    if (log.isDebugEnabled() && !name.equals(actualName)) {
      log.debug("PropertySource '{}' does not contain property '{}', but found equivalent '{}'",
                getName(), name, actualName);
    }
    return super.getProperty(actualName);
  }

  /**
   * Check to see if this property source contains a property with the given name, or
   * any underscore / uppercase variation thereof. Return the resolved name if one is
   * found or otherwise the original name. Never returns {@code null}.
   */
  protected final String resolvePropertyName(String name) {
    Assert.notNull(name, "Property name must not be null");
    String resolvedName = checkPropertyName(name);
    if (resolvedName != null) {
      return resolvedName;
    }
    String uppercasedName = name.toUpperCase();
    if (!name.equals(uppercasedName)) {
      resolvedName = checkPropertyName(uppercasedName);
      if (resolvedName != null) {
        return resolvedName;
      }
    }
    return name;
  }

  @Nullable
  private String checkPropertyName(String name) {
    // Check name as-is
    if (this.source.containsKey(name)) {
      return name;
    }
    // Check name with just dots replaced
    String noDotName = name.replace('.', '_');
    if (!name.equals(noDotName) && this.source.containsKey(noDotName)) {
      return noDotName;
    }
    // Check name with just hyphens replaced
    String noHyphenName = name.replace('-', '_');
    if (!name.equals(noHyphenName) && this.source.containsKey(noHyphenName)) {
      return noHyphenName;
    }
    // Check name with dots and hyphens replaced
    String noDotNoHyphenName = noDotName.replace('-', '_');
    if (!noDotName.equals(noDotNoHyphenName) && this.source.containsKey(noDotNoHyphenName)) {
      return noDotNoHyphenName;
    }
    // Give up
    return null;
  }

}
