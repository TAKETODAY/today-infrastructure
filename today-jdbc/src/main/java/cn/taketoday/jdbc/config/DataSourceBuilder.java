/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.jdbc.config;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zaxxer.hikari.HikariDataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.postgresql.ds.PGSimpleDataSource;

import java.beans.PropertyVetoException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.sql.DataSource;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.jdbc.datasource.SimpleDriverDataSource;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabase;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;
import oracle.jdbc.datasource.OracleDataSource;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceImpl;

/**
 * Convenience class for building a {@link DataSource}. Provides a limited subset of the
 * properties supported by a typical {@link DataSource} as well as detection logic to pick
 * the most suitable pooling {@link DataSource} implementation.
 * <p>
 * The following pooling {@link DataSource} implementations are supported by this builder.
 * When no {@link #type(Class) type} has been explicitly set, the first available pool
 * implementation will be picked:
 * <ul>
 * <li>Hikari ({@code com.zaxxer.hikari.HikariDataSource})</li>
 * <li>Tomcat JDBC Pool ({@code org.apache.tomcat.jdbc.pool.DataSource})</li>
 * <li>Apache DBCP2 ({@code org.apache.commons.dbcp2.BasicDataSource})</li>
 * <li>Oracle UCP ({@code oracle.ucp.jdbc.PoolDataSourceImpl})</li>
 * </ul>
 * <p>
 * The following non-pooling {@link DataSource} implementations can be used when
 * explicitly set as a {@link #type(Class) type}:
 * <ul>
 * <li>Framework's {@code SimpleDriverDataSource}
 * ({@code cn.taketoday.jdbc.datasource.SimpleDriverDataSource})</li>
 * <li>Oracle ({@code oracle.jdbc.datasource.OracleDataSource})</li>
 * <li>H2 ({@code org.h2.jdbcx.JdbcDataSource})</li>
 * <li>Postgres ({@code org.postgresql.ds.PGSimpleDataSource})</li>
 * <li>Any {@code DataSource} implementation with appropriately named methods</li>
 * </ul>
 * <p>
 * This class is commonly used in an {@code @Bean} method and often combined with
 * {@code @ConfigurationProperties}.
 *
 * @param <T> the {@link DataSource} type being built
 * @author Dave Syer
 * @author Madhura Bhave
 * @author Fabio Grassi
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #create()
 * @see #create(ClassLoader)
 * @see #derivedFrom(DataSource)
 * @since 4.0 2022/2/23 17:22
 */
public final class DataSourceBuilder<T extends DataSource> {
  @Nullable
  private final ClassLoader classLoader;

  private final Map<DataSourceProperty, String> values = new HashMap<>();

  @Nullable
  private Class<T> type;

  @Nullable
  private final DataSource deriveFrom;

  private DataSourceBuilder(@Nullable ClassLoader classLoader) {
    this.classLoader = classLoader;
    this.deriveFrom = null;
  }

  @SuppressWarnings("unchecked")
  private DataSourceBuilder(T deriveFrom) {
    Assert.notNull(deriveFrom, "DataSource is required");
    this.classLoader = deriveFrom.getClass().getClassLoader();
    this.type = (Class<T>) deriveFrom.getClass();
    this.deriveFrom = deriveFrom;
  }

  /**
   * Set the {@link DataSource} type that should be built.
   *
   * @param <D> the datasource type
   * @param type the datasource type
   * @return this builder
   */
  @SuppressWarnings("unchecked")
  public <D extends DataSource> DataSourceBuilder<D> type(@Nullable Class<D> type) {
    this.type = (Class<T>) type;
    return (DataSourceBuilder<D>) this;
  }

  /**
   * Set the URL that should be used when building the datasource.
   *
   * @param url the JDBC url
   * @return this builder
   */
  public DataSourceBuilder<T> url(String url) {
    set(DataSourceProperty.URL, url);
    return this;
  }

  /**
   * Set the driver class name that should be used when building the datasource.
   *
   * @param driverClassName the driver class name
   * @return this builder
   */
  public DataSourceBuilder<T> driverClassName(String driverClassName) {
    set(DataSourceProperty.DRIVER_CLASS_NAME, driverClassName);
    return this;
  }

  /**
   * Set the username that should be used when building the datasource.
   *
   * @param username the user name
   * @return this builder
   */
  public DataSourceBuilder<T> username(@Nullable String username) {
    set(DataSourceProperty.USERNAME, username);
    return this;
  }

  /**
   * Set the password that should be used when building the datasource.
   *
   * @param password the password
   * @return this builder
   */
  public DataSourceBuilder<T> password(@Nullable String password) {
    set(DataSourceProperty.PASSWORD, password);
    return this;
  }

  private void set(DataSourceProperty property, @Nullable String value) {
    this.values.put(property, value);
  }

  /**
   * Return a newly built {@link DataSource} instance.
   *
   * @return the built datasource
   */
  public T build() {
    DataSourceProperties<T> properties = DataSourceProperties.forType(this.classLoader, this.type);
    DataSourceProperties<DataSource> deriveFromProperties = getDeriveFromProperties();
    Class<? extends T> instanceType = (this.type != null) ? this.type : properties.getDataSourceInstanceType();
    T dataSource = BeanUtils.newInstance(instanceType);
    Set<DataSourceProperty> applied = new HashSet<>();
    for (DataSourceProperty property : DataSourceProperty.values()) {
      String value = this.values.get(property);
      if (value == null && deriveFromProperties != null && properties.canSet(property)) {
        value = deriveFromProperties.get(this.deriveFrom, property);
      }
      if (value != null) {
        properties.set(dataSource, property, value);
        applied.add(property);
      }
    }
    if (!applied.contains(DataSourceProperty.DRIVER_CLASS_NAME)
            && properties.canSet(DataSourceProperty.DRIVER_CLASS_NAME)
            && this.values.containsKey(DataSourceProperty.URL)) {
      String url = this.values.get(DataSourceProperty.URL);
      DatabaseDriver driver = DatabaseDriver.fromJdbcUrl(url);
      properties.set(dataSource, DataSourceProperty.DRIVER_CLASS_NAME, driver.getDriverClassName());
    }
    return dataSource;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private DataSourceProperties<DataSource> getDeriveFromProperties() {
    if (this.deriveFrom == null) {
      return null;
    }
    return DataSourceProperties.forType(this.classLoader, (Class<DataSource>) this.deriveFrom.getClass());
  }

  /**
   * Create a new {@link DataSourceBuilder} instance.
   *
   * @return a new datasource builder instance
   */
  public static DataSourceBuilder<?> create() {
    return create(null);
  }

  /**
   * Create a new {@link DataSourceBuilder} instance.
   *
   * @param classLoader the classloader used to discover preferred settings
   * @return a new {@link DataSource} builder instance
   */
  public static DataSourceBuilder<?> create(@Nullable ClassLoader classLoader) {
    return new DataSourceBuilder<>(classLoader);
  }

  /**
   * Create a new {@link DataSourceBuilder} instance derived from the specified data
   * source. The returned builder can be used to build the same type of
   * {@link DataSource} with {@code username}, {@code password}, {@code url} and
   * {@code driverClassName} properties copied from the original when not specifically
   * set.
   *
   * @param dataSource the source {@link DataSource}
   * @return a new {@link DataSource} builder
   */
  public static DataSourceBuilder<?> derivedFrom(DataSource dataSource) {
    if (dataSource instanceof EmbeddedDatabase) {
      try {
        dataSource = dataSource.unwrap(DataSource.class);
      }
      catch (SQLException ex) {
        throw new IllegalStateException("Unable to unwrap embedded database", ex);
      }
    }
    return new DataSourceBuilder<>(unwrap(dataSource));
  }

  private static DataSource unwrap(DataSource dataSource) {
    try {
      while (dataSource.isWrapperFor(DataSource.class)) {
        DataSource unwrapped = dataSource.unwrap(DataSource.class);
        if (unwrapped == dataSource) {
          return unwrapped;
        }
        dataSource = unwrapped;
      }
    }
    catch (SQLException ex) {
      // Try to continue with the existing, potentially still wrapped, DataSource
    }
    return dataSource;
  }

  /**
   * Find the {@link DataSource} type preferred for the given classloader.
   *
   * @param classLoader the classloader used to discover preferred settings
   * @return the preferred {@link DataSource} type
   */
  @Nullable
  public static Class<? extends DataSource> findType(@Nullable ClassLoader classLoader) {
    MappedDataSourceProperties<?> mappings = MappedDataSourceProperties.forType(classLoader, null);
    return (mappings != null) ? mappings.getDataSourceInstanceType() : null;
  }

  /**
   * An individual DataSource property supported by the builder.
   */
  private enum DataSourceProperty {

    URL(false, "url", "URL"),

    DRIVER_CLASS_NAME(true, "driverClassName"),

    USERNAME(false, "username", "user"),

    PASSWORD(false, "password");

    private final boolean optional;

    private final String[] names;

    DataSourceProperty(boolean optional, String... names) {
      this.optional = optional;
      this.names = names;
    }

    boolean isRequired() {
      return !this.optional;
    }

    @Override
    public String toString() {
      return this.names[0];
    }

    @Nullable
    Method findSetter(Class<?> type) {
      return findMethod("set", type, String.class);
    }

    @Nullable
    Method findGetter(Class<?> type) {
      return findMethod("get", type);
    }

    @Nullable
    private Method findMethod(String prefix, Class<?> type, Class<?>... paramTypes) {
      for (String name : this.names) {
        String candidate = prefix + StringUtils.capitalize(name);
        Method method = ReflectionUtils.findMethod(type, candidate, paramTypes);
        if (method != null) {
          return method;
        }
      }
      return null;
    }

  }

  private interface DataSourceProperties<T extends DataSource> {

    Class<? extends T> getDataSourceInstanceType();

    boolean canSet(DataSourceProperty property);

    void set(T dataSource, DataSourceProperty property, String value);

    @Nullable
    String get(T dataSource, DataSourceProperty property);

    static <T extends DataSource> DataSourceProperties<T> forType(
            @Nullable ClassLoader classLoader, @Nullable Class<T> type) {
      MappedDataSourceProperties<T> mapped = MappedDataSourceProperties.forType(classLoader, type);
      return (mapped != null) ? mapped : new ReflectionDataSourceProperties<>(type);
    }

  }

  private static class MappedDataSourceProperties<T extends DataSource> implements DataSourceProperties<T> {

    private final Map<DataSourceProperty, MappedDataSourceProperty<T, ?>> mappedProperties = new HashMap<>();

    private final Class<T> dataSourceType;

    @SuppressWarnings("unchecked")
    MappedDataSourceProperties() {
      this.dataSourceType = (Class<T>) ResolvableType.forClass(MappedDataSourceProperties.class, getClass())
              .resolveGeneric();
    }

    @Override
    public Class<? extends T> getDataSourceInstanceType() {
      return this.dataSourceType;
    }

    protected void add(DataSourceProperty property, @Nullable Getter<T, String> getter, @NonNull Setter<T, String> setter) {
      add(property, String.class, getter, setter);
    }

    protected <V> void add(DataSourceProperty property, Class<V> type, @Nullable Getter<T, V> getter, Setter<T, V> setter) {
      this.mappedProperties.put(property, new MappedDataSourceProperty<>(property, type, getter, setter));
    }

    @Override
    public boolean canSet(DataSourceProperty property) {
      return this.mappedProperties.containsKey(property);
    }

    @Override
    public void set(T dataSource, DataSourceProperty property, String value) {
      MappedDataSourceProperty<T, ?> mappedProperty = getMapping(property);
      if (mappedProperty != null) {
        mappedProperty.set(dataSource, value);
      }
    }

    @Nullable
    @Override
    public String get(T dataSource, DataSourceProperty property) {
      MappedDataSourceProperty<T, ?> mappedProperty = getMapping(property);
      if (mappedProperty != null) {
        return mappedProperty.get(dataSource);
      }
      return null;
    }

    @Nullable
    private MappedDataSourceProperty<T, ?> getMapping(DataSourceProperty property) {
      MappedDataSourceProperty<T, ?> mappedProperty = this.mappedProperties.get(property);
      UnsupportedDataSourcePropertyException.throwIf(property.isRequired() && mappedProperty == null,
              () -> "No mapping found for " + property);
      return mappedProperty;
    }

    @Nullable
    static <T extends DataSource> MappedDataSourceProperties<T> forType(@Nullable ClassLoader classLoader, @Nullable Class<T> type) {
      MappedDataSourceProperties<T> pooled = lookupPooled(classLoader, type);
      if (type == null || pooled != null) {
        return pooled;
      }
      return lookupBasic(classLoader, type);
    }

    @Nullable
    private static <T extends DataSource> MappedDataSourceProperties<T> lookupPooled(
            @Nullable ClassLoader classLoader, @Nullable Class<T> type) {
      var result = lookup(classLoader, type, null, "com.zaxxer.hikari.HikariDataSource", HikariDataSourceProperties::new);
      result = lookup(classLoader, type, result, "org.apache.tomcat.jdbc.pool.DataSource", TomcatPoolDataSourceProperties::new);
      result = lookup(classLoader, type, result, "org.apache.commons.dbcp2.BasicDataSource", MappedDbcp2DataSource::new);
      result = lookup(classLoader, type, result, "oracle.ucp.jdbc.PoolDataSourceImpl", OraclePoolDataSourceProperties::new, "oracle.jdbc.OracleConnection");
      result = lookup(classLoader, type, result, "com.mchange.v2.c3p0.ComboPooledDataSource", ComboPooledDataSourceProperties::new);
      return result;
    }

    @Nullable
    private static <T extends DataSource> MappedDataSourceProperties<T> lookupBasic(
            @Nullable ClassLoader classLoader, Class<T> dataSourceType) {
      MappedDataSourceProperties<T> result =
              lookup(classLoader, dataSourceType, null,
                      "cn.taketoday.jdbc.datasource.SimpleDriverDataSource", SimpleDataSourceProperties::new);
      result = lookup(classLoader, dataSourceType, result,
              "oracle.jdbc.datasource.OracleDataSource", OracleDataSourceProperties::new);
      result = lookup(classLoader, dataSourceType, result,
              "org.h2.jdbcx.JdbcDataSource", H2DataSourceProperties::new);
      result = lookup(classLoader, dataSourceType, result,
              "org.postgresql.ds.PGSimpleDataSource", PostgresDataSourceProperties::new);
      return result;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <T extends DataSource> MappedDataSourceProperties<T> lookup(
            @Nullable ClassLoader classLoader, @Nullable Class<T> dataSourceType,
            @Nullable MappedDataSourceProperties<T> existing, String dataSourceClassName,
            Supplier<MappedDataSourceProperties<?>> propertyMappingsSupplier, String... requiredClassNames) {
      if (existing != null || !allPresent(classLoader, dataSourceClassName, requiredClassNames)) {
        return existing;
      }
      MappedDataSourceProperties<?> propertyMappings = propertyMappingsSupplier.get();
      return (dataSourceType == null || propertyMappings.getDataSourceInstanceType().isAssignableFrom(dataSourceType))
             ? (MappedDataSourceProperties<T>) propertyMappings : null;
    }

    private static boolean allPresent(
            @Nullable ClassLoader classLoader, String dataSourceClassName, String[] requiredClassNames) {
      boolean result = ClassUtils.isPresent(dataSourceClassName, classLoader);
      for (String requiredClassName : requiredClassNames) {
        result = result && ClassUtils.isPresent(requiredClassName, classLoader);
      }
      return result;
    }

  }

  private record MappedDataSourceProperty<T extends DataSource, V>(
          DataSourceProperty property, Class<V> type, @Nullable Getter<T, V> getter, @Nullable Setter<T, V> setter) {

    void set(T dataSource, String value) {
      try {
        if (this.setter == null) {
          UnsupportedDataSourcePropertyException.throwIf(this.property.isRequired(),
                  () -> "No setter mapped for '" + this.property + "' property");
          return;
        }
        this.setter.set(dataSource, convertFromString(value));
      }
      catch (SQLException ex) {
        throw new IllegalStateException(ex);
      }
    }

    @Nullable
    String get(T dataSource) {
      try {
        if (this.getter == null) {
          UnsupportedDataSourcePropertyException.throwIf(this.property.isRequired(),
                  () -> "No getter mapped for '" + this.property + "' property");
          return null;
        }
        return convertToString(this.getter.get(dataSource));
      }
      catch (SQLException ex) {
        throw new IllegalStateException(ex);
      }
    }

    @SuppressWarnings("unchecked")
    private V convertFromString(String value) {
      if (String.class.equals(this.type)) {
        return (V) value;
      }
      if (Class.class.equals(this.type)) {
        return (V) ClassUtils.resolveClassName(value, null);
      }
      throw new IllegalStateException("Unsupported value type " + this.type);
    }

    private String convertToString(V value) {
      if (String.class.equals(this.type)) {
        return (String) value;
      }
      if (Class.class.equals(this.type)) {
        return ((Class<?>) value).getName();
      }
      throw new IllegalStateException("Unsupported value type " + this.type);
    }

  }

  private static class ReflectionDataSourceProperties<T extends DataSource> implements DataSourceProperties<T> {

    private final Map<DataSourceProperty, Method> getters;

    private final Map<DataSourceProperty, Method> setters;

    private final Class<T> dataSourceType;

    ReflectionDataSourceProperties(@NonNull Class<T> dataSourceType) {
      Assert.notNull(dataSourceType, "No supported DataSource type found");
      Map<DataSourceProperty, Method> getters = new HashMap<>();
      Map<DataSourceProperty, Method> setters = new HashMap<>();
      for (DataSourceProperty property : DataSourceProperty.values()) {
        putIfNotNull(getters, property, property.findGetter(dataSourceType));
        putIfNotNull(setters, property, property.findSetter(dataSourceType));
      }
      this.dataSourceType = dataSourceType;
      this.getters = Collections.unmodifiableMap(getters);
      this.setters = Collections.unmodifiableMap(setters);
    }

    private void putIfNotNull(Map<DataSourceProperty, Method> map, DataSourceProperty property, @Nullable Method method) {
      if (method != null) {
        map.put(property, method);
      }
    }

    @Override
    public Class<T> getDataSourceInstanceType() {
      return this.dataSourceType;
    }

    @Override
    public boolean canSet(DataSourceProperty property) {
      return this.setters.containsKey(property);
    }

    @Override
    public void set(T dataSource, DataSourceProperty property, String value) {
      Method method = getMethod(property, this.setters);
      if (method != null) {
        ReflectionUtils.invokeMethod(method, dataSource, value);
      }
    }

    @Nullable
    @Override
    public String get(T dataSource, DataSourceProperty property) {
      Method method = getMethod(property, this.getters);
      if (method != null) {
        return (String) ReflectionUtils.invokeMethod(method, dataSource);
      }
      return null;
    }

    @Nullable
    private Method getMethod(DataSourceProperty property, Map<DataSourceProperty, Method> methods) {
      Method method = methods.get(property);
      if (method == null) {
        UnsupportedDataSourcePropertyException.throwIf(property.isRequired(),
                () -> "Unable to find suitable method for " + property);
        return null;
      }
      ReflectionUtils.makeAccessible(method);
      return method;
    }

  }

  @FunctionalInterface
  private interface Getter<T, V> {

    V get(T instance) throws SQLException;

  }

  @FunctionalInterface
  private interface Setter<T, V> {

    void set(T instance, V value) throws SQLException;

  }

  /**
   * {@link DataSourceProperties} for Hikari.
   */
  private static class HikariDataSourceProperties extends MappedDataSourceProperties<HikariDataSource> {

    HikariDataSourceProperties() {
      add(DataSourceProperty.URL, HikariDataSource::getJdbcUrl, HikariDataSource::setJdbcUrl);
      add(DataSourceProperty.DRIVER_CLASS_NAME, HikariDataSource::getDriverClassName,
              HikariDataSource::setDriverClassName);
      add(DataSourceProperty.USERNAME, HikariDataSource::getUsername, HikariDataSource::setUsername);
      add(DataSourceProperty.PASSWORD, HikariDataSource::getPassword, HikariDataSource::setPassword);
    }

  }

  /**
   * {@link DataSourceProperties} for Tomcat Pool.
   */
  private static class TomcatPoolDataSourceProperties
          extends MappedDataSourceProperties<org.apache.tomcat.jdbc.pool.DataSource> {

    TomcatPoolDataSourceProperties() {
      add(DataSourceProperty.URL, org.apache.tomcat.jdbc.pool.DataSource::getUrl,
              org.apache.tomcat.jdbc.pool.DataSource::setUrl);
      add(DataSourceProperty.DRIVER_CLASS_NAME, org.apache.tomcat.jdbc.pool.DataSource::getDriverClassName,
              org.apache.tomcat.jdbc.pool.DataSource::setDriverClassName);
      add(DataSourceProperty.USERNAME, org.apache.tomcat.jdbc.pool.DataSource::getUsername,
              org.apache.tomcat.jdbc.pool.DataSource::setUsername);
      add(DataSourceProperty.PASSWORD, org.apache.tomcat.jdbc.pool.DataSource::getPassword,
              org.apache.tomcat.jdbc.pool.DataSource::setPassword);
    }

  }

  /**
   * {@link DataSourceProperties} for DBCP2.
   */
  private static class MappedDbcp2DataSource extends MappedDataSourceProperties<BasicDataSource> {

    MappedDbcp2DataSource() {
      add(DataSourceProperty.URL, BasicDataSource::getUrl, BasicDataSource::setUrl);
      add(DataSourceProperty.USERNAME, BasicDataSource::getUsername, BasicDataSource::setUsername);
      add(DataSourceProperty.PASSWORD, BasicDataSource::getPassword, BasicDataSource::setPassword);
      add(DataSourceProperty.DRIVER_CLASS_NAME, BasicDataSource::getDriverClassName, BasicDataSource::setDriverClassName);
    }

  }

  /**
   * {@link DataSourceProperties} for Oracle Pool.
   */
  private static class OraclePoolDataSourceProperties extends MappedDataSourceProperties<PoolDataSource> {

    @Override
    public Class<? extends PoolDataSource> getDataSourceInstanceType() {
      return PoolDataSourceImpl.class;
    }

    OraclePoolDataSourceProperties() {
      add(DataSourceProperty.URL, PoolDataSource::getURL, PoolDataSource::setURL);
      add(DataSourceProperty.USERNAME, PoolDataSource::getUser, PoolDataSource::setUser);
      add(DataSourceProperty.PASSWORD, null, PoolDataSource::setPassword);
      add(DataSourceProperty.DRIVER_CLASS_NAME, PoolDataSource::getConnectionFactoryClassName, PoolDataSource::setConnectionFactoryClassName);
    }

  }

  /**
   * {@link DataSourceProperties} for C3P0.
   */
  private static class ComboPooledDataSourceProperties extends MappedDataSourceProperties<ComboPooledDataSource> {

    ComboPooledDataSourceProperties() {
      add(DataSourceProperty.URL, ComboPooledDataSource::getJdbcUrl, ComboPooledDataSource::setJdbcUrl);
      add(DataSourceProperty.DRIVER_CLASS_NAME, ComboPooledDataSource::getDriverClass, this::setDriverClass);
      add(DataSourceProperty.USERNAME, ComboPooledDataSource::getUser, ComboPooledDataSource::setUser);
      add(DataSourceProperty.PASSWORD, ComboPooledDataSource::getPassword, ComboPooledDataSource::setPassword);
    }

    private void setDriverClass(ComboPooledDataSource dataSource, String driverClass) {
      try {
        dataSource.setDriverClass(driverClass);
      }
      catch (PropertyVetoException ex) {
        throw new IllegalArgumentException(ex);
      }
    }

  }

  /**
   * {@link DataSourceProperties} for Framework's {@link SimpleDriverDataSource}.
   */
  private static class SimpleDataSourceProperties extends MappedDataSourceProperties<SimpleDriverDataSource> {

    @SuppressWarnings("unchecked")
    SimpleDataSourceProperties() {
      add(DataSourceProperty.URL, SimpleDriverDataSource::getUrl, SimpleDriverDataSource::setUrl);
      add(DataSourceProperty.DRIVER_CLASS_NAME, Class.class, (dataSource) -> dataSource.getDriver().getClass(),
              (dataSource, driverClass) -> dataSource.setDriverClass(driverClass));
      add(DataSourceProperty.USERNAME, SimpleDriverDataSource::getUsername, SimpleDriverDataSource::setUsername);
      add(DataSourceProperty.PASSWORD, SimpleDriverDataSource::getPassword, SimpleDriverDataSource::setPassword);
    }

  }

  /**
   * {@link DataSourceProperties} for Oracle.
   */
  private static class OracleDataSourceProperties extends MappedDataSourceProperties<OracleDataSource> {

    OracleDataSourceProperties() {
      add(DataSourceProperty.URL, OracleDataSource::getURL, OracleDataSource::setURL);
      add(DataSourceProperty.USERNAME, OracleDataSource::getUser, OracleDataSource::setUser);
      add(DataSourceProperty.PASSWORD, null, OracleDataSource::setPassword);
    }

  }

  /**
   * {@link DataSourceProperties} for H2.
   */
  private static class H2DataSourceProperties extends MappedDataSourceProperties<JdbcDataSource> {

    H2DataSourceProperties() {
      add(DataSourceProperty.URL, JdbcDataSource::getUrl, JdbcDataSource::setUrl);
      add(DataSourceProperty.USERNAME, JdbcDataSource::getUser, JdbcDataSource::setUser);
      add(DataSourceProperty.PASSWORD, JdbcDataSource::getPassword, JdbcDataSource::setPassword);
    }

  }

  /**
   * {@link DataSourceProperties} for Postgres.
   */
  private static class PostgresDataSourceProperties extends MappedDataSourceProperties<PGSimpleDataSource> {

    PostgresDataSourceProperties() {
      add(DataSourceProperty.URL, PGSimpleDataSource::getUrl, PGSimpleDataSource::setUrl);
      add(DataSourceProperty.USERNAME, PGSimpleDataSource::getUser, PGSimpleDataSource::setUser);
      add(DataSourceProperty.PASSWORD, PGSimpleDataSource::getPassword, PGSimpleDataSource::setPassword);
    }

  }

}
