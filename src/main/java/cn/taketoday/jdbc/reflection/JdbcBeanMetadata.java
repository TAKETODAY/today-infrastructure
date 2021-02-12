package cn.taketoday.jdbc.reflection;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import cn.taketoday.context.factory.BeanMetadata;
import cn.taketoday.context.factory.BeanProperty;
import cn.taketoday.jdbc.utils.UnderscoreToCamelCase;

/**
 * Stores metadata for a POJO.2
 *
 * @author TODAY
 */
public class JdbcBeanMetadata extends BeanMetadata {

  private final Map<String, String> columnMappings;

  private final boolean caseSensitive;
  public final boolean throwOnMappingFailure;
  private final boolean autoDeriveColumnNames;

  public JdbcBeanMetadata(
          Class<?> clazz,
          boolean caseSensitive,
          boolean autoDeriveColumnNames,
          Map<String, String> columnMappings,
          boolean throwOnMappingError
  ) {
    super(clazz);
    this.caseSensitive = caseSensitive;
    this.autoDeriveColumnNames = autoDeriveColumnNames;
    this.columnMappings = columnMappings == null ? Collections.emptyMap() : columnMappings;
    this.throwOnMappingFailure = throwOnMappingError;
  }

  public Map<String, String> getColumnMappings() {
    return columnMappings;
  }

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public boolean isAutoDeriveColumnNames() {
    return autoDeriveColumnNames;
  }

  @Override
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

    return super.getBeanProperty(name);
  }

  @Override
  protected String getPropertyName(Field declaredField) {
    String propertyName = super.getPropertyName(declaredField);
    return caseSensitive ? propertyName : propertyName.toLowerCase();
  }

  public JdbcBeanMetadata createProperty(Class<?> propertyType, boolean caseSensitive) {
    return new JdbcBeanMetadata(propertyType,
                                caseSensitive,
                                autoDeriveColumnNames,
                                columnMappings,
                                throwOnMappingFailure);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    JdbcBeanMetadata that = (JdbcBeanMetadata) o;

    return autoDeriveColumnNames == that.autoDeriveColumnNames
            && caseSensitive == that.caseSensitive
            && super.equals(that)
            && columnMappings.equals(that.columnMappings);
  }

  @Override
  public int hashCode() {
    int result = (caseSensitive ? 1 : 0);
    result = 31 * result + super.hashCode();
    return result;
  }

}
