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

package cn.taketoday.framework.context.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.lang.Assert;

/**
 * Configuration data that has been loaded from a {@link ConfigDataResource} and may
 * ultimately contribute {@link PropertySource property sources} to Spring's
 * {@link Environment}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see ConfigDataLocationResolver
 * @see ConfigDataLoader
 * @since 4.0
 */
public final class ConfigData {

  private final List<PropertySource<?>> propertySources;

  private final PropertySourceOptions propertySourceOptions;

  /**
   * A {@link ConfigData} instance that contains no data.
   */
  public static final ConfigData EMPTY = new ConfigData(Collections.emptySet());

  /**
   * Create a new {@link ConfigData} instance with the same options applied to each
   * source.
   *
   * @param propertySources the config data property sources in ascending priority
   * order.
   * @param options the config data options applied to each source
   * @see #ConfigData(Collection, PropertySourceOptions)
   */
  public ConfigData(Collection<? extends PropertySource<?>> propertySources, Option... options) {
    this(propertySources, PropertySourceOptions.always(Options.of(options)));
  }

  /**
   * Create a new {@link ConfigData} instance with specific property source options.
   *
   * @param propertySources the config data property sources in ascending priority
   * order.
   * @param propertySourceOptions the property source options
   */
  public ConfigData(Collection<? extends PropertySource<?>> propertySources,
          PropertySourceOptions propertySourceOptions) {
    Assert.notNull(propertySources, "PropertySources must not be null");
    Assert.notNull(propertySourceOptions, "PropertySourceOptions must not be null");
    this.propertySources = List.copyOf(propertySources);
    this.propertySourceOptions = propertySourceOptions;
  }

  /**
   * Return the configuration data property sources in ascending priority order. If the
   * same key is contained in more than one of the sources, then the later source will
   * win.
   *
   * @return the config data property sources
   */
  public List<PropertySource<?>> getPropertySources() {
    return this.propertySources;
  }

  /**
   * Return the {@link Options config data options} that apply to the given source.
   *
   * @param propertySource the property source to check
   * @return the options that apply
   * @since 4.0
   */
  public Options getOptions(PropertySource<?> propertySource) {
    Options options = this.propertySourceOptions.get(propertySource);
    return (options != null) ? options : Options.NONE;
  }

  /**
   * Strategy interface used to supply {@link Options} for a given
   * {@link PropertySource}.
   */
  @FunctionalInterface
  public interface PropertySourceOptions {

    /**
     * {@link PropertySourceOptions} instance that always returns
     * {@link Options#NONE}.
     */
    PropertySourceOptions ALWAYS_NONE = new AlwaysPropertySourceOptions(Options.NONE);

    /**
     * Return the options that should apply for the given property source.
     *
     * @param propertySource the property source
     * @return the options to apply
     */
    Options get(PropertySource<?> propertySource);

    /**
     * Create a new {@link PropertySourceOptions} instance that always returns the
     * same options regardless of the property source.
     *
     * @param options the options to return
     * @return a new {@link PropertySourceOptions} instance
     */
    static PropertySourceOptions always(Option... options) {
      return always(Options.of(options));
    }

    /**
     * Create a new {@link PropertySourceOptions} instance that always returns the
     * same options regardless of the property source.
     *
     * @param options the options to return
     * @return a new {@link PropertySourceOptions} instance
     */
    static PropertySourceOptions always(Options options) {
      if (options == Options.NONE) {
        return ALWAYS_NONE;
      }
      return new AlwaysPropertySourceOptions(options);
    }

  }

  /**
   * {@link PropertySourceOptions} that always returns the same result.
   */
  private record AlwaysPropertySourceOptions(Options options) implements PropertySourceOptions {

    @Override
    public Options get(PropertySource<?> propertySource) {
      return this.options;
    }

  }

  /**
   * A set of {@link Option} flags.
   */
  public static final class Options {

    /**
     * No options.
     */
    public static final Options NONE = new Options(Collections.emptySet());

    private final Set<Option> options;

    private Options(Set<Option> options) {
      this.options = Collections.unmodifiableSet(options);
    }

    Set<Option> asSet() {
      return this.options;
    }

    /**
     * Returns if the given option is contained in this set.
     *
     * @param option the option to check
     * @return {@code true} of the option is present
     */
    public boolean contains(Option option) {
      return this.options.contains(option);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      Options other = (Options) obj;
      return this.options.equals(other.options);
    }

    @Override
    public int hashCode() {
      return this.options.hashCode();
    }

    @Override
    public String toString() {
      return this.options.toString();
    }

    /**
     * Create a new {@link Options} instance that contains the options in this set
     * excluding the given option.
     *
     * @param option the option to exclude
     * @return a new {@link Options} instance
     */
    public Options without(Option option) {
      return copy((options) -> options.remove(option));
    }

    /**
     * Create a new {@link Options} instance that contains the options in this set
     * including the given option.
     *
     * @param option the option to include
     * @return a new {@link Options} instance
     */
    public Options with(Option option) {
      return copy((options) -> options.add(option));
    }

    private Options copy(Consumer<EnumSet<Option>> processor) {
      EnumSet<Option> options = EnumSet.noneOf(Option.class);
      options.addAll(this.options);
      processor.accept(options);
      return new Options(options);
    }

    /**
     * Create a new instance with the given {@link Option} values.
     *
     * @param options the options to include
     * @return a new {@link Options} instance
     */
    public static Options of(Option... options) {
      Assert.notNull(options, "Options must not be null");
      if (options.length == 0) {
        return NONE;
      }
      return new Options(EnumSet.copyOf(Arrays.asList(options)));
    }

  }

  /**
   * Option flags that can be applied.
   */
  public enum Option {

    /**
     * Ignore all imports properties from the source.
     */
    IGNORE_IMPORTS,

    /**
     * Ignore all profile activation and include properties.
     */
    IGNORE_PROFILES,

    /**
     * Indicates that the source is "profile specific" and should be included after
     * profile specific sibling imports.
     */
    PROFILE_SPECIFIC

  }

}
