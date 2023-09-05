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

package cn.taketoday.framework.jackson;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.ClassPathScanningCandidateComponentProvider;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.type.filter.AnnotationTypeFilter;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Provide the mapping of json mixin class to consider.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/29 21:30
 */
public final class JsonMixinModuleEntries {

  private final Map<Object, Object> entries;

  private JsonMixinModuleEntries(Builder builder) {
    this.entries = new LinkedHashMap<>(builder.entries);
  }

  /**
   * Create an instance using the specified {@link Builder}.
   *
   * @param mixins a consumer of the builder
   * @return an instance with the state of the customized builder.
   */
  public static JsonMixinModuleEntries create(Consumer<Builder> mixins) {
    Builder builder = new Builder();
    mixins.accept(builder);
    return builder.build();
  }

  /**
   * Scan the classpath for {@link JsonMixin @JsonMixin} in the specified
   * {@code basePackages}.
   *
   * @param context the application context to use
   * @param basePackages the base packages to consider
   * @return an instance with the result of the scanning
   */
  public static JsonMixinModuleEntries scan(ApplicationContext context, Collection<String> basePackages) {
    return JsonMixinModuleEntries.create(builder -> {
      if (ObjectUtils.isEmpty(basePackages)) {
        return;
      }
      var scanner = new ClassPathScanningCandidateComponentProvider();
      scanner.setEnvironment(context.getEnvironment());
      scanner.setResourceLoader(context);
      scanner.addIncludeFilter(new AnnotationTypeFilter(JsonMixin.class));
      scanner.setCandidateComponentPredicate(annotationMetadata -> true);
      for (String basePackage : basePackages) {
        if (StringUtils.hasText(basePackage)) {
          for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
            Class<?> mixinClass = ClassUtils.resolveClassName(
                    candidate.getBeanClassName(), context.getClassLoader());
            registerMixinClass(builder, mixinClass);
          }
        }
      }
    });
  }

  private static void registerMixinClass(Builder builder, Class<?> mixinClass) {
    var annotation = MergedAnnotations.from(mixinClass,
            MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).get(JsonMixin.class);
    for (Class<?> targetType : annotation.getClassArray("type")) {
      builder.and(targetType, mixinClass);
    }

  }

  /**
   * Perform an action on each entry defined by this instance. If a class needs to be
   * resolved from its class name, the specified {@link ClassLoader} is used.
   *
   * @param classLoader the classloader to use to resolve class name if necessary
   * @param action the action to invoke on each type to mixin class entry
   */
  public void doWithEntry(ClassLoader classLoader, BiConsumer<Class<?>, Class<?>> action) {
    for (Map.Entry<Object, Object> entry : entries.entrySet()) {
      Object type = entry.getKey();
      Object mixin = entry.getValue();
      action.accept(resolveClassNameIfNecessary(type, classLoader),
              resolveClassNameIfNecessary(mixin, classLoader));
    }
  }

  private Class<?> resolveClassNameIfNecessary(Object type, ClassLoader classLoader) {
    return type instanceof Class<?> clazz
           ? clazz : ClassUtils.resolveClassName((String) type, classLoader);
  }

  /**
   * Builder for {@link JsonMixinModuleEntries}.
   */
  public static class Builder {

    private final LinkedHashMap<Object, Object> entries;

    Builder() {
      this.entries = new LinkedHashMap<>();
    }

    /**
     * Add a mapping for the specified class names.
     *
     * @param typeClassName the type class name
     * @param mixinClassName the mixin class name
     * @return {@code this}, to facilitate method chaining
     */
    public Builder and(String typeClassName, String mixinClassName) {
      this.entries.put(typeClassName, mixinClassName);
      return this;
    }

    /**
     * Add a mapping for the specified classes.
     *
     * @param type the type class
     * @param mixinClass the mixin class
     * @return {@code this}, to facilitate method chaining
     */
    public Builder and(Class<?> type, Class<?> mixinClass) {
      this.entries.put(type, mixinClass);
      return this;
    }

    JsonMixinModuleEntries build() {
      return new JsonMixinModuleEntries(this);
    }

  }

}

