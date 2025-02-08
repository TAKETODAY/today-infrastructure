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
package infra.classify;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A {@link Classifier} for a parameterised object type based on a map. Classifies objects
 * according to their inheritance relation with the supplied type map. If the object to be
 * classified is one of the keys of the provided map, or is a subclass of one of the keys,
 * then the map entry value for that key is returned. Otherwise returns the default value
 * which is null by default.
 *
 * @param <T> the type of the thing to classify
 * @param <C> the output of the classifier
 * @author Dave Syer
 * @author Gary Russell
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
@SuppressWarnings("serial")
public class SubclassClassifier<T, C> implements Classifier<T, C> {

  private C defaultValue;

  private ConcurrentMap<Class<? extends T>, C> classified;

  /**
   * Create a {@link SubclassClassifier} with null default value.
   */
  public SubclassClassifier() {
    this(null);
  }

  /**
   * Create a {@link SubclassClassifier} with supplied default value.
   *
   * @param defaultValue the default value
   */
  public SubclassClassifier(C defaultValue) {
    this(new HashMap<>(), defaultValue);
  }

  /**
   * Create a {@link SubclassClassifier} with supplied default value.
   *
   * @param defaultValue the default value
   * @param typeMap the map of types
   */
  public SubclassClassifier(Map<Class<? extends T>, C> typeMap, C defaultValue) {
    super();
    this.classified = new ConcurrentHashMap<>(typeMap);
    this.defaultValue = defaultValue;
  }

  /**
   * Public setter for the default value for mapping keys that are not found in the map
   * (or their subclasses). Defaults to false.
   *
   * @param defaultValue the default value to set
   */
  public void setDefaultValue(C defaultValue) {
    this.defaultValue = defaultValue;
  }

  /**
   * Set the classifications up as a map. The keys are types and these will be mapped
   * along with all their subclasses to the corresponding value. The most specific types
   * will match first.
   *
   * @param map a map from type to class
   */
  public void setTypeMap(Map<Class<? extends T>, C> map) {
    this.classified = new ConcurrentHashMap<>(map);
  }

  /**
   * The keys is the type and this will be mapped along with all subclasses to the
   * corresponding value. The most specific types will match first.
   *
   * @param type the type of the input object
   * @param target the target value for all such types
   */
  public void add(Class<? extends T> type, C target) {
    this.classified.put(type, target);
  }

  /**
   * Return the value from the type map whose key is the class of the given Throwable,
   * or its nearest ancestor if a subclass.
   *
   * @param classifiable the classifiable thing
   * @return C the classified value
   */
  @Override
  public C classify(T classifiable) {

    if (classifiable == null) {
      return this.defaultValue;
    }

    @SuppressWarnings("unchecked")
    Class<? extends T> exceptionClass = (Class<? extends T>) classifiable.getClass();
    if (this.classified.containsKey(exceptionClass)) {
      return this.classified.get(exceptionClass);
    }

    // check for subclasses
    C value = null;
    for (Class<?> cls = exceptionClass; !cls.equals(Object.class) && value == null; cls = cls.getSuperclass()) {
      value = this.classified.get(cls);
    }

    // check for interfaces subclasses
    if (value == null) {
      for (Class<?> cls = exceptionClass; !cls.equals(Object.class) && value == null; cls = cls.getSuperclass()) {
        for (Class<?> ifc : cls.getInterfaces()) {
          value = this.classified.get(ifc);
          if (value != null) {
            break;
          }
        }
      }
    }

    // ConcurrentHashMap doesn't allow nulls
    if (value != null) {
      this.classified.put(exceptionClass, value);
    }

    if (value == null) {
      value = this.defaultValue;
    }

    return value;
  }

  /**
   * Return the default value supplied in the constructor (default false).
   *
   * @return C the default value
   */
  public final C getDefault() {
    return this.defaultValue;
  }

  protected Map<Class<? extends T>, C> getClassified() {
    return this.classified;
  }

}
