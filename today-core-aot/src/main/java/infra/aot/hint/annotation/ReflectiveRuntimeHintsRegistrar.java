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

package infra.aot.hint.annotation;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import infra.aot.hint.ReflectionHints;
import infra.aot.hint.RuntimeHints;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.util.ClassUtils;
import infra.util.ReflectionUtils;

/**
 * Process {@link Reflective @Reflective} annotated elements.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class ReflectiveRuntimeHintsRegistrar {

  private final Map<Class<?>, ReflectiveProcessor> processors = new HashMap<>();

  /**
   * Register the relevant runtime hints for elements that are annotated with
   * {@link Reflective}.
   *
   * @param runtimeHints the runtime hints instance to use
   * @param types the types to process
   */
  public void registerRuntimeHints(RuntimeHints runtimeHints, Class<?>... types) {
    Set<Entry> entries = new HashSet<>();
    for (Class<?> type : types) {
      processType(entries, type);
      for (Class<?> implementedInterface : ClassUtils.getAllInterfacesForClass(type)) {
        processType(entries, implementedInterface);
      }
    }
    entries.forEach(entry -> {
      AnnotatedElement element = entry.element();
      entry.processor().registerReflectionHints(runtimeHints.reflection(), element);
    });
  }

  /**
   * Specify if the given {@code type} is a valid candidate.
   *
   * @param type the type to inspect
   * @return {@code true} if the type uses {@link Reflective} in a way that
   * is supported by this registrar
   * @since 5.0
   */
  public boolean isCandidate(Class<?> type) {
    if (isReflective(type)) {
      return true;
    }
    AtomicBoolean candidate = new AtomicBoolean(false);
    doWithReflectiveConstructors(type, constructor -> candidate.set(true));
    if (!candidate.get()) {
      ReflectionUtils.doWithFields(type, field -> candidate.set(true), this::isReflective);
    }
    if (!candidate.get()) {
      ReflectionUtils.doWithMethods(type, method -> candidate.set(true), this::isReflective);
    }
    return candidate.get();
  }

  private void processType(Set<Entry> entries, Class<?> typeToProcess) {
    if (isReflective(typeToProcess)) {
      entries.add(createEntry(typeToProcess));
    }
    doWithReflectiveConstructors(typeToProcess, constructor -> entries.add(createEntry(constructor)));
    ReflectionUtils.doWithFields(typeToProcess, field -> entries.add(createEntry(field)), this::isReflective);
    ReflectionUtils.doWithMethods(typeToProcess, method -> entries.add(createEntry(method)), this::isReflective);
  }

  private void doWithReflectiveConstructors(Class<?> typeToProcess, Consumer<Constructor<?>> consumer) {
    for (Constructor<?> constructor : typeToProcess.getDeclaredConstructors()) {
      if (isReflective(constructor)) {
        consumer.accept(constructor);
      }
    }
  }

  private boolean isReflective(AnnotatedElement element) {
    return MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY).isPresent(Reflective.class);
  }

  private Entry createEntry(AnnotatedElement element) {
    List<ReflectiveProcessor> processors = MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY)
            .stream(Reflective.class)
            .map(annotation -> annotation.getClassArray("value"))
            .flatMap(Arrays::stream)
            .distinct()
            .map(processorClass -> this.processors.computeIfAbsent(processorClass, this::instantiateClass))
            .toList();
    ReflectiveProcessor processorToUse = (processors.size() == 1 ? processors.get(0) :
            new DelegatingReflectiveProcessor(processors));
    return new Entry(element, processorToUse);
  }

  private ReflectiveProcessor instantiateClass(Class<?> type) {
    try {
      return (ReflectiveProcessor) ReflectionUtils.accessibleConstructor(type).newInstance();
    }
    catch (Exception ex) {
      throw new IllegalStateException("Failed to instantiate " + type, ex);
    }
  }

  private static class DelegatingReflectiveProcessor implements ReflectiveProcessor {

    private final Iterable<ReflectiveProcessor> processors;

    DelegatingReflectiveProcessor(Iterable<ReflectiveProcessor> processors) {
      this.processors = processors;
    }

    @Override
    public void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
      this.processors.forEach(processor -> processor.registerReflectionHints(hints, element));
    }

  }

  private record Entry(AnnotatedElement element, ReflectiveProcessor processor) {
  }

}
