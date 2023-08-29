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

package cn.taketoday.context.annotation.config;

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

import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.context.BootstrapContext;
import cn.taketoday.context.BootstrapContextAware;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.DeferredImportSelector;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

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
public class AutoConfigurationImportSelector implements DeferredImportSelector,
        BeanClassLoaderAware, BootstrapContextAware, Ordered, Predicate<String> {

  private static final Logger log = LoggerFactory.getLogger(AutoConfigurationImportSelector.class);

  private static final String PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE = "infra.autoconfigure.exclude";

  private ClassLoader beanClassLoader;

  private BootstrapContext bootstrapContext;

  private ConfigurationClassFilter configurationClassFilter;

  @Override
  public String[] selectImports(AnnotationMetadata annotationMetadata) {
    if (isEnabled(annotationMetadata)) {
      AutoConfigurationEntry autoConfigurationEntry = getAutoConfigurationEntry(annotationMetadata);
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
      throw new IllegalArgumentException("No auto-configuration attributes found. Is " + metadata.getClassName()
              + " annotated with " + ClassUtils.getShortName(name) + "?");
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
   * this method will load candidates using {@link cn.taketoday.lang.TodayStrategies} with
   * {@link #getStrategyClass()}.
   *
   * @param metadata the source metadata
   * @param attributes the {@link #getAttributes(AnnotationMetadata) annotation attributes}
   * @return a list of candidate configurations
   */
  protected List<String> getCandidateConfigurations(
          AnnotationMetadata metadata, @Nullable AnnotationAttributes attributes) {
    var configurations = ImportCandidates.load(AutoConfiguration.class, getBeanClassLoader()).getCandidates();
    configurations.addAll(TodayStrategies.findNames(getStrategyClass(), getBeanClassLoader()));

    Assert.notEmpty(configurations,
            "No auto configuration classes found in META-INF/today-strategies.properties " +
                    "nor in META-INF/config/cn.taketoday.context.annotation.config.AutoConfiguration.imports." +
                    " If you are using a custom packaging, make sure that file is correct.");
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
    ClassLoader classLoader = getClass().getClassLoader();
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
    return excluded;
  }

  /**
   * Returns the auto-configurations excluded by the
   * {@code infra.autoconfigure.exclude} property.
   *
   * @return excluded auto-configurations
   */
  protected List<String> getExcludeAutoConfigurationsProperty() {
    Environment environment = getEnvironment();
    if (environment == null) {
      return Collections.emptyList();
    }
    if (environment instanceof ConfigurableEnvironment) {
      Binder binder = Binder.get(environment);
      return binder.bind(PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE, String[].class)
              .map(Arrays::asList)
              .orElse(Collections.emptyList());
    }
    String[] excludes = environment.getProperty(PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE, String[].class);
    return excludes != null ? Arrays.asList(excludes) : Collections.emptyList();
  }

  protected List<AutoConfigurationImportFilter> getAutoConfigurationImportFilters() {
    return TodayStrategies.find(AutoConfigurationImportFilter.class, beanClassLoader, bootstrapContext);
  }

  private ConfigurationClassFilter getConfigurationClassFilter() {
    if (this.configurationClassFilter == null) {
      List<AutoConfigurationImportFilter> filters = getAutoConfigurationImportFilters();
      this.configurationClassFilter = new ConfigurationClassFilter(this.beanClassLoader, filters);
    }
    return this.configurationClassFilter;
  }

  protected final <T> List<T> removeDuplicates(List<T> list) {
    return new ArrayList<>(new LinkedHashSet<>(list));
  }

  protected final List<String> asList(AnnotationAttributes attributes, String name) {
    String[] value = attributes.getStringArray(name);
    return Arrays.asList(value);
  }

  private void fireAutoConfigurationImportEvents(List<String> configurations, Set<String> exclusions) {
    List<AutoConfigurationImportListener> listeners = getAutoConfigurationImportListeners();
    if (!listeners.isEmpty()) {
      var event = new AutoConfigurationImportEvent(this, configurations, exclusions);
      for (AutoConfigurationImportListener listener : listeners) {
        listener.onAutoConfigurationImportEvent(event);
      }
    }
  }

  protected List<AutoConfigurationImportListener> getAutoConfigurationImportListeners() {
    return TodayStrategies.find(AutoConfigurationImportListener.class, beanClassLoader, bootstrapContext);
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

  protected final Environment getEnvironment() {
    if (bootstrapContext == null) {
      return null;
    }
    return bootstrapContext.getEnvironment();
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE - 1;
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

    private final LinkedHashMap<String, AnnotationMetadata> entries = new LinkedHashMap<>();
    private final ArrayList<AutoConfigurationEntry> autoConfigurationEntries = new ArrayList<>();

    public AutoConfigurationGroup(BootstrapContext context, ClassLoader beanClassLoader) {
      this.context = context;
      this.beanClassLoader = beanClassLoader;
    }

    @Override
    public void process(AnnotationMetadata annotationMetadata, DeferredImportSelector selector) {
      if (selector instanceof AutoConfigurationImportSelector autoConfigSelector) {
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

    private List<String> sortAutoConfigurations(
            Set<String> configurations, AutoConfigurationMetadata autoConfigurationMetadata) {
      return new AutoConfigurationSorter(context.getMetadataReaderFactory(), autoConfigurationMetadata)
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

