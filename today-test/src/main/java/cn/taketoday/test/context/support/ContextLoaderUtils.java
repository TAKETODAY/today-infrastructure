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

package cn.taketoday.test.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.lang.Assert;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextConfigurationAttributes;
import cn.taketoday.test.context.ContextHierarchy;
import cn.taketoday.test.context.SmartContextLoader;
import cn.taketoday.test.context.TestContextAnnotationUtils.AnnotationDescriptor;
import cn.taketoday.test.context.TestContextAnnotationUtils.UntypedAnnotationDescriptor;
import cn.taketoday.util.StringUtils;

import static cn.taketoday.core.annotation.AnnotationUtils.getAnnotation;
import static cn.taketoday.core.annotation.AnnotationUtils.isAnnotationDeclaredLocally;
import static cn.taketoday.test.context.TestContextAnnotationUtils.findAnnotationDescriptor;
import static cn.taketoday.test.context.TestContextAnnotationUtils.findAnnotationDescriptorForTypes;

/**
 * Utility methods for resolving {@link ContextConfigurationAttributes} from the
 * {@link ContextConfiguration @ContextConfiguration} and
 * {@link ContextHierarchy @ContextHierarchy} annotations for use with
 * {@link SmartContextLoader SmartContextLoaders}.
 *
 * @author Sam Brannen
 * @see SmartContextLoader
 * @see ContextConfigurationAttributes
 * @see ContextConfiguration
 * @see ContextHierarchy
 * @since 3.1
 */
abstract class ContextLoaderUtils {

  static final String GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX = "ContextHierarchyLevel#";

  private static final Log logger = LogFactory.getLog(ContextLoaderUtils.class);

  /**
   * Resolve the list of lists of {@linkplain ContextConfigurationAttributes context
   * configuration attributes} for the supplied {@linkplain Class test class} and its
   * superclasses, taking into account context hierarchies declared via
   * {@link ContextHierarchy @ContextHierarchy} and
   * {@link ContextConfiguration @ContextConfiguration}.
   * <p>The outer list represents a top-down ordering of context configuration
   * attributes, where each element in the list represents the context configuration
   * declared on a given test class in the class hierarchy. Each nested list
   * contains the context configuration attributes declared either via a single
   * instance of {@code @ContextConfiguration} on the particular class or via
   * multiple instances of {@code @ContextConfiguration} declared within a
   * single {@code @ContextHierarchy} instance on the particular class.
   * Furthermore, each nested list maintains the order in which
   * {@code @ContextConfiguration} instances are declared.
   * <p>Note that the {@link ContextConfiguration#inheritLocations inheritLocations} and
   * {@link ContextConfiguration#inheritInitializers() inheritInitializers} flags of
   * {@link ContextConfiguration @ContextConfiguration} will <strong>not</strong>
   * be taken into consideration. If these flags need to be honored, that must be
   * handled manually when traversing the nested lists returned by this method.
   *
   * @param testClass the class for which to resolve the context hierarchy attributes
   * (must not be {@code null})
   * @return the list of lists of configuration attributes for the specified class;
   * never {@code null}
   * @throws IllegalArgumentException if the supplied class is {@code null}; or if
   * neither {@code @ContextConfiguration} nor {@code @ContextHierarchy} is
   * <em>present</em> on the supplied class
   * @throws IllegalStateException if a test class or composed annotation
   * in the class hierarchy declares both {@code @ContextConfiguration} and
   * {@code @ContextHierarchy} as top-level annotations.
   * @see #buildContextHierarchyMap(Class)
   * @see #resolveContextConfigurationAttributes(Class)
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  static List<List<ContextConfigurationAttributes>> resolveContextHierarchyAttributes(Class<?> testClass) {
    Assert.notNull(testClass, "Class must not be null");

    Class<ContextConfiguration> contextConfigType = ContextConfiguration.class;
    Class<ContextHierarchy> contextHierarchyType = ContextHierarchy.class;
    List<List<ContextConfigurationAttributes>> hierarchyAttributes = new ArrayList<>();

    UntypedAnnotationDescriptor desc =
            findAnnotationDescriptorForTypes(testClass, contextConfigType, contextHierarchyType);
    Assert.notNull(desc, () -> String.format(
            "Could not find an 'annotation declaring class' for annotation type [%s] or [%s] and test class [%s]",
            contextConfigType.getName(), contextHierarchyType.getName(), testClass.getName()));

    while (desc != null) {
      Class<?> rootDeclaringClass = desc.getRootDeclaringClass();
      Class<?> declaringClass = desc.getDeclaringClass();

      boolean contextConfigDeclaredLocally = isAnnotationDeclaredLocally(contextConfigType, declaringClass);
      boolean contextHierarchyDeclaredLocally = isAnnotationDeclaredLocally(contextHierarchyType, declaringClass);

      if (contextConfigDeclaredLocally && contextHierarchyDeclaredLocally) {
        String msg = String.format("Class [%s] has been configured with both @ContextConfiguration " +
                "and @ContextHierarchy. Only one of these annotations may be declared on a test class " +
                "or composed annotation.", declaringClass.getName());
        logger.error(msg);
        throw new IllegalStateException(msg);
      }

      List<ContextConfigurationAttributes> configAttributesList = new ArrayList<>();

      if (contextConfigDeclaredLocally) {
        ContextConfiguration contextConfiguration = (ContextConfiguration) desc.getAnnotation();
        convertContextConfigToConfigAttributesAndAddToList(
                contextConfiguration, rootDeclaringClass, configAttributesList);
      }
      else if (contextHierarchyDeclaredLocally) {
        ContextHierarchy contextHierarchy = getAnnotation(declaringClass, contextHierarchyType);
        if (contextHierarchy != null) {
          for (ContextConfiguration contextConfiguration : contextHierarchy.value()) {
            convertContextConfigToConfigAttributesAndAddToList(
                    contextConfiguration, rootDeclaringClass, configAttributesList);
          }
        }
      }
      else {
        // This should theoretically never happen...
        String msg = String.format("Test class [%s] has been configured with neither @ContextConfiguration " +
                "nor @ContextHierarchy as a class-level annotation.", rootDeclaringClass.getName());
        logger.error(msg);
        throw new IllegalStateException(msg);
      }

      hierarchyAttributes.add(0, configAttributesList);

      desc = desc.next();
    }

    return hierarchyAttributes;
  }

  /**
   * Build a <em>context hierarchy map</em> for the supplied {@linkplain Class
   * test class} and its superclasses, taking into account context hierarchies
   * declared via {@link ContextHierarchy @ContextHierarchy} and
   * {@link ContextConfiguration @ContextConfiguration}.
   * <p>Each value in the map represents the consolidated list of {@linkplain
   * ContextConfigurationAttributes context configuration attributes} for a
   * given level in the context hierarchy (potentially across the test class
   * hierarchy), keyed by the {@link ContextConfiguration#name() name} of the
   * context hierarchy level.
   * <p>If a given level in the context hierarchy does not have an explicit
   * name (i.e., configured via {@link ContextConfiguration#name}), a name will
   * be generated for that hierarchy level by appending the numerical level to
   * the {@link #GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX}.
   *
   * @param testClass the class for which to resolve the context hierarchy map
   * (must not be {@code null})
   * @return a map of context configuration attributes for the context hierarchy,
   * keyed by context hierarchy level name; never {@code null}
   * @throws IllegalArgumentException if the lists of context configuration
   * attributes for each level in the {@code @ContextHierarchy} do not define
   * unique context configuration within the overall hierarchy.
   * @see #resolveContextHierarchyAttributes(Class)
   * @since 4.0
   */
  static Map<String, List<ContextConfigurationAttributes>> buildContextHierarchyMap(Class<?> testClass) {
    Map<String, List<ContextConfigurationAttributes>> map = new LinkedHashMap<>();
    int hierarchyLevel = 1;

    for (List<ContextConfigurationAttributes> configAttributesList : resolveContextHierarchyAttributes(testClass)) {
      for (ContextConfigurationAttributes configAttributes : configAttributesList) {
        String name = configAttributes.getName();

        // Assign a generated name?
        if (!StringUtils.hasText(name)) {
          name = GENERATED_CONTEXT_HIERARCHY_LEVEL_PREFIX + hierarchyLevel;
        }

        // Encountered a new context hierarchy level?
        if (!map.containsKey(name)) {
          hierarchyLevel++;
          map.put(name, new ArrayList<>());
        }

        map.get(name).add(configAttributes);
      }
    }

    // Check for uniqueness
    Set<List<ContextConfigurationAttributes>> set = new HashSet<>(map.values());
    if (set.size() != map.size()) {
      String msg = String.format("The @ContextConfiguration elements configured via @ContextHierarchy in " +
                      "test class [%s] and its superclasses must define unique contexts per hierarchy level.",
              testClass.getName());
      logger.error(msg);
      throw new IllegalStateException(msg);
    }

    return map;
  }

  /**
   * Resolve the list of {@linkplain ContextConfigurationAttributes context
   * configuration attributes} for the supplied {@linkplain Class test class} and its
   * superclasses.
   * <p>Note that the {@link ContextConfiguration#inheritLocations inheritLocations} and
   * {@link ContextConfiguration#inheritInitializers() inheritInitializers} flags of
   * {@link ContextConfiguration @ContextConfiguration} will <strong>not</strong>
   * be taken into consideration. If these flags need to be honored, that must be
   * handled manually when traversing the list returned by this method.
   *
   * @param testClass the class for which to resolve the configuration attributes
   * (must not be {@code null})
   * @return the list of configuration attributes for the specified class, ordered
   * <em>bottom-up</em> (i.e., as if we were traversing up the class hierarchy);
   * never {@code null}
   * @throws IllegalArgumentException if the supplied class is {@code null} or if
   * {@code @ContextConfiguration} is not <em>present</em> on the supplied class
   */
  static List<ContextConfigurationAttributes> resolveContextConfigurationAttributes(Class<?> testClass) {
    Assert.notNull(testClass, "Class must not be null");

    Class<ContextConfiguration> annotationType = ContextConfiguration.class;
    AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(testClass, annotationType);
    Assert.notNull(descriptor, () -> String.format(
            "Could not find an 'annotation declaring class' for annotation type [%s] and class [%s]",
            annotationType.getName(), testClass.getName()));

    List<ContextConfigurationAttributes> attributesList = new ArrayList<>();
    ContextConfiguration previousAnnotation = null;
    Class<?> previousDeclaringClass = null;
    while (descriptor != null) {
      ContextConfiguration currentAnnotation = descriptor.getAnnotation();
      // Don't ignore duplicate @ContextConfiguration declaration without resources,
      // because the ContextLoader will likely detect default resources specific to the
      // annotated class.
      if (currentAnnotation.equals(previousAnnotation) && hasResources(currentAnnotation)) {
        if (logger.isDebugEnabled()) {
          logger.debug(String.format("Ignoring duplicate %s declaration on [%s], "
                          + "since it is also declared on [%s].", currentAnnotation,
                  previousDeclaringClass.getName(), descriptor.getRootDeclaringClass().getName()));
        }
      }
      else {
        convertContextConfigToConfigAttributesAndAddToList(currentAnnotation,
                descriptor.getRootDeclaringClass(), attributesList);
      }
      previousAnnotation = currentAnnotation;
      previousDeclaringClass = descriptor.getRootDeclaringClass();
      descriptor = descriptor.next();
    }
    return attributesList;
  }

  private static boolean hasResources(ContextConfiguration contextConfiguration) {
    return (contextConfiguration.locations().length > 0 || contextConfiguration.classes().length > 0);
  }

  /**
   * Convenience method for creating a {@link ContextConfigurationAttributes}
   * instance from the supplied {@link ContextConfiguration} annotation and
   * declaring class and then adding the attributes to the supplied list.
   */
  private static void convertContextConfigToConfigAttributesAndAddToList(ContextConfiguration contextConfiguration,
          Class<?> declaringClass, List<ContextConfigurationAttributes> attributesList) {

    if (logger.isTraceEnabled()) {
      logger.trace(String.format("Retrieved @ContextConfiguration [%s] for declaring class [%s].",
              contextConfiguration, declaringClass.getName()));
    }
    ContextConfigurationAttributes attributes =
            new ContextConfigurationAttributes(declaringClass, contextConfiguration);
    if (logger.isTraceEnabled()) {
      logger.trace("Resolved context configuration attributes: " + attributes);
    }
    attributesList.add(attributes);
  }

}
