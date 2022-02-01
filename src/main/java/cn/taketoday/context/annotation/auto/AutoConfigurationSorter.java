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

package cn.taketoday.context.annotation.auto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.lang.Assert;

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

  private final AutoConfigurationMetadata autoConfigurationMetadata;

  AutoConfigurationSorter(MetadataReaderFactory metadataReaderFactory,
                          AutoConfigurationMetadata autoConfigurationMetadata) {
    Assert.notNull(metadataReaderFactory, "MetadataReaderFactory must not be null");
    this.metadataReaderFactory = metadataReaderFactory;
    this.autoConfigurationMetadata = autoConfigurationMetadata;
  }

  ArrayList<String> getInPriorityOrder(Collection<String> classNames) {
    AutoConfigurationClasses classes = new AutoConfigurationClasses(this.metadataReaderFactory,
            this.autoConfigurationMetadata, classNames);
    ArrayList<String> orderedClassNames = new ArrayList<>(classNames);
    // Initially sort alphabetically
    Collections.sort(orderedClassNames);
    // Then sort by order
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
    LinkedHashSet<String> sorted = new LinkedHashSet<>();
    LinkedHashSet<String> processing = new LinkedHashSet<>();
    while (!toSort.isEmpty()) {
      doSortByAfterAnnotation(classes, toSort, sorted, processing, null);
    }
    sorted.retainAll(classNames);
    return new ArrayList<>(sorted);
  }

  private void doSortByAfterAnnotation(
          AutoConfigurationClasses classes, List<String> toSort,
          Set<String> sorted, Set<String> processing, String current) {
    if (current == null) {
      current = toSort.remove(0);
    }
    processing.add(current);
    for (String after : classes.getClassesRequestedAfter(current)) {
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
      throw new IllegalStateException("AutoConfigure cycle detected between " + current + " and " + after);
    }
  }

  private static class AutoConfigurationClasses {
    private final HashMap<String, AutoConfigurationClass> classes = new HashMap<>();

    AutoConfigurationClasses(
            MetadataReaderFactory metadataReaderFactory,
            AutoConfigurationMetadata autoConfigurationMetadata, Collection<String> classNames) {
      addToClasses(metadataReaderFactory, autoConfigurationMetadata, classNames, true);
    }

    Set<String> getAllNames() {
      return this.classes.keySet();
    }

    private void addToClasses(
            MetadataReaderFactory metadataReaderFactory,
            AutoConfigurationMetadata autoConfigurationMetadata,
            Collection<String> classNames, boolean required) {
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

    LinkedHashSet<String> getClassesRequestedAfter(String className) {
      LinkedHashSet<String> classesRequestedAfter = new LinkedHashSet<>(get(className).getAfter());
      for (Map.Entry<String, AutoConfigurationClass> entry : classes.entrySet()) {
        String name = entry.getKey();
        AutoConfigurationClass autoConfigurationClass = entry.getValue();
        if (autoConfigurationClass.getBefore().contains(className)) {
          classesRequestedAfter.add(name);
        }
      }
      return classesRequestedAfter;
    }

  }

  private static class AutoConfigurationClass {

    private final String className;

    private final MetadataReaderFactory metadataReaderFactory;

    private final AutoConfigurationMetadata autoConfigurationMetadata;

    private volatile AnnotationMetadata annotationMetadata;

    private volatile Set<String> before;

    private volatile Set<String> after;

    AutoConfigurationClass(String className, MetadataReaderFactory metadataReaderFactory,
                           AutoConfigurationMetadata autoConfigurationMetadata) {
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
      if (this.before == null) {
        this.before = wasProcessed() ? this.autoConfigurationMetadata.getSet(this.className,
                "AutoConfigureBefore", Collections.emptySet()) : getAnnotationValue(AutoConfigureBefore.class);
      }
      return this.before;
    }

    Set<String> getAfter() {
      if (this.after == null) {
        this.after = wasProcessed() ? this.autoConfigurationMetadata.getSet(this.className,
                "AutoConfigureAfter", Collections.emptySet()) : getAnnotationValue(AutoConfigureAfter.class);
      }
      return this.after;
    }

    private int getOrder() {
      if (wasProcessed()) {
        return this.autoConfigurationMetadata.getInteger(
                className, "AutoConfigureOrder", AutoConfigureOrder.DEFAULT_ORDER);
      }
      Map<String, Object> attributes = getAnnotationMetadata().getAnnotationAttributes(
              AutoConfigureOrder.class.getName());
      return attributes != null ? (Integer) attributes.get("value") : AutoConfigureOrder.DEFAULT_ORDER;
    }

    private boolean wasProcessed() {
      return this.autoConfigurationMetadata != null
              && this.autoConfigurationMetadata.wasProcessed(this.className);
    }

    private Set<String> getAnnotationValue(Class<?> annotation) {
      Map<String, Object> attributes = getAnnotationMetadata().getAnnotationAttributes(
              annotation.getName(), true);
      if (attributes == null) {
        return Collections.emptySet();
      }
      LinkedHashSet<String> value = new LinkedHashSet<>();
      Collections.addAll(value, (String[]) attributes.get("value"));
      Collections.addAll(value, (String[]) attributes.get("name"));
      return value;
    }

    private AnnotationMetadata getAnnotationMetadata() {
      if (this.annotationMetadata == null) {
        try {
          MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(this.className);
          this.annotationMetadata = metadataReader.getAnnotationMetadata();
        }
        catch (IOException ex) {
          throw new IllegalStateException("Unable to read meta-data for class " + this.className, ex);
        }
      }
      return this.annotationMetadata;
    }

  }

}

