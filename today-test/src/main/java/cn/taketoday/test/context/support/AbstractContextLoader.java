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
import java.util.List;
import java.util.Set;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.GenericTypeResolver;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.lang.Assert;
import cn.taketoday.test.context.ContextConfigurationAttributes;
import cn.taketoday.test.context.ContextCustomizer;
import cn.taketoday.test.context.ContextLoader;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.SmartContextLoader;
import cn.taketoday.test.context.util.TestContextResourceUtils;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ResourceUtils;

/**
 * Abstract application context loader that provides a basis for all concrete
 * implementations of the {@link ContextLoader} SPI. Provides a
 * <em>Template Method</em> based approach for {@link #processLocations processing}
 * resource locations.
 *
 * <p>As of Spring 3.1, {@code AbstractContextLoader} also provides a basis
 * for all concrete implementations of the {@link SmartContextLoader} SPI. For
 * backwards compatibility with the {@code ContextLoader} SPI,
 * {@link #processContextConfiguration(ContextConfigurationAttributes)} delegates
 * to {@link #processLocations(Class, String...)}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @see #generateDefaultLocations
 * @see #getResourceSuffixes
 * @see #modifyLocations
 * @see #prepareContext
 * @see #customizeContext
 * @since 2.5
 */
public abstract class AbstractContextLoader implements SmartContextLoader {

  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  private static final Log logger = LogFactory.getLog(AbstractContextLoader.class);

  // SmartContextLoader

  /**
   * For backwards compatibility with the {@link ContextLoader} SPI, the
   * default implementation simply delegates to {@link #processLocations(Class, String...)},
   * passing it the {@link ContextConfigurationAttributes#getDeclaringClass()
   * declaring class} and {@link ContextConfigurationAttributes#getLocations()
   * resource locations} retrieved from the supplied
   * {@link ContextConfigurationAttributes configuration attributes}. The
   * processed locations are then
   * {@link ContextConfigurationAttributes#setLocations(String[]) set} in
   * the supplied configuration attributes.
   * <p>Can be overridden in subclasses &mdash; for example, to process
   * annotated classes instead of resource locations.
   *
   * @see #processLocations(Class, String...)
   * @since 3.1
   */
  @Override
  public void processContextConfiguration(ContextConfigurationAttributes configAttributes) {
    String[] processedLocations =
            processLocations(configAttributes.getDeclaringClass(), configAttributes.getLocations());
    configAttributes.setLocations(processedLocations);
  }

  /**
   * Prepare the {@link ConfigurableApplicationContext} created by this
   * {@code SmartContextLoader} <i>before</i> bean definitions are read.
   * <p>The default implementation:
   * <ul>
   * <li>Sets the <em>active bean definition profiles</em> from the supplied
   * {@code MergedContextConfiguration} in the
   * {@link cn.taketoday.core.env.Environment Environment} of the
   * context.</li>
   * <li>Adds {@link PropertySource PropertySources} for all
   * {@linkplain MergedContextConfiguration#getPropertySourceLocations()
   * resource locations} and
   * {@linkplain MergedContextConfiguration#getPropertySourceProperties()
   * inlined properties} from the supplied {@code MergedContextConfiguration}
   * to the {@code Environment} of the context.</li>
   * <li>Determines what (if any) context initializer classes have been supplied
   * via the {@code MergedContextConfiguration} and instantiates and
   * {@linkplain ApplicationContextInitializer#initialize invokes} each with the
   * given application context.
   * <ul>
   * <li>Any {@code ApplicationContextInitializers} implementing
   * {@link cn.taketoday.core.Ordered Ordered} or annotated with {@link
   * cn.taketoday.core.annotation.Order @Order} will be sorted appropriately.</li>
   * </ul>
   * </li>
   * </ul>
   *
   * @param context the newly created application context
   * @param mergedConfig the merged context configuration
   * @see TestPropertySourceUtils#addPropertiesFilesToEnvironment
   * @see TestPropertySourceUtils#addInlinedPropertiesToEnvironment
   * @see ApplicationContextInitializer#initialize(ConfigurableApplicationContext)
   * @see #loadContext(MergedContextConfiguration)
   * @see ConfigurableApplicationContext#setId
   * @since 4.0
   */
  protected void prepareContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
    context.getEnvironment().setActiveProfiles(mergedConfig.getActiveProfiles());
    TestPropertySourceUtils.addPropertiesFilesToEnvironment(context, mergedConfig.getPropertySourceLocations());
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, mergedConfig.getPropertySourceProperties());
    invokeApplicationContextInitializers(context, mergedConfig);
  }

  @SuppressWarnings("unchecked")
  private void invokeApplicationContextInitializers(ConfigurableApplicationContext context,
          MergedContextConfiguration mergedConfig) {

    Set<Class<? extends ApplicationContextInitializer<?>>> initializerClasses =
            mergedConfig.getContextInitializerClasses();
    if (initializerClasses.isEmpty()) {
      // no ApplicationContextInitializers have been declared -> nothing to do
      return;
    }

    List<ApplicationContextInitializer<ConfigurableApplicationContext>> initializerInstances = new ArrayList<>();
    Class<?> contextClass = context.getClass();

    for (Class<? extends ApplicationContextInitializer<?>> initializerClass : initializerClasses) {
      Class<?> initializerContextClass =
              GenericTypeResolver.resolveTypeArgument(initializerClass, ApplicationContextInitializer.class);
      if (initializerContextClass != null && !initializerContextClass.isInstance(context)) {
        throw new ApplicationContextException(String.format(
                "Could not apply context initializer [%s] since its generic parameter [%s] " +
                        "is not assignable from the type of application context used by this " +
                        "context loader: [%s]", initializerClass.getName(), initializerContextClass.getName(),
                contextClass.getName()));
      }
      initializerInstances.add((ApplicationContextInitializer<ConfigurableApplicationContext>) BeanUtils.instantiateClass(initializerClass));
    }

    AnnotationAwareOrderComparator.sort(initializerInstances);
    for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : initializerInstances) {
      initializer.initialize(context);
    }
  }

  /**
   * Customize the {@link ConfigurableApplicationContext} created by this
   * {@code ContextLoader} <em>after</em> bean definitions have been loaded
   * into the context but <em>before</em> the context has been refreshed.
   * <p>The default implementation delegates to all
   * {@link MergedContextConfiguration#getContextCustomizers context customizers}
   * that have been registered with the supplied {@code mergedConfig}.
   *
   * @param context the newly created application context
   * @param mergedConfig the merged context configuration
   * @since 4.0
   */
  protected void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
    for (ContextCustomizer contextCustomizer : mergedConfig.getContextCustomizers()) {
      contextCustomizer.customizeContext(context, mergedConfig);
    }
  }

  // ContextLoader

  /**
   * If the supplied {@code locations} are {@code null} or <em>empty</em>
   * and {@link #isGenerateDefaultLocations()} returns {@code true},
   * default locations will be {@link #generateDefaultLocations(Class)
   * generated} (i.e., detected) for the specified {@link Class class}
   * and the configured {@linkplain #getResourceSuffixes() resource suffixes};
   * otherwise, the supplied {@code locations} will be
   * {@linkplain #modifyLocations modified} if necessary and returned.
   *
   * @param clazz the class with which the locations are associated: to be
   * used when generating default locations
   * @param locations the unmodified locations to use for loading the
   * application context (can be {@code null} or empty)
   * @return a processed array of application context resource locations
   * @see #isGenerateDefaultLocations()
   * @see #generateDefaultLocations(Class)
   * @see #modifyLocations(Class, String...)
   * @see ContextLoader#processLocations(Class, String...)
   * @see #processContextConfiguration(ContextConfigurationAttributes)
   * @since 2.5
   */
  @Override
  public final String[] processLocations(Class<?> clazz, String... locations) {
    return (ObjectUtils.isEmpty(locations) && isGenerateDefaultLocations()) ?
           generateDefaultLocations(clazz) : modifyLocations(clazz, locations);
  }

  /**
   * Generate the default classpath resource locations array based on the
   * supplied class.
   * <p>For example, if the supplied class is {@code com.example.MyTest},
   * the generated locations will contain a single string with a value of
   * {@code "classpath:com/example/MyTest<suffix>"}, where {@code <suffix>}
   * is the value of the first configured
   * {@linkplain #getResourceSuffixes() resource suffix} for which the
   * generated location actually exists in the classpath.
   * <p>As of Spring 3.1, the implementation of this method adheres to the
   * contract defined in the {@link SmartContextLoader} SPI. Specifically,
   * this method will <em>preemptively</em> verify that the generated default
   * location actually exists. If it does not exist, this method will log a
   * warning and return an empty array.
   * <p>Subclasses can override this method to implement a different
   * <em>default location generation</em> strategy.
   *
   * @param clazz the class for which the default locations are to be generated
   * @return an array of default application context resource locations
   * @see #getResourceSuffixes()
   * @since 2.5
   */
  protected String[] generateDefaultLocations(Class<?> clazz) {
    Assert.notNull(clazz, "Class must not be null");

    String[] suffixes = getResourceSuffixes();
    for (String suffix : suffixes) {
      Assert.hasText(suffix, "Resource suffix must not be empty");
      String resourcePath = ClassUtils.convertClassNameToResourcePath(clazz.getName()) + suffix;
      ClassPathResource classPathResource = new ClassPathResource(resourcePath);
      if (classPathResource.exists()) {
        String prefixedResourcePath = ResourceUtils.CLASSPATH_URL_PREFIX + resourcePath;
        if (logger.isInfoEnabled()) {
          logger.info(String.format("Detected default resource location \"%s\" for test class [%s]",
                  prefixedResourcePath, clazz.getName()));
        }
        return new String[] { prefixedResourcePath };
      }
      else if (logger.isDebugEnabled()) {
        logger.debug(String.format("Did not detect default resource location for test class [%s]: " +
                "%s does not exist", clazz.getName(), classPathResource));
      }
    }

    if (logger.isInfoEnabled()) {
      logger.info(String.format("Could not detect default resource locations for test class [%s]: " +
              "no resource found for suffixes %s.", clazz.getName(), ObjectUtils.nullSafeToString(suffixes)));
    }

    return EMPTY_STRING_ARRAY;
  }

  /**
   * Generate a modified version of the supplied locations array and return it.
   * <p>The default implementation simply delegates to
   * {@link TestContextResourceUtils#convertToClasspathResourcePaths}.
   * <p>Subclasses can override this method to implement a different
   * <em>location modification</em> strategy.
   *
   * @param clazz the class with which the locations are associated
   * @param locations the resource locations to be modified
   * @return an array of modified application context resource locations
   * @since 2.5
   */
  protected String[] modifyLocations(Class<?> clazz, String... locations) {
    return TestContextResourceUtils.convertToClasspathResourcePaths(clazz, locations);
  }

  /**
   * Determine whether or not <em>default</em> resource locations should be
   * generated if the {@code locations} provided to
   * {@link #processLocations(Class, String...)} are {@code null} or empty.
   * <p>As of Spring 3.1, the semantics of this method have been overloaded
   * to include detection of either default resource locations or default
   * configuration classes. Consequently, this method can also be used to
   * determine whether or not <em>default</em> configuration classes should be
   * detected if the {@code classes} present in the
   * {@link ContextConfigurationAttributes configuration attributes} supplied
   * to {@link #processContextConfiguration(ContextConfigurationAttributes)}
   * are {@code null} or empty.
   * <p>Can be overridden by subclasses to change the default behavior.
   *
   * @return always {@code true} by default
   * @since 2.5
   */
  protected boolean isGenerateDefaultLocations() {
    return true;
  }

  /**
   * Get the suffixes to append to {@link ApplicationContext} resource locations
   * when detecting default locations.
   * <p>The default implementation simply wraps the value returned by
   * {@link #getResourceSuffix()} in a single-element array, but this
   * can be overridden by subclasses in order to support multiple suffixes.
   *
   * @return the resource suffixes; never {@code null} or empty
   * @see #generateDefaultLocations(Class)
   * @since 4.1
   */
  protected String[] getResourceSuffixes() {
    return new String[] { getResourceSuffix() };
  }

  /**
   * Get the suffix to append to {@link ApplicationContext} resource locations
   * when detecting default locations.
   * <p>Subclasses must provide an implementation of this method that returns
   * a single suffix. Alternatively subclasses may provide a  <em>no-op</em>
   * implementation of this method and override {@link #getResourceSuffixes()}
   * in order to provide multiple custom suffixes.
   *
   * @return the resource suffix; never {@code null} or empty
   * @see #generateDefaultLocations(Class)
   * @see #getResourceSuffixes()
   * @since 2.5
   */
  protected abstract String getResourceSuffix();

}
