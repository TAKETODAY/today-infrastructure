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

package cn.taketoday.beans.factory.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.PropertyAccessor;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.BeanClassLoadFailedException;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConstructorArgumentValues;
import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.core.io.EncodedResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.DefaultPropertiesPersister;
import cn.taketoday.util.PropertiesPersister;
import cn.taketoday.util.StringUtils;

/**
 * Bean definition reader for a simple properties format.
 *
 * <p>Provides bean definition registration methods for Map/Properties and
 * ResourceBundle. Typically applied to a StandardBeanFactory.
 *
 * <p><b>Example:</b>
 *
 * <pre class="code">
 * employee.(class)=MyClass       // bean is of class MyClass
 * employee.(abstract)=true       // this bean can't be instantiated directly
 * employee.group=Insurance       // real property
 * employee.usesDialUp=false      // real property (potentially overridden)
 *
 * salesrep.(parent)=employee     // derives from "employee" bean definition
 * salesrep.(lazy-init)=true      // lazily initialize this singleton bean
 * salesrep.manager(ref)=tony     // reference to another bean
 * salesrep.department=Sales      // real property
 *
 * techie.(parent)=employee       // derives from "employee" bean definition
 * techie.(scope)=prototype       // bean is a prototype (not a shared instance)
 * techie.manager(ref)=jeff       // reference to another bean
 * techie.department=Engineering  // real property
 * techie.usesDialUp=true         // real property (overriding parent value)
 *
 * ceo.$0(ref)=secretary          // inject 'secretary' bean as 0th constructor arg
 * ceo.$1=1000000                 // inject value '1000000' at 1st constructor arg
 * </pre>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see StandardBeanFactory
 * @since 4.0 2022/3/19 18:33
 */
public class PropertiesBeanDefinitionReader extends AbstractBeanDefinitionReader {

  /**
   * Value of a T/F attribute that represents true.
   * Anything else represents false. Case seNsItive.
   */
  public static final String TRUE_VALUE = "true";

  /**
   * Separator between bean name and property name.
   * We follow normal Java conventions.
   */
  public static final String SEPARATOR = ".";

  /**
   * Special key to distinguish {@code owner.(class)=com.myapp.MyClass}.
   */
  public static final String CLASS_KEY = "(class)";

  /**
   * Special key to distinguish {@code owner.(parent)=parentBeanName}.
   */
  public static final String PARENT_KEY = "(parent)";

  /**
   * Special key to distinguish {@code owner.(scope)=prototype}.
   * Default is "true".
   */
  public static final String SCOPE_KEY = "(scope)";

  /**
   * Special key to distinguish {@code owner.(singleton)=false}.
   * Default is "true".
   */
  public static final String SINGLETON_KEY = "(singleton)";

  /**
   * Special key to distinguish {@code owner.(abstract)=true}
   * Default is "false".
   */
  public static final String ABSTRACT_KEY = "(abstract)";

  /**
   * Special key to distinguish {@code owner.(lazy-init)=true}
   * Default is "false".
   */
  public static final String LAZY_INIT_KEY = "(lazy-init)";

  /**
   * Property suffix for references to other beans in the current
   * BeanFactory: e.g. {@code owner.dog(ref)=fido}.
   * Whether this is a reference to a singleton or a prototype
   * will depend on the definition of the target bean.
   */
  public static final String REF_SUFFIX = "(ref)";

  /**
   * Prefix before values referencing other beans.
   */
  public static final String REF_PREFIX = "*";

  /**
   * Prefix used to denote a constructor argument definition.
   */
  public static final String CONSTRUCTOR_ARG_PREFIX = "$";

  @Nullable
  private String defaultParentBean;

  private PropertiesPersister propertiesPersister = DefaultPropertiesPersister.INSTANCE;

  /**
   * Create new PropertiesBeanDefinitionReader for the given bean factory.
   *
   * @param registry the BeanFactory to load bean definitions into,
   * in the form of a BeanDefinitionRegistry
   */
  public PropertiesBeanDefinitionReader(BeanDefinitionRegistry registry) {
    super(registry);
  }

  /**
   * Set the default parent bean for this bean factory.
   * If a child bean definition handled by this factory provides neither
   * a parent nor a class attribute, this default value gets used.
   * <p>Can be used e.g. for view definition files, to define a parent
   * with a default view class and common attributes for all views.
   * View definitions that define their own parent or carry their own
   * class can still override this.
   * <p>Strictly speaking, the rule that a default parent setting does
   * not apply to a bean definition that carries a class is there for
   * backwards compatibility reasons. It still matches the typical use case.
   */
  public void setDefaultParentBean(@Nullable String defaultParentBean) {
    this.defaultParentBean = defaultParentBean;
  }

  /**
   * Return the default parent bean for this bean factory.
   */
  @Nullable
  public String getDefaultParentBean() {
    return this.defaultParentBean;
  }

  /**
   * Set the PropertiesPersister to use for parsing properties files.
   * The default is ResourcePropertiesPersister.
   *
   * @see DefaultPropertiesPersister#INSTANCE
   */
  public void setPropertiesPersister(@Nullable PropertiesPersister propertiesPersister) {
    this.propertiesPersister =
            (propertiesPersister != null ? propertiesPersister : DefaultPropertiesPersister.INSTANCE);
  }

  /**
   * Return the PropertiesPersister to use for parsing properties files.
   */
  public PropertiesPersister getPropertiesPersister() {
    return this.propertiesPersister;
  }

  /**
   * Load bean definitions from the specified properties file,
   * using all property keys (i.e. not filtering by prefix).
   *
   * @param resource the resource descriptor for the properties file
   * @return the number of bean definitions found
   * @throws BeanDefinitionStoreException in case of loading or parsing errors
   * @see #loadBeanDefinitions(cn.taketoday.core.io.Resource, String)
   */
  @Override
  public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
    return loadBeanDefinitions(new EncodedResource(resource), null);
  }

  /**
   * Load bean definitions from the specified properties file.
   *
   * @param resource the resource descriptor for the properties file
   * @param prefix a filter within the keys in the map: e.g. 'beans.'
   * (can be empty or {@code null})
   * @return the number of bean definitions found
   * @throws BeanDefinitionStoreException in case of loading or parsing errors
   */
  public int loadBeanDefinitions(Resource resource, @Nullable String prefix) throws BeanDefinitionStoreException {
    return loadBeanDefinitions(new EncodedResource(resource), prefix);
  }

  /**
   * Load bean definitions from the specified properties file.
   *
   * @param encodedResource the resource descriptor for the properties file,
   * allowing to specify an encoding to use for parsing the file
   * @return the number of bean definitions found
   * @throws BeanDefinitionStoreException in case of loading or parsing errors
   */
  public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
    return loadBeanDefinitions(encodedResource, null);
  }

  /**
   * Load bean definitions from the specified properties file.
   *
   * @param encodedResource the resource descriptor for the properties file,
   * allowing to specify an encoding to use for parsing the file
   * @param prefix a filter within the keys in the map: e.g. 'beans.'
   * (can be empty or {@code null})
   * @return the number of bean definitions found
   * @throws BeanDefinitionStoreException in case of loading or parsing errors
   */
  public int loadBeanDefinitions(EncodedResource encodedResource, @Nullable String prefix)
          throws BeanDefinitionStoreException {

    if (logger.isTraceEnabled()) {
      logger.trace("Loading properties bean definitions from {}", encodedResource);
    }

    Properties props = new Properties();
    try {
      try (InputStream is = encodedResource.getResource().getInputStream()) {
        if (encodedResource.getEncoding() != null) {
          getPropertiesPersister().load(props, new InputStreamReader(is, encodedResource.getEncoding()));
        }
        else {
          getPropertiesPersister().load(props, is);
        }
      }

      int count = registerBeanDefinitions(props, prefix, encodedResource.getResource().toString());
      if (logger.isDebugEnabled()) {
        logger.debug("Loaded {} bean definitions from {}", count, encodedResource);
      }
      return count;
    }
    catch (IOException ex) {
      throw new BeanDefinitionStoreException("Could not parse properties from " + encodedResource.getResource(), ex);
    }
  }

  /**
   * Register bean definitions contained in a resource bundle,
   * using all property keys (i.e. not filtering by prefix).
   *
   * @param rb the ResourceBundle to load from
   * @return the number of bean definitions found
   * @throws BeanDefinitionStoreException in case of loading or parsing errors
   * @see #registerBeanDefinitions(java.util.ResourceBundle, String)
   */
  public int registerBeanDefinitions(ResourceBundle rb) throws BeanDefinitionStoreException {
    return registerBeanDefinitions(rb, null);
  }

  /**
   * Register bean definitions contained in a ResourceBundle.
   * <p>Similar syntax as for a Map. This method is useful to enable
   * standard Java internationalization support.
   *
   * @param rb the ResourceBundle to load from
   * @param prefix a filter within the keys in the map: e.g. 'beans.'
   * (can be empty or {@code null})
   * @return the number of bean definitions found
   * @throws BeanDefinitionStoreException in case of loading or parsing errors
   */
  public int registerBeanDefinitions(ResourceBundle rb, @Nullable String prefix) throws BeanDefinitionStoreException {
    // Simply create a map and call overloaded method.
    Map<String, Object> map = new HashMap<>();
    Enumeration<String> keys = rb.getKeys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      map.put(key, rb.getObject(key));
    }
    return registerBeanDefinitions(map, prefix);
  }

  /**
   * Register bean definitions contained in a Map, using all property keys (i.e. not
   * filtering by prefix).
   *
   * @param map a map of {@code name} to {@code property} (String or Object). Property
   * values will be strings if coming from a Properties file etc. Property names
   * (keys) <b>must</b> be Strings. Class keys must be Strings.
   * @return the number of bean definitions found
   * @throws BeansException in case of loading or parsing errors
   * @see #registerBeanDefinitions(java.util.Map, String, String)
   */
  public int registerBeanDefinitions(Map<?, ?> map) throws BeansException {
    return registerBeanDefinitions(map, null);
  }

  /**
   * Register bean definitions contained in a Map.
   * Ignore ineligible properties.
   *
   * @param map a map of {@code name} to {@code property} (String or Object). Property
   * values will be strings if coming from a Properties file etc. Property names
   * (keys) <b>must</b> be Strings. Class keys must be Strings.
   * @param prefix a filter within the keys in the map: e.g. 'beans.'
   * (can be empty or {@code null})
   * @return the number of bean definitions found
   * @throws BeansException in case of loading or parsing errors
   */
  public int registerBeanDefinitions(Map<?, ?> map, @Nullable String prefix) throws BeansException {
    return registerBeanDefinitions(map, prefix, "Map " + map);
  }

  /**
   * Register bean definitions contained in a Map.
   * Ignore ineligible properties.
   *
   * @param map a map of {@code name} to {@code property} (String or Object). Property
   * values will be strings if coming from a Properties file etc. Property names
   * (keys) <b>must</b> be Strings. Class keys must be Strings.
   * @param prefix a filter within the keys in the map: e.g. 'beans.'
   * (can be empty or {@code null})
   * @param resourceDescription description of the resource that the
   * Map came from (for logging purposes)
   * @return the number of bean definitions found
   * @throws BeansException in case of loading or parsing errors
   * @see #registerBeanDefinitions(Map, String)
   */
  public int registerBeanDefinitions(Map<?, ?> map, @Nullable String prefix, String resourceDescription)
          throws BeansException {

    if (prefix == null) {
      prefix = "";
    }
    int beanCount = 0;

    for (Object key : map.keySet()) {
      if (!(key instanceof String keyString)) {
        throw new IllegalArgumentException("Illegal key [" + key + "]: only Strings allowed");
      }
      if (keyString.startsWith(prefix)) {
        // Key is of form: prefix<name>.property
        String nameAndProperty = keyString.substring(prefix.length());
        // Find dot before property name, ignoring dots in property keys.
        int sepIdx;
        int propKeyIdx = nameAndProperty.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX);
        if (propKeyIdx != -1) {
          sepIdx = nameAndProperty.lastIndexOf(SEPARATOR, propKeyIdx);
        }
        else {
          sepIdx = nameAndProperty.lastIndexOf(SEPARATOR);
        }
        if (sepIdx != -1) {
          String beanName = nameAndProperty.substring(0, sepIdx);
          if (logger.isTraceEnabled()) {
            logger.trace("Found bean name '{}'", beanName);
          }
          if (!getRegistry().containsBeanDefinition(beanName)) {
            // If we haven't already registered it...
            registerBeanDefinition(beanName, map, prefix + beanName, resourceDescription);
            ++beanCount;
          }
        }
        else {
          // Ignore it: It wasn't a valid bean name and property,
          // although it did start with the required prefix.
          if (logger.isDebugEnabled()) {
            logger.debug("Invalid bean name and property [{}]", nameAndProperty);
          }
        }
      }
    }

    return beanCount;
  }

  /**
   * Get all property values, given a prefix (which will be stripped)
   * and add the bean they define to the factory with the given name.
   *
   * @param beanName name of the bean to define
   * @param map a Map containing string pairs
   * @param prefix prefix of each entry, which will be stripped
   * @param resourceDescription description of the resource that the
   * Map came from (for logging purposes)
   * @throws BeansException if the bean definition could not be parsed or registered
   */
  protected void registerBeanDefinition(String beanName, Map<?, ?> map, String prefix, String resourceDescription)
          throws BeansException {

    String className = null;
    String parent = null;
    String scope = BeanDefinition.SCOPE_SINGLETON;
    boolean isAbstract = false;
    boolean lazyInit = false;

    ConstructorArgumentValues cas = new ConstructorArgumentValues();
    PropertyValues pvs = new PropertyValues();

    String prefixWithSep = prefix + SEPARATOR;
    int beginIndex = prefixWithSep.length();

    for (Map.Entry<?, ?> entry : map.entrySet()) {
      String key = ((String) entry.getKey()).strip();
      if (key.startsWith(prefixWithSep)) {
        String property = key.substring(beginIndex);
        if (CLASS_KEY.equals(property)) {
          className = ((String) entry.getValue()).strip();
        }
        else if (PARENT_KEY.equals(property)) {
          parent = ((String) entry.getValue()).strip();
        }
        else if (ABSTRACT_KEY.equals(property)) {
          String val = ((String) entry.getValue()).strip();
          isAbstract = TRUE_VALUE.equals(val);
        }
        else if (SCOPE_KEY.equals(property)) {
          // Spring 2.0 style
          scope = ((String) entry.getValue()).strip();
        }
        else if (SINGLETON_KEY.equals(property)) {
          // Spring 1.2 style
          String val = ((String) entry.getValue()).strip();
          scope = StringUtils.isEmpty(val) || TRUE_VALUE.equals(val) ?
                  BeanDefinition.SCOPE_SINGLETON : BeanDefinition.SCOPE_PROTOTYPE;
        }
        else if (LAZY_INIT_KEY.equals(property)) {
          String val = ((String) entry.getValue()).strip();
          lazyInit = TRUE_VALUE.equals(val);
        }
        else if (property.startsWith(CONSTRUCTOR_ARG_PREFIX)) {
          if (property.endsWith(REF_SUFFIX)) {
            int index = Integer.parseInt(property, 1, property.length() - REF_SUFFIX.length(), 10);
            cas.addIndexedArgumentValue(index, new RuntimeBeanReference(entry.getValue().toString()));
          }
          else {
            int index = Integer.parseInt(property, 1, property.length(), 10);
            cas.addIndexedArgumentValue(index, readValue(entry));
          }
        }
        else if (property.endsWith(REF_SUFFIX)) {
          // This isn't a real property, but a reference to another prototype
          // Extract property name: property is of form dog(ref)
          property = property.substring(0, property.length() - REF_SUFFIX.length());
          String ref = ((String) entry.getValue()).strip();

          // It doesn't matter if the referenced bean hasn't yet been registered:
          // this will ensure that the reference is resolved at runtime.
          Object val = new RuntimeBeanReference(ref);
          pvs.add(property, val);
        }
        else {
          // It's a normal bean property.
          pvs.add(property, readValue(entry));
        }
      }
    }

    if (logger.isTraceEnabled()) {
      logger.trace("Registering bean definition for bean name '{}' with {}", beanName, pvs);
    }

    // Just use default parent if we're not dealing with the parent itself,
    // and if there's no class name specified. The latter has to happen for
    // backwards compatibility reasons.
    if (parent == null && className == null && !beanName.equals(this.defaultParentBean)) {
      parent = this.defaultParentBean;
    }

    try {
      var bd = BeanDefinitionReaderUtils.createBeanDefinition(parent, className, getBeanClassLoader());
      bd.setScope(scope);
      bd.setAbstract(isAbstract);
      bd.setLazyInit(lazyInit);
      bd.setConstructorArgumentValues(cas);
      bd.setPropertyValues(pvs);
      getRegistry().registerBeanDefinition(beanName, bd);
    }
    catch (ClassNotFoundException ex) {
      throw new BeanClassLoadFailedException(resourceDescription, beanName, className, ex);
    }
    catch (LinkageError err) {
      throw new BeanClassLoadFailedException(resourceDescription, beanName, className, err);
    }
  }

  /**
   * Reads the value of the entry. Correctly interprets bean references for
   * values that are prefixed with an asterisk.
   */
  private Object readValue(Map.Entry<?, ?> entry) {
    Object val = entry.getValue();
    if (val instanceof String strVal) {
      // If it starts with a reference prefix...
      if (strVal.startsWith(REF_PREFIX)) {
        // Expand the reference.
        String targetName = strVal.substring(1);
        if (targetName.startsWith(REF_PREFIX)) {
          // Escaped prefix -> use plain value.
          val = targetName;
        }
        else {
          val = new RuntimeBeanReference(targetName);
        }
      }
    }
    return val;
  }

}

