package cn.taketoday.beans.factory.xml;

import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.BeanNameGenerator;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Nullable;

/**
 * Simple interface for bean definition readers.
 * Specifies load methods with Resource and String location parameters.
 *
 * <p>Concrete bean definition readers can of course add additional
 * load and register methods for bean definitions, specific to
 * their bean definition format.
 *
 * <p>Note that a bean definition reader does not have to implement
 * this interface. It only serves as suggestion for bean definition
 * readers that want to follow standard naming conventions.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see org.springframework.core.io.Resource
 * @since 4.0 2021/12/15 13:51
 */
public interface BeanDefinitionReader {

  /**
   * Return the bean factory to register the bean definitions with.
   * <p>The factory is exposed through the BeanDefinitionRegistry interface,
   * encapsulating the methods that are relevant for bean definition handling.
   */
  BeanDefinitionRegistry getRegistry();

  /**
   * Return the resource loader to use for resource locations.
   * Can be checked for the <b>ResourcePatternResolver</b> interface and cast
   * accordingly, for loading multiple resources for a given resource pattern.
   * <p>A {@code null} return value suggests that absolute resource loading
   * is not available for this bean definition reader.
   * <p>This is mainly meant to be used for importing further resources
   * from within a bean definition resource, for example via the "import"
   * tag in XML bean definitions. It is recommended, however, to apply
   * such imports relative to the defining resource; only explicit full
   * resource locations will trigger absolute resource loading.
   * <p>There is also a {@code loadBeanDefinitions(String)} method available,
   * for loading bean definitions from a resource location (or location pattern).
   * This is a convenience to avoid explicit ResourceLoader handling.
   *
   * @see #loadBeanDefinitions(String)
   */
  @Nullable
  ResourceLoader getResourceLoader();

  /**
   * Return the class loader to use for bean classes.
   * <p>{@code null} suggests to not load bean classes eagerly
   * but rather to just register bean definitions with class names,
   * with the corresponding Classes to be resolved later (or never).
   */
  @Nullable
  ClassLoader getBeanClassLoader();

  /**
   * Return the BeanNameGenerator to use for anonymous beans
   * (without explicit bean name specified).
   */
  BeanNameGenerator getBeanNameGenerator();

  /**
   * Load bean definitions from the specified resource.
   *
   * @param resource the resource descriptor
   * @return the number of bean definitions found
   * @throws BeanDefinitionStoreException in case of loading or parsing errors
   */
  int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException;

  /**
   * Load bean definitions from the specified resources.
   *
   * @param resources the resource descriptors
   * @return the number of bean definitions found
   * @throws BeanDefinitionStoreException in case of loading or parsing errors
   */
  int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException;

  /**
   * Load bean definitions from the specified resource location.
   * <p>The location can also be a location pattern, provided that the
   * ResourceLoader of this bean definition reader is a ResourcePatternResolver.
   *
   * @param location the resource location, to be loaded with the ResourceLoader
   * (or ResourcePatternResolver) of this bean definition reader
   * @return the number of bean definitions found
   * @throws BeanDefinitionStoreException in case of loading or parsing errors
   * @see #getResourceLoader()
   * @see #loadBeanDefinitions(org.springframework.core.io.Resource)
   * @see #loadBeanDefinitions(org.springframework.core.io.Resource[])
   */
  int loadBeanDefinitions(String location) throws BeanDefinitionStoreException;

  /**
   * Load bean definitions from the specified resource locations.
   *
   * @param locations the resource locations, to be loaded with the ResourceLoader
   * (or ResourcePatternResolver) of this bean definition reader
   * @return the number of bean definitions found
   * @throws BeanDefinitionStoreException in case of loading or parsing errors
   */
  int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException;

}
