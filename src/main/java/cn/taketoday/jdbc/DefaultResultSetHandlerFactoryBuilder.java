package cn.taketoday.jdbc;

import java.util.Map;

import cn.taketoday.jdbc.reflection.BeanMetadata;
import cn.taketoday.jdbc.type.TypeHandlerRegistry;

public class DefaultResultSetHandlerFactoryBuilder implements ResultSetHandlerFactoryBuilder {
  private boolean caseSensitive;
  private boolean autoDeriveColumnNames;
  private boolean throwOnMappingError;
  private final TypeHandlerRegistry registry;
  private Map<String, String> columnMappings;

  public DefaultResultSetHandlerFactoryBuilder(TypeHandlerRegistry registry) {
    this.registry = registry;
  }

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public void setCaseSensitive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  public boolean isAutoDeriveColumnNames() {
    return autoDeriveColumnNames;
  }

  public void setAutoDeriveColumnNames(boolean autoDeriveColumnNames) {
    this.autoDeriveColumnNames = autoDeriveColumnNames;
  }

  @Override
  public boolean isThrowOnMappingError() {
    return throwOnMappingError;
  }

  @Override
  public void throwOnMappingError(boolean throwOnMappingError) {
    this.throwOnMappingError = throwOnMappingError;
  }

  public Map<String, String> getColumnMappings() {
    return columnMappings;
  }

  public void setColumnMappings(Map<String, String> columnMappings) {
    this.columnMappings = columnMappings;
  }


  public <T> ResultSetHandlerFactory<T> newFactory(Class<T> clazz) {
    BeanMetadata pojoMetadata = new BeanMetadata(clazz, caseSensitive, autoDeriveColumnNames, columnMappings, throwOnMappingError);
    return new DefaultResultSetHandlerFactory<>(pojoMetadata, registry);
  }

}
