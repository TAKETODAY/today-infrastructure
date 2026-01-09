/*
 * Copyright 2012-present the original author or authors.
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

package infra.context.annotation.config;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import infra.beans.factory.BeanClassLoaderAware;
import infra.context.BootstrapContext;
import infra.context.BootstrapContextAware;
import infra.context.annotation.Configuration;
import infra.context.annotation.DeferredImportSelector;
import infra.context.properties.bind.Binder;
import infra.core.Ordered;
import infra.core.annotation.AnnotationAttributes;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.type.AnnotationMetadata;
import infra.lang.Assert;
import infra.lang.TodayStrategies;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;
import infra.util.StringUtils;

/**
 * {@link DeferredImportSelector} to handle {@link EnableAutoConfiguration
 * auto-configuration}. This class can also be subclassed if a custom variant of
 * {@link EnableAutoConfiguration @EnableAutoConfiguration} is needed.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EnableAutoConfiguration
 * @since 4.0 2022/2/1 02:37
 */
@SuppressWarnings("NullAway")
public class AutoConfigurationImportSelector implements DeferredImportSelector,
        BeanClassLoaderAware, BootstrapContextAware, Ordered, Predicate<String> {

  private static final Logger log = LoggerFactory.getLogger(AutoConfigurationImportSelector.class);

  private static final String PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE = "infra.auto-configuration.exclude";

  static final int ORDER = Ordered.LOWEST_PRECEDENCE - 1;

  private final Class<?> autoConfigurationAnnotation;

  private ClassLoader beanClassLoader;

  protected BootstrapContext bootstrapContext;

  @Nullable
  private volatile ConfigurationClassFilter configurationClassFilter;

  @Nullable
  private volatile AutoConfigurationReplacements autoConfigurationReplacements;

  public AutoConfigurationImportSelector() {
    this(null);
  }

  protected AutoConfigurationImportSelector(@Nullable Class<?> autoConfigurationAnnotation) {
    this.autoConfigurationAnnotation = autoConfigurationAnnotation != null ? autoConfigurationAnnotation
            : AutoConfiguration.class;
  }

  @Override
  public String[] selectImports(AnnotationMetadata importMetadata) {
    if (isEnabled(importMetadata)) {
      AutoConfigurationEntry autoConfigurationEntry = getAutoConfigurationEntry(importMetadata);
      return StringUtils.toStringArray(autoConfigurationEntry.configurations);
    }
    return NO_IMPORTS;
  }

  @Override
  public Predicate<String> getExclusionFilter() {
    return this;
  }

  @Override
  public boolean test(String configurationClassName) {
    return shouldExclude(configurationClassName);
  }

  private boolean shouldExclude(String configurationClassName) {
    return getConfigurationClassFilter()
            .filter(Collections.singletonList(configurationClassName))
            .isEmpty();
  }

  /**
   * Return the {@link AutoConfigurationEntry} based on the {@link AnnotationMetadata}
   * of the importing {@link Configuration @Configuration} class.
   *
   * @param annotationMetadata the annotation metadata of the configuration class
   * @return the auto-configurations that should be imported
   */
  protected AutoConfigurationEntry getAutoConfigurationEntry(AnnotationMetadata annotationMetadata) {
    if (isEnabled(annotationMetadata)) {
      AnnotationAttributes attributes = getAttributes(annotationMetadata);
      List<String> configurations = getCandidateConfigurations(annotationMetadata, attributes);
      configurations = removeDuplicates(configurations);
      Set<String> exclusions = getExclusions(annotationMetadata, attributes);
      checkExcludedClasses(configurations, exclusions);
      configurations.removeAll(exclusions);
      configurations = getConfigurationClassFilter().filter(configurations);
      fireAutoConfigurationImportEvents(configurations, exclusions);
      return new AutoConfigurationEntry(configurations, exclusions);
    }
    return AutoConfigurationEntry.empty();
  }

  @Override
  public Class<? extends Group> getImportGroup() {
    return AutoConfigurationGroup.class;
  }

  protected boolean isEnabled(AnnotationMetadata metadata) {
    if (getClass() == AutoConfigurationImportSelector.class) {
      Environment environment = getEnvironment();
      return environment != null && environment.getFlag(
              EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY, true);
    }
    return true;
  }

  /**
   * Return the appropriate {@link AnnotationAttributes} from the
   * {@link AnnotationMetadata}. By default this method will return attributes for
   * {@link #getAnnotationClass()}.
   *
   * @param metadata the annotation metadata
   * @return annotation attributes
   */
  @Nullable
  protected AnnotationAttributes getAttributes(AnnotationMetadata metadata) {
    String name = getAnnotationClass().getName();
    AnnotationAttributes attributes = AnnotationAttributes.fromMap(
            metadata.getAnnotationAttributes(name, true));
    if (attributes == null) {
      throw new IllegalArgumentException("No auto-configuration attributes found. Is %s annotated with %s?"
              .formatted(metadata.getClassName(), ClassUtils.getShortName(name)));
    }
    return attributes;
  }

  /**
   * Return the source annotation class used by the selector.
   *
   * @return the annotation class
   */
  protected Class<?> getAnnotationClass() {
    return EnableAutoConfiguration.class;
  }

  /**
   * Return the auto-configuration class names that should be considered. By default
   * this method will load candidates using {@link TodayStrategies} with
   * {@link #getStrategyClass()}.
   *
   * @param metadata the source metadata
   * @param attributes the {@link #getAttributes(AnnotationMetadata) annotation attributes}
   * @return a list of candidate configurations
   */
  protected List<String> getCandidateConfigurations(AnnotationMetadata metadata, @Nullable AnnotationAttributes attributes) {
    var configurations = ImportCandidates.load(autoConfigurationAnnotation, getBeanClassLoader()).getCandidates();
    configurations.addAll(TodayStrategies.findNames(getStrategyClass(), getBeanClassLoader()));

    if (CollectionUtils.isEmpty(configurations)) {
      throw new IllegalArgumentException("No auto configuration classes found in META-INF/today.strategies " +
              "nor in META-INF/config/%s.imports If you are using a custom packaging, make sure that file is correct."
                      .formatted(autoConfigurationAnnotation.getName()));
    }

    return configurations;
  }

  /**
   * Return the class used by {@link TodayStrategies} to load configuration
   * candidates.
   *
   * @return the strategy class
   */
  protected Class<?> getStrategyClass() {
    return EnableAutoConfiguration.class;
  }

  private void checkExcludedClasses(List<String> configurations, Set<String> exclusions) {
    ArrayList<String> invalidExcludes = new ArrayList<>(exclusions.size());
    ClassLoader classLoader = beanClassLoader != null ? this.beanClassLoader : getClass().getClassLoader();
    for (String exclusion : exclusions) {
      if (ClassUtils.isPresent(exclusion, classLoader)
              && !configurations.contains(exclusion)) {
        invalidExcludes.add(exclusion);
      }
    }
    if (!invalidExcludes.isEmpty()) {
      handleInvalidExcludes(invalidExcludes);
    }
  }

  /**
   * Handle any invalid excludes that have been specified.
   *
   * @param invalidExcludes the list of invalid excludes (will always have at least one
   * element)
   */
  protected void handleInvalidExcludes(List<String> invalidExcludes) {
    StringBuilder message = new StringBuilder();
    for (String exclude : invalidExcludes) {
      message.append("\t- ").append(exclude).append(String.format("%n"));
    }
    throw new IllegalStateException(String.format(
            "The following classes could not be excluded because they are not auto-configuration classes:%n%s",
            message));
  }

  /**
   * Return any exclusions that limit the candidate configurations.
   *
   * @param metadata the source metadata
   * @param attributes the {@link #getAttributes(AnnotationMetadata) annotation
   * attributes}
   * @return exclusions or an empty set
   */
  protected Set<String> getExclusions(AnnotationMetadata metadata, @Nullable AnnotationAttributes attributes) {
    LinkedHashSet<String> excluded = new LinkedHashSet<>();
    if (attributes != null) {
      excluded.addAll(asList(attributes, "exclude"));
      excluded.addAll(asList(attributes, "excludeName"));
    }
    excluded.addAll(getExcludeAutoConfigurationsProperty());
    return getAutoConfigurationReplacements().replaceAll(excluded);
  }

  /**
   * Returns the auto-configurations excluded by the
   * {@code infra.auto-configuration.exclude} property.
   *
   * @return excluded auto-configurations
   */
  @SuppressWarnings("NullAway")
  protected List<String> getExcludeAutoConfigurationsProperty() {
    Environment environment = getEnvironment();
    if (environment == null) {
      return Collections.emptyList();
    }
    if (environment instanceof ConfigurableEnvironment) {
      Binder binder = Binder.get((ConfigurableEnvironment) environment);
      return binder.bind(PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE, String[].class)
              .map(Arrays::asList)
              .orElse(Collections.emptyList());
    }
    String[] excludes = environment.getProperty(PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE, String[].class);
    return excludes != null ? Arrays.asList(excludes) : Collections.emptyList();
  }

  protected List<AutoConfigurationImportFilter> getAutoConfigurationImportFilters() {
    return TodayStrategies.find(AutoConfigurationImportFilter.class, getBeanClassLoader(), bootstrapContext);
  }

  private ConfigurationClassFilter getConfigurationClassFilter() {
    ConfigurationClassFilter configurationClassFilter = this.configurationClassFilter;
    if (configurationClassFilter == null) {
      List<AutoConfigurationImportFilter> filters = getAutoConfigurationImportFilters();
      configurationClassFilter = new ConfigurationClassFilter(getBeanClassLoader(), filters);
      this.configurationClassFilter = configurationClassFilter;
    }
    return configurationClassFilter;
  }

  private AutoConfigurationReplacements getAutoConfigurationReplacements() {
    AutoConfigurationReplacements autoConfigurationReplacements = this.autoConfigurationReplacements;
    if (autoConfigurationReplacements == null) {
      autoConfigurationReplacements = AutoConfigurationReplacements.load(autoConfigurationAnnotation, getBeanClassLoader());
      this.autoConfigurationReplacements = autoConfigurationReplacements;
    }
    return autoConfigurationReplacements;
  }

  protected final <T> List<T> removeDuplicates(List<T> list) {
    return new ArrayList<>(new LinkedHashSet<>(list));
  }

  protected final List<String> asList(AnnotationAttributes attributes, String name) {
    String[] value = attributes.getStringArray(name);
    return Arrays.asList(value);
  }

  private void fireAutoConfigurationImportEvents(List<String> configurations, Set<String> exclusions) {
    var listeners = getAutoConfigurationImportListeners();
    if (!listeners.isEmpty()) {
      var event = new AutoConfigurationImportEvent(this, configurations, exclusions);
      for (AutoConfigurationImportListener listener : listeners) {
        listener.onAutoConfigurationImportEvent(event);
      }
    }
  }

  protected List<AutoConfigurationImportListener> getAutoConfigurationImportListeners() {
    return TodayStrategies.find(AutoConfigurationImportListener.class, getBeanClassLoader(), bootstrapContext);
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.beanClassLoader = classLoader;
  }

  @Override
  public void setBootstrapContext(BootstrapContext bootstrapContext) {
    this.bootstrapContext = bootstrapContext;
  }

  protected ClassLoader getBeanClassLoader() {
    return this.beanClassLoader;
  }

  @Nullable
  protected final Environment getEnvironment() {
    if (bootstrapContext == null) {
      return null;
    }
    return bootstrapContext.getEnvironment();
  }

  @Override
  public int getOrder() {
    return ORDER;
  }

  private static class ConfigurationClassFilter {

    private final List<AutoConfigurationImportFilter> filters;

    private final AutoConfigurationMetadata autoConfigurationMetadata;

    ConfigurationClassFilter(ClassLoader classLoader, List<AutoConfigurationImportFilter> filters) {
      this.autoConfigurationMetadata = AutoConfigurationMetadata.load(classLoader);
      this.filters = filters;
    }

    List<String> filter(List<String> configurations) {
      long startTime = System.nanoTime();
      String[] candidates = StringUtils.toStringArray(configurations);
      boolean skipped = false;
      for (AutoConfigurationImportFilter filter : filters) {
        boolean[] match = filter.match(candidates, autoConfigurationMetadata);
        for (int i = 0; i < match.length; i++) {
          if (!match[i]) {
            candidates[i] = null;
            skipped = true;
          }
        }
      }
      if (!skipped) {
        return configurations;
      }
      ArrayList<String> result = new ArrayList<>(candidates.length);
      for (String candidate : candidates) {
        if (candidate != null) {
          result.add(candidate);
        }
      }
      if (log.isTraceEnabled()) {
        int numberFiltered = configurations.size() - result.size();
        log.trace("Filtered {} auto configuration class in {} ms",
                numberFiltered, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
      }
      return result;
    }

  }

  private static class AutoConfigurationGroup implements DeferredImportSelector.Group {

    private final BootstrapContext context;

    private final ClassLoader beanClassLoader;

    private AutoConfigurationMetadata autoConfigurationMetadata;

    private AutoConfigurationReplacements autoConfigurationReplacements;

    private final LinkedHashMap<String, AnnotationMetadata> entries = new LinkedHashMap<>();

    private final ArrayList<AutoConfigurationEntry> autoConfigurationEntries = new ArrayList<>();

    public AutoConfigurationGroup(BootstrapContext context, ClassLoader beanClassLoader) {
      this.context = context;
      this.beanClassLoader = beanClassLoader;
    }

    @Override
    public void process(AnnotationMetadata annotationMetadata, DeferredImportSelector selector) {
      if (selector instanceof AutoConfigurationImportSelector autoConfigSelector) {

        var autoConfigurationReplacements = autoConfigSelector.getAutoConfigurationReplacements();
        Assert.state(this.autoConfigurationReplacements == null
                        || this.autoConfigurationReplacements.equals(autoConfigurationReplacements),
                "Auto-configuration replacements must be the same for each call to process");
        this.autoConfigurationReplacements = autoConfigurationReplacements;

        AutoConfigurationEntry entry = autoConfigSelector.getAutoConfigurationEntry(annotationMetadata);
        autoConfigurationEntries.add(entry);
        for (String importClassName : entry.configurations) {
          entries.putIfAbsent(importClassName, annotationMetadata);
        }
      }
      else {
        throw new IllegalStateException("Only %s implementations are supported, got %s"
                .formatted(AutoConfigurationImportSelector.class.getSimpleName(), selector.getClass().getName()));
      }
    }

    @Override
    public Iterable<Entry> selectImports() {
      if (autoConfigurationEntries.isEmpty()) {
        return Collections.emptyList();
      }

      var processedConfigurations = new LinkedHashSet<String>();
      for (AutoConfigurationEntry entry : autoConfigurationEntries) {
        processedConfigurations.addAll(entry.configurations);
      }

      for (AutoConfigurationEntry entry : autoConfigurationEntries) {
        processedConfigurations.removeAll(entry.exclusions);
      }

      List<String> sortedConfigurations = sortAutoConfigurations(processedConfigurations, getAutoConfigurationMetadata());

      var entries = new ArrayList<Entry>(sortedConfigurations.size());
      for (String importClassName : sortedConfigurations) {
        Entry entry = new Entry(this.entries.get(importClassName), importClassName);
        entries.add(entry);
      }
      return entries;
    }

    private AutoConfigurationMetadata getAutoConfigurationMetadata() {
      if (this.autoConfigurationMetadata == null) {
        this.autoConfigurationMetadata = AutoConfigurationMetadata.load(this.beanClassLoader);
      }
      return this.autoConfigurationMetadata;
    }

    private List<String> sortAutoConfigurations(Set<String> configurations, AutoConfigurationMetadata autoConfigurationMetadata) {
      return new AutoConfigurationSorter(context.getMetadataReaderFactory(), autoConfigurationMetadata, autoConfigurationReplacements::replace)
              .getInPriorityOrder(configurations);
    }

  }

  protected static class AutoConfigurationEntry {

    public final Set<String> exclusions;

    public final List<String> configurations;

    /**
     * Create an entry with the configurations that were contributed and their
     * exclusions.
     *
     * @param configurations the configurations that should be imported
     * @param exclusions the exclusions that were applied to the original list
     */
    AutoConfigurationEntry(Collection<String> configurations, Collection<String> exclusions) {
      this.configurations = new ArrayList<>(configurations);
      this.exclusions = new HashSet<>(exclusions);
    }

    static AutoConfigurationEntry empty() {
      return new AutoConfigurationEntry(Collections.emptyList(), Collections.emptyList());
    }

  }

}

