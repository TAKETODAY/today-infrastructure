package cn.taketoday.jdbc.reflection;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.reflect.ConstructorAccessor;
import cn.taketoday.context.reflect.PropertyAccessor;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.Mappings;
import cn.taketoday.context.utils.ReflectionUtils;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.jdbc.annotation.Column;
import cn.taketoday.jdbc.utils.UnderscoreToCamelCase;

/**
 * Stores metadata for a POJO.
 *
 * @author TODAY
 */
public class BeanMetadata {

  private static final Cache caseSensitiveTrue = new Cache();
  private static final Cache caseSensitiveFalse = new Cache();

  private final Map<String, String> columnMappings;
  private final Map<String, BeanProperty> beanProperties;

  private final Class<?> beanClass;
  private ConstructorAccessor constructor;

  private final boolean caseSensitive;
  public final boolean throwOnMappingFailure;
  private final boolean autoDeriveColumnNames;

  public BeanMetadata(
          Class<?> clazz,
          boolean caseSensitive,
          boolean autoDeriveColumnNames,
          Map<String, String> columnMappings,
          boolean throwOnMappingError
  ) {
    this.beanClass = clazz;
    this.caseSensitive = caseSensitive;
    this.autoDeriveColumnNames = autoDeriveColumnNames;
    this.columnMappings = columnMappings == null ? Collections.emptyMap() : columnMappings;
    this.beanProperties = (caseSensitive ? caseSensitiveTrue : caseSensitiveFalse).get(beanClass, this);
    this.throwOnMappingFailure = throwOnMappingError;
  }

  public Map<String, String> getColumnMappings() {
    return columnMappings;
  }

  public Class<?> getType() {
    return this.beanClass;
  }

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public boolean isAutoDeriveColumnNames() {
    return autoDeriveColumnNames;
  }

  public ConstructorAccessor getConstructor() {
    if (constructor == null) {
      this.constructor = ReflectionUtils.newConstructorAccessor(beanClass);
    }
    return constructor;
  }

  public Object newInstance() {
    return getConstructor().newInstance();
  }

  public PropertyAccessor getPropertyAccessor(String propertyName) {
    return getBeanProperty(propertyName).getPropertyAccessor();
  }

  public BeanProperty getBeanProperty(final String propertyName) {
    String name = this.caseSensitive ? propertyName : propertyName.toLowerCase();

    if (this.columnMappings.containsKey(name)) {
      name = this.columnMappings.get(name);
    }

    if (autoDeriveColumnNames) {
      name = UnderscoreToCamelCase.convert(name);
      if (!this.caseSensitive) {
        name = name.toLowerCase();
      }
    }

    return beanProperties.get(name);
  }

  public Map<String, BeanProperty> getBeanProperties() {
    Map<String, BeanProperty> beanPropertyMap = new HashMap<>();
    final Collection<Field> declaredFields = ReflectionUtils.getFields(beanClass);
    for (final Field declaredField : declaredFields) {
      if (Modifier.isStatic(declaredField.getModifiers())) {
        continue;
      }
      String propertyName = getAnnotatedColumnName(declaredField);
      if (propertyName == null) {
        propertyName = declaredField.getName();
      }

      propertyName = caseSensitive ? propertyName : propertyName.toLowerCase();
      beanPropertyMap.put(propertyName, new BeanProperty(declaredField));
    }
    return beanPropertyMap;
  }

  private String getAnnotatedColumnName(AnnotatedElement classMember) {
    final Column columnInformation = ClassUtils.getAnnotation(Column.class, classMember);
    if (columnInformation != null) {
      final String name = columnInformation.value();
      if (StringUtils.isNotEmpty(name)) {
        return name;
      }
    }
    return null;
  }

  public BeanMetadata createProperty(Class<?> propertyType, boolean caseSensitive) {
    return new BeanMetadata(propertyType,
                            caseSensitive,
                            autoDeriveColumnNames,
                            columnMappings,
                            throwOnMappingFailure);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BeanMetadata that = (BeanMetadata) o;

    return autoDeriveColumnNames == that.autoDeriveColumnNames
            && caseSensitive == that.caseSensitive
            && beanClass.equals(that.beanClass)
            && columnMappings.equals(that.columnMappings)
            && beanProperties.equals(that.beanProperties);
  }

  @Override
  public int hashCode() {
    int result = (caseSensitive ? 1 : 0);
    result = 31 * result + beanClass.hashCode();
    return result;
  }

  private static class Cache extends Mappings<Map<String, BeanProperty>, BeanMetadata> {

    @Override
    protected Map<String, BeanProperty> createValue(Object key, BeanMetadata param) {
      return param.getBeanProperties();
    }
  }

}
