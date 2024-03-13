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

package cn.taketoday.core.env;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.conversion.support.ConfigurableConversionService;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Abstract base class for {@link Environment} implementations. Supports the notion of
 * reserved default profile names and enables specifying active and default profiles
 * through the {@link #KEY_ACTIVE_PROFILES} and
 * {@link #KEY_DEFAULT_PROFILES} properties.
 *
 * <p>Concrete subclasses differ primarily on which {@link PropertySource} objects they
 * add by default. {@code AbstractEnvironment} adds none. Subclasses should contribute
 * property sources through the protected {@link #customizePropertySources(PropertySources)}
 * hook, while clients should customize using {@link ConfigurableEnvironment#getPropertySources()}
 * and working against the {@link PropertySources} API.
 * See {@link ConfigurableEnvironment} javadoc for usage examples.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConfigurableEnvironment
 * @see StandardEnvironment
 * @since 4.0
 */
public abstract class AbstractEnvironment implements ConfigurableEnvironment, IterablePropertyResolver {
  private static final Logger log = LoggerFactory.getLogger(AbstractEnvironment.class);

  private final LinkedHashSet<String> activeProfiles = new LinkedHashSet<>();

  private final LinkedHashSet<String> defaultProfiles = new LinkedHashSet<>(getReservedDefaultProfiles());

  private final PropertySources propertySources;

  private final ConfigurablePropertyResolver propertyResolver;

  /**
   * Create a new {@code Environment} instance, calling back to
   * {@link #customizePropertySources(PropertySources)} during construction to
   * allow subclasses to contribute or manipulate {@link PropertySource} instances as
   * appropriate.
   *
   * @see #customizePropertySources(PropertySources)
   */
  public AbstractEnvironment() {
    this(new PropertySources());
  }

  /**
   * Create a new {@code Environment} instance with a specific
   * {@link PropertySources} instance, calling back to
   * {@link #customizePropertySources(PropertySources)} during
   * construction to allow subclasses to contribute or manipulate
   * {@link PropertySource} instances as appropriate.
   *
   * @see #customizePropertySources(PropertySources)
   */
  protected AbstractEnvironment(PropertySources propertySources) {
    this.propertySources = propertySources;
    this.propertyResolver = createPropertyResolver(propertySources);
    customizePropertySources(propertySources);
  }

  /**
   * Factory method used to create the {@link ConfigurablePropertyResolver}
   * instance used by the Environment.
   *
   * @see #getPropertyResolver()
   */
  protected ConfigurablePropertyResolver createPropertyResolver(PropertySources propertySources) {
    return new PropertySourcesPropertyResolver(propertySources);
  }

  /**
   * Return the {@link ConfigurablePropertyResolver} being used by the
   * {@link Environment}.
   *
   * @see #createPropertyResolver(PropertySources)
   */
  protected final ConfigurablePropertyResolver getPropertyResolver() {
    return this.propertyResolver;
  }

  /**
   * Customize the set of {@link PropertySource} objects to be searched by this
   * {@code Environment} during calls to {@link #getProperty(String)} and related
   * methods.
   *
   * <p>Subclasses that override this method are encouraged to add property
   * sources using {@link PropertySources#addLast(PropertySource)} such that
   * further subclasses may call {@code super.customizePropertySources()} with
   * predictable results. For example:
   * <pre> {@code
   * public class Level1Environment extends AbstractEnvironment {
   *     @Override
   *     protected void customizePropertySources(PropertySources propertySources) {
   *         super.customizePropertySources(propertySources); // no-op from base class
   *         propertySources.addLast(new PropertySourceA(...));
   *         propertySources.addLast(new PropertySourceB(...));
   *     }
   * }
   *
   * public class Level2Environment extends Level1Environment {
   *     @Override
   *     protected void customizePropertySources(PropertySources propertySources) {
   *         super.customizePropertySources(propertySources); // add all from superclass
   *         propertySources.addLast(new PropertySourceC(...));
   *         propertySources.addLast(new PropertySourceD(...));
   *     }
   * }
   * }</pre>
   * In this arrangement, properties will be resolved against sources A, B, C, D in that
   * order. That is to say that property source "A" has precedence over property source
   * "D". If the {@code Level2Environment} subclass wished to give property sources C
   * and D higher precedence than A and B, it could simply call
   * {@code super.customizePropertySources} after, rather than before adding its own:
   * <pre> {@code
   * public class Level2Environment extends Level1Environment {
   *     @Override
   *     protected void customizePropertySources(PropertySources propertySources) {
   *         propertySources.addLast(new PropertySourceC(...));
   *         propertySources.addLast(new PropertySourceD(...));
   *         super.customizePropertySources(propertySources); // add all from superclass
   *     }
   * }
   * }</pre>
   * The search order is now C, D, A, B as desired.
   *
   * <p>Beyond these recommendations, subclasses may use any of the {@code add&#42;},
   * {@code remove}, or {@code replace} methods exposed by {@link PropertySources}
   * in order to create the exact arrangement of property sources desired.
   *
   * <p>The base implementation registers no property sources.
   *
   * <p>Note that clients of any {@link ConfigurableEnvironment} may further customize
   * property sources via the {@link #getPropertySources()} accessor, typically within
   * an {@link cn.taketoday.context.ApplicationContextInitializer}. For example:
   * <pre class="code">
   * ConfigurableEnvironment env = new StandardEnvironment();
   * env.getPropertySources().addLast(new PropertySourceX(...));
   * </pre>
   *
   * <h2>A warning about instance variable access</h2>
   * Instance variables declared in subclasses and having default initial values should
   * <em>not</em> be accessed from within this method. Due to Java object creation
   * lifecycle constraints, any initial value will not yet be assigned when this
   * callback is invoked by the {@link #AbstractEnvironment()} constructor, which may
   * lead to a {@code NullPointerException} or other problems. If you need to access
   * default values of instance variables, leave this method as a no-op and perform
   * property source manipulation and instance variable access directly within the
   * subclass constructor. Note that <em>assigning</em> values to instance variables is
   * not problematic; it is only attempting to read default values that must be avoided.
   *
   * @see PropertySources
   * @see PropertySourcesPropertyResolver
   */
  protected void customizePropertySources(PropertySources propertySources) { }

  /**
   * Return the set of reserved default profile names. This implementation returns
   * {@value #DEFAULT_PROFILE}. Subclasses may override in order to
   * customize the set of reserved names.
   *
   * @see #DEFAULT_PROFILE
   * @see #doGetDefaultProfiles()
   */
  protected Set<String> getReservedDefaultProfiles() {
    return Collections.singleton(DEFAULT_PROFILE);
  }

  //---------------------------------------------------------------------
  // Implementation of ConfigurableEnvironment interface
  //---------------------------------------------------------------------

  @Override
  public String[] getActiveProfiles() {
    return StringUtils.toStringArray(doGetActiveProfiles());
  }

  /**
   * Return the set of active profiles as explicitly set through
   * {@link #setActiveProfiles} or if the current set of active profiles
   * is empty, check for the presence of {@link #doGetActiveProfilesProperty()}
   * and assign its value to the set of active profiles.
   *
   * @see #getActiveProfiles()
   * @see #doGetActiveProfilesProperty()
   */
  protected Set<String> doGetActiveProfiles() {
    synchronized(this.activeProfiles) {
      if (this.activeProfiles.isEmpty()) {
        String profiles = doGetActiveProfilesProperty();
        if (StringUtils.hasText(profiles)) {
          setActiveProfiles(StringUtils.commaDelimitedListToStringArray(
                  StringUtils.trimAllWhitespace(profiles)));
        }
      }
      return this.activeProfiles;
    }
  }

  /**
   * Return the property value for the active profiles.
   *
   * @see #KEY_ACTIVE_PROFILES
   */
  @Nullable
  protected String doGetActiveProfilesProperty() {
    return getProperty(KEY_ACTIVE_PROFILES);
  }

  @Override
  public void setActiveProfiles(String... profiles) {
    Assert.notNull(profiles, "Profile array is required");
    if (log.isDebugEnabled()) {
      log.debug("Activating profiles {}", Arrays.asList(profiles));
    }
    synchronized(this.activeProfiles) {
      this.activeProfiles.clear();
      for (String profile : profiles) {
        validateProfile(profile);
        this.activeProfiles.add(profile);
      }
    }
  }

  @Override
  public void addActiveProfile(String profile) {
    log.debug("Activating profile '{}'", profile);
    validateProfile(profile);
    doGetActiveProfiles();
    synchronized(this.activeProfiles) {
      this.activeProfiles.add(profile);
    }
  }

  @Override
  public String[] getDefaultProfiles() {
    return StringUtils.toStringArray(doGetDefaultProfiles());
  }

  /**
   * Return the set of default profiles explicitly set via
   * {@link #setDefaultProfiles(String...)} or if the current set of default profiles
   * consists only of {@linkplain #getReservedDefaultProfiles() reserved default
   * profiles}, then check for the presence of {@link #doGetActiveProfilesProperty()}
   * and assign its value (if any) to the set of default profiles.
   *
   * @see #AbstractEnvironment()
   * @see #getDefaultProfiles()
   * @see #getReservedDefaultProfiles()
   * @see #doGetDefaultProfilesProperty()
   */
  protected Set<String> doGetDefaultProfiles() {
    synchronized(this.defaultProfiles) {
      if (this.defaultProfiles.equals(getReservedDefaultProfiles())) {
        String profiles = doGetDefaultProfilesProperty();
        if (StringUtils.hasText(profiles)) {
          setDefaultProfiles(StringUtils.commaDelimitedListToStringArray(
                  StringUtils.trimAllWhitespace(profiles)));
        }
      }
      return this.defaultProfiles;
    }
  }

  /**
   * Return the property value for the default profiles.
   *
   * @see #KEY_DEFAULT_PROFILES
   */
  @Nullable
  protected String doGetDefaultProfilesProperty() {
    return getProperty(KEY_DEFAULT_PROFILES);
  }

  /**
   * Specify the set of profiles to be made active by default if no other profiles
   * are explicitly made active through {@link #setActiveProfiles}.
   * <p>Calling this method removes overrides any reserved default profiles
   * that may have been added during construction of the environment.
   *
   * @see #AbstractEnvironment()
   * @see #getReservedDefaultProfiles()
   */
  @Override
  public void setDefaultProfiles(String... profiles) {
    Assert.notNull(profiles, "Profile array is required");
    synchronized(this.defaultProfiles) {
      this.defaultProfiles.clear();
      for (String profile : profiles) {
        validateProfile(profile);
        this.defaultProfiles.add(profile);
      }
    }
  }

  @Override
  public boolean acceptsProfiles(Profiles profiles) {
    Assert.notNull(profiles, "Profiles is required");
    return profiles.matches(this::isProfileActive);
  }

  /**
   * Return whether the given profile is active, or if active profiles are empty
   * whether the profile should be active by default.
   *
   * @throws IllegalArgumentException per {@link #validateProfile(String)}
   */
  protected boolean isProfileActive(String profile) {
    validateProfile(profile);
    Set<String> currentActiveProfiles = doGetActiveProfiles();
    return currentActiveProfiles.contains(profile)
            || (currentActiveProfiles.isEmpty() && doGetDefaultProfiles().contains(profile));
  }

  /**
   * Validate the given profile, called internally prior to adding to the set of
   * active or default profiles.
   * <p>Subclasses may override to impose further restrictions on profile syntax.
   *
   * @throws IllegalArgumentException if the profile is null, empty, whitespace-only or
   * begins with the profile NOT operator (!).
   * @see #matchesProfiles
   * @see #addActiveProfile
   * @see #setDefaultProfiles
   */
  protected void validateProfile(String profile) {
    if (StringUtils.isBlank(profile)) {
      throw new IllegalArgumentException(
              "Invalid profile [" + profile + "]: must contain text");
    }
    if (profile.charAt(0) == '!') {
      throw new IllegalArgumentException(
              "Invalid profile [" + profile + "]: must not begin with ! operator");
    }
  }

  @Override
  public PropertySources getPropertySources() {
    return this.propertySources;
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public Map<String, Object> getSystemProperties() {
    return (Map) System.getProperties();
  }

  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public Map<String, Object> getSystemEnvironment() {
    if (suppressGetenvAccess()) {
      return Collections.emptyMap();
    }
    return (Map) System.getenv();
  }

  /**
   * Determine whether to suppress {@link System#getenv()}/{@link System#getenv(String)}
   * access for the purposes of {@link #getSystemEnvironment()}.
   * <p>If this method returns {@code true}, an empty dummy Map will be used instead
   * of the regular system environment Map, never even trying to call {@code getenv}
   * and therefore avoiding security manager warnings (if any).
   * <p>The default implementation checks for the "infra.getenv.ignore" system property,
   * returning {@code true} if its value equals "true" in any case.
   *
   * @see #KEY_IGNORE_GETENV
   * @see TodayStrategies#getFlag
   */
  protected boolean suppressGetenvAccess() {
    return TodayStrategies.getFlag(KEY_IGNORE_GETENV);
  }

  @Override
  public void merge(ConfigurableEnvironment parent) {
    for (PropertySource<?> ps : parent.getPropertySources()) {
      if (!this.propertySources.contains(ps.getName())) {
        this.propertySources.addLast(ps);
      }
    }
    String[] parentActiveProfiles = parent.getActiveProfiles();
    if (ObjectUtils.isNotEmpty(parentActiveProfiles)) {
      synchronized(this.activeProfiles) {
        Collections.addAll(this.activeProfiles, parentActiveProfiles);
      }
    }
    String[] parentDefaultProfiles = parent.getDefaultProfiles();
    if (ObjectUtils.isNotEmpty(parentDefaultProfiles)) {
      synchronized(this.defaultProfiles) {
        this.defaultProfiles.remove(DEFAULT_PROFILE);
        Collections.addAll(this.defaultProfiles, parentDefaultProfiles);
      }
    }
  }

  //---------------------------------------------------------------------
  // Implementation of ConfigurablePropertyResolver interface
  //---------------------------------------------------------------------

  @Override
  public ConfigurableConversionService getConversionService() {
    return this.propertyResolver.getConversionService();
  }

  @Override
  public void setConversionService(ConfigurableConversionService conversionService) {
    this.propertyResolver.setConversionService(conversionService);
  }

  @Override
  public void setPlaceholderPrefix(String placeholderPrefix) {
    this.propertyResolver.setPlaceholderPrefix(placeholderPrefix);
  }

  @Override
  public void setPlaceholderSuffix(String placeholderSuffix) {
    this.propertyResolver.setPlaceholderSuffix(placeholderSuffix);
  }

  @Override
  public void setValueSeparator(@Nullable String valueSeparator) {
    this.propertyResolver.setValueSeparator(valueSeparator);
  }

  @Override
  public void setEscapeCharacter(@Nullable Character escapeCharacter) {
    this.propertyResolver.setEscapeCharacter(escapeCharacter);
  }

  @Override
  public void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders) {
    this.propertyResolver.setIgnoreUnresolvableNestedPlaceholders(ignoreUnresolvableNestedPlaceholders);
  }

  @Override
  public void setRequiredProperties(String... requiredProperties) {
    this.propertyResolver.setRequiredProperties(requiredProperties);
  }

  @Override
  public void addRequiredProperties(String... requiredProperties) {
    propertyResolver.addRequiredProperties(requiredProperties);
  }

  @Override
  public void validateRequiredProperties() throws MissingRequiredPropertiesException {
    this.propertyResolver.validateRequiredProperties();
  }

  //---------------------------------------------------------------------
  // Implementation of PropertyResolver interface
  //---------------------------------------------------------------------

  @Override
  public boolean containsProperty(String key) {
    return this.propertyResolver.containsProperty(key);
  }

  @Override
  @Nullable
  public String getProperty(String key) {
    return this.propertyResolver.getProperty(key);
  }

  @Override
  public String getProperty(String key, String defaultValue) {
    return this.propertyResolver.getProperty(key, defaultValue);
  }

  @Override
  @Nullable
  public <T> T getProperty(String key, Class<T> targetType) {
    return this.propertyResolver.getProperty(key, targetType);
  }

  @Override
  public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
    return this.propertyResolver.getProperty(key, targetType, defaultValue);
  }

  @Override
  public String getRequiredProperty(String key) throws IllegalStateException {
    return this.propertyResolver.getRequiredProperty(key);
  }

  @Override
  public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
    return this.propertyResolver.getRequiredProperty(key, targetType);
  }

  @Override
  public String resolvePlaceholders(String text) {
    return this.propertyResolver.resolvePlaceholders(text);
  }

  @Override
  public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
    return this.propertyResolver.resolveRequiredPlaceholders(text);
  }

  //---------------------------------------------------------------------
  // Implementation of IterablePropertyResolver interface
  //---------------------------------------------------------------------

  @Override
  public Iterator<String> iterator() {
    if (propertyResolver instanceof IterablePropertyResolver resolver) {
      return resolver.iterator();
    }
    return Collections.emptyIterator();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " {activeProfiles=" + this.activeProfiles +
            ", defaultProfiles=" + this.defaultProfiles + ", propertySources=" + this.propertySources + "}";
  }

}
