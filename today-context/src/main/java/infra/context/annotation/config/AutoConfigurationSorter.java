/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.annotation.config;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.UnaryOperator;

import infra.core.type.AnnotationMetadata;
import infra.core.type.classreading.MetadataReader;
import infra.core.type.classreading.MetadataReaderFactory;
import infra.lang.Assert;

/**
 * Sort {@link EnableAutoConfiguration auto-configuration} classes into priority order by
 * reading {@link AutoConfigureOrder @AutoConfigureOrder},
 * {@link AutoConfigureBefore @AutoConfigureBefore} and
 * {@link AutoConfigureAfter @AutoConfigureAfter} annotations (without loading classes).
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 11:54
 */
class AutoConfigurationSorter {

  private final MetadataReaderFactory metadataReaderFactory;

  @Nullable
  private final AutoConfigurationMetadata autoConfigurationMetadata;

  @Nullable
  private final UnaryOperator<String> replacementMapper;

  AutoConfigurationSorter(MetadataReaderFactory metadataReaderFactory,
          @Nullable AutoConfigurationMetadata autoConfigurationMetadata, @Nullable UnaryOperator<String> replacementMapper) {
    Assert.notNull(metadataReaderFactory, "MetadataReaderFactory required");
    this.replacementMapper = replacementMapper;
    this.metadataReaderFactory = metadataReaderFactory;
    this.autoConfigurationMetadata = autoConfigurationMetadata;
  }

  protected List<String> getInPriorityOrder(Collection<String> classNames) {
    // Initially sort alphabetically
    List<String> alphabeticallyOrderedClassNames = new ArrayList<>(classNames);
    Collections.sort(alphabeticallyOrderedClassNames);
    // Then sort by order
    AutoConfigurationClasses classes = new AutoConfigurationClasses(this.metadataReaderFactory,
            this.autoConfigurationMetadata, alphabeticallyOrderedClassNames);
    ArrayList<String> orderedClassNames = new ArrayList<>(classNames);
    Collections.sort(orderedClassNames);
    orderedClassNames.sort((o1, o2) -> {
      int i1 = classes.get(o1).getOrder();
      int i2 = classes.get(o2).getOrder();
      return Integer.compare(i1, i2);
    });
    // Then respect @AutoConfigureBefore @AutoConfigureAfter
    orderedClassNames = sortByAnnotation(classes, orderedClassNames);
    return orderedClassNames;
  }

  private ArrayList<String> sortByAnnotation(AutoConfigurationClasses classes, List<String> classNames) {
    ArrayList<String> toSort = new ArrayList<>(classNames);
    toSort.addAll(classes.getAllNames());
    Set<String> sorted = new LinkedHashSet<>();
    Set<String> processing = new LinkedHashSet<>();
    while (!toSort.isEmpty()) {
      doSortByAfterAnnotation(classes, toSort, sorted, processing, null);
    }
    sorted.retainAll(classNames);
    return new ArrayList<>(sorted);
  }

  private void doSortByAfterAnnotation(AutoConfigurationClasses classes,
          List<String> toSort, Set<String> sorted, Set<String> processing, @Nullable String current) {
    if (current == null) {
      current = toSort.remove(0);
    }
    processing.add(current);
    TreeSet<String> afters = new TreeSet<>(Comparator.comparing(toSort::indexOf));
    afters.addAll(classes.getClassesRequestedAfter(current));
    for (String after : afters) {
      checkForCycles(processing, current, after);
      if (!sorted.contains(after) && toSort.contains(after)) {
        doSortByAfterAnnotation(classes, toSort, sorted, processing, after);
      }
    }
    processing.remove(current);
    sorted.add(current);
  }

  private void checkForCycles(Set<String> processing, String current, String after) {
    if (processing.contains(after)) {
      throw new IllegalStateException("AutoConfigure cycle detected between %s and %s".formatted(current, after));
    }
  }

  private class AutoConfigurationClasses {

    private final LinkedHashMap<String, AutoConfigurationClass> classes = new LinkedHashMap<>();

    AutoConfigurationClasses(MetadataReaderFactory metadataReaderFactory,
            @Nullable AutoConfigurationMetadata autoConfigurationMetadata, Collection<String> classNames) {
      addToClasses(metadataReaderFactory, autoConfigurationMetadata, classNames, true);
    }

    Set<String> getAllNames() {
      return this.classes.keySet();
    }

    private void addToClasses(MetadataReaderFactory metadataReaderFactory,
            @Nullable AutoConfigurationMetadata autoConfigurationMetadata, Collection<String> classNames, boolean required) {
      for (String className : classNames) {
        if (!this.classes.containsKey(className)) {
          AutoConfigurationClass autoConfigurationClass = new AutoConfigurationClass(className,
                  metadataReaderFactory, autoConfigurationMetadata);
          boolean available = autoConfigurationClass.isAvailable();
          if (required || available) {
            this.classes.put(className, autoConfigurationClass);
          }
          if (available) {
            addToClasses(metadataReaderFactory, autoConfigurationMetadata,
                    autoConfigurationClass.getBefore(), false);
            addToClasses(metadataReaderFactory, autoConfigurationMetadata,
                    autoConfigurationClass.getAfter(), false);
          }
        }
      }
    }

    AutoConfigurationClass get(String className) {
      return this.classes.get(className);
    }

    Set<String> getClassesRequestedAfter(String className) {
      Set<String> classesRequestedAfter = new LinkedHashSet<>(get(className).getAfter());
      this.classes.forEach((name, autoConfigurationClass) -> {
        if (autoConfigurationClass.getBefore().contains(className)) {
          classesRequestedAfter.add(name);
        }
      });
      return classesRequestedAfter;
    }

  }

  private class AutoConfigurationClass {

    private final String className;

    private final MetadataReaderFactory metadataReaderFactory;

    @Nullable
    private final AutoConfigurationMetadata autoConfigurationMetadata;

    @Nullable
    private volatile AnnotationMetadata annotationMetadata;

    @Nullable
    private volatile Set<String> before;

    @Nullable
    private volatile Set<String> after;

    AutoConfigurationClass(String className, MetadataReaderFactory metadataReaderFactory,
            @Nullable AutoConfigurationMetadata autoConfigurationMetadata) {
      this.className = className;
      this.metadataReaderFactory = metadataReaderFactory;
      this.autoConfigurationMetadata = autoConfigurationMetadata;
    }

    boolean isAvailable() {
      try {
        if (!wasProcessed()) {
          getAnnotationMetadata();
        }
        return true;
      }
      catch (Exception ex) {
        return false;
      }
    }

    Set<String> getBefore() {
      Set<String> before = this.before;
      if (before == null) {
        this.before = before = getClassNames("AutoConfigureBefore", AutoConfigureBefore.class);
      }
      return before;
    }

    Set<String> getAfter() {
      Set<String> after = this.after;
      if (after == null) {
        this.after = after = getClassNames("AutoConfigureAfter", AutoConfigureAfter.class);
      }
      return after;
    }

    @SuppressWarnings("NullAway")
    private Set<String> getClassNames(String metadataKey, Class<? extends Annotation> annotation) {
      Set<String> annotationValue = wasProcessed()
              ? this.autoConfigurationMetadata.getSet(this.className, metadataKey, Collections.emptySet())
              : getAnnotationValue(annotation);
      return applyReplacements(annotationValue);
    }

    private Set<String> applyReplacements(Set<String> values) {
      if (AutoConfigurationSorter.this.replacementMapper == null) {
        return values;
      }
      Set<String> replaced = new LinkedHashSet<>(values);
      for (String value : values) {
        replaced.add(AutoConfigurationSorter.this.replacementMapper.apply(value));
      }
      return replaced;
    }

    @SuppressWarnings("NullAway")
    private int getOrder() {
      if (wasProcessed()) {
        return this.autoConfigurationMetadata.getInteger(this.className, "AutoConfigureOrder",
                AutoConfigureOrder.DEFAULT_ORDER);
      }
      Map<String, Object> attributes = getAnnotationMetadata().getAnnotationAttributes(AutoConfigureOrder.class);
      return (attributes != null) ? (Integer) attributes.get("value") : AutoConfigureOrder.DEFAULT_ORDER;
    }

    private boolean wasProcessed() {
      return (this.autoConfigurationMetadata != null
              && this.autoConfigurationMetadata.wasProcessed(this.className));
    }

    private Set<String> getAnnotationValue(Class<? extends Annotation> annotation) {
      Map<String, Object> attributes = getAnnotationMetadata().getAnnotationAttributes(annotation, true);
      if (attributes == null) {
        return Collections.emptySet();
      }
      LinkedHashSet<String> value = new LinkedHashSet<>();
      Collections.addAll(value, (String[]) attributes.get("value"));
      Collections.addAll(value, (String[]) attributes.get("name"));
      return value;
    }

    private AnnotationMetadata getAnnotationMetadata() {
      AnnotationMetadata annotationMetadata = this.annotationMetadata;
      if (annotationMetadata == null) {
        try {
          MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(this.className);
          this.annotationMetadata = annotationMetadata = metadataReader.getAnnotationMetadata();
        }
        catch (IOException ex) {
          throw new IllegalStateException("Unable to read meta-data for class " + this.className, ex);
        }
      }
      return annotationMetadata;
    }

  }

}

