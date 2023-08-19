/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.jdbc.core;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import cn.taketoday.beans.BeanMetadata;
import cn.taketoday.beans.BeanProperty;
import cn.taketoday.beans.BeanWrapperImpl;
import cn.taketoday.beans.NotWritablePropertyException;
import cn.taketoday.beans.TypeConverter;
import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.dao.DataRetrievalFailureException;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.format.support.ApplicationConversionService;
import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link RowMapper} implementation that converts a row into a new instance
 * of the specified mapped target class. The mapped target class must be a
 * top-level class or {@code static} nested class, and it must have a default or
 * no-arg constructor.
 *
 * <p>Column values are mapped based on matching the column name (as obtained from
 * result set meta-data) to public setters in the target class for the corresponding
 * properties. The names are matched either directly or by transforming a name
 * separating the parts with underscores to the same name using "camel" case.
 *
 * <p>Mapping is provided for properties in the target class for many common types &mdash;
 * for example: String, boolean, Boolean, byte, Byte, short, Short, int, Integer,
 * long, Long, float, Float, double, Double, BigDecimal, {@code java.util.Date}, etc.
 *
 * <p>To facilitate mapping between columns and properties that don't have matching
 * names, try using underscore-separated column aliases in the SQL statement like
 * {@code "select fname as first_name from customer"}, where {@code first_name}
 * can be mapped to a {@code setFirstName(String)} method in the target class.
 *
 * <p>For a {@code NULL} value read from the database, an attempt will be made to
 * call the corresponding setter method with {@code null}, but in the case of
 * Java primitives this will result in a {@link TypeMismatchException} by default.
 * To ignore {@code NULL} database values for all primitive properties in the
 * target class, set the {@code primitivesDefaultedForNullValue} flag to
 * {@code true}. See {@link #setPrimitivesDefaultedForNullValue(boolean)} for
 * details.
 *
 * <p>If you need to map to a target class which has a <em>data class</em> constructor
 * &mdash; for example, a Java {@code record} or a Kotlin {@code data} class &mdash;
 * use {@link DataClassRowMapper} instead.
 *
 * <p>Please note that this class is designed to provide convenience rather than
 * high performance. For best performance, consider using a custom {@code RowMapper}
 * implementation.
 *
 * @param <T> the result type
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DataClassRowMapper
 * @since 4.0
 */
public class BeanPropertyRowMapper<T> implements RowMapper<T> {
  private static final Logger log = LoggerFactory.getLogger(BeanPropertyRowMapper.class);

  /** The class we are mapping to. */
  @Nullable
  private Class<T> mappedClass;

  /** Whether we're strictly validating. */
  private boolean checkFullyPopulated = false;

  /** Whether we're defaulting primitives when mapping a null value. */
  private boolean primitivesDefaultedForNullValue = false;

  /** Map of the fields we provide mapping for. */
  @Nullable
  private HashMap<String, BeanProperty> mappedFields;

  /** Set of bean properties we provide mapping for. */
  @Nullable
  private Set<String> mappedProperties;

  @Nullable
  private BeanMetadata metadata;

  protected final BeanWrapperImpl beanWrapper = new BeanWrapperImpl();

  /**
   * Create a new {@code BeanPropertyRowMapper} for bean-style configuration.
   *
   * @see #setMappedClass
   * @see #setCheckFullyPopulated
   */
  public BeanPropertyRowMapper() { }

  /**
   * Create a new {@code BeanPropertyRowMapper}, accepting unpopulated
   * properties in the target bean.
   *
   * @param mappedClass the class that each row should be mapped to
   */
  public BeanPropertyRowMapper(Class<T> mappedClass) {
    initialize(mappedClass);
  }

  /**
   * Create a new {@code BeanPropertyRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   * @param checkFullyPopulated whether we're strictly validating that
   * all bean properties have been mapped from corresponding database fields
   */
  public BeanPropertyRowMapper(
          Class<T> mappedClass, boolean checkFullyPopulated) {
    this.checkFullyPopulated = checkFullyPopulated;
    initialize(mappedClass);
  }

  /**
   * Set the class that each row should be mapped to.
   */
  public void setMappedClass(Class<T> mappedClass) {
    if (this.mappedClass == null) {
      initialize(mappedClass);
    }
    else {
      if (this.mappedClass != mappedClass) {
        throw new InvalidDataAccessApiUsageException("The mapped class can not be reassigned to map to " +
                mappedClass + " since it is already providing mapping for " + this.mappedClass);
      }
    }
  }

  /**
   * Get the class that we are mapping to.
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public final Class<T> getMappedClass() {
    return metadata != null ? (Class<T>) metadata.getType() : null;
  }

  /**
   * Set whether we're strictly validating that all bean properties have been mapped
   * from corresponding database fields.
   * <p>Default is {@code false}, accepting unpopulated properties in the target bean.
   */
  public void setCheckFullyPopulated(boolean checkFullyPopulated) {
    this.checkFullyPopulated = checkFullyPopulated;
  }

  /**
   * Return whether we're strictly validating that all bean properties have been
   * mapped from corresponding database fields.
   */
  public boolean isCheckFullyPopulated() {
    return this.checkFullyPopulated;
  }

  /**
   * Set whether we're defaulting Java primitives in the case of mapping a null value
   * from corresponding database fields.
   * <p>Default is {@code false}, throwing an exception when nulls are mapped to Java primitives.
   */
  public void setPrimitivesDefaultedForNullValue(boolean primitivesDefaultedForNullValue) {
    this.primitivesDefaultedForNullValue = primitivesDefaultedForNullValue;
  }

  /**
   * Return whether we're defaulting Java primitives in the case of mapping a null value
   * from corresponding database fields.
   */
  public boolean isPrimitivesDefaultedForNullValue() {
    return this.primitivesDefaultedForNullValue;
  }

  /**
   * Set a {@link ConversionService} for binding JDBC values to bean properties,
   * or {@code null} for none.
   * <p>Default is a {@link DefaultConversionService}, This
   * provides support for {@code java.time} conversion and other special types.
   */
  public void setConversionService(@Nullable ConversionService conversionService) {
    beanWrapper.setConversionService(conversionService);
  }

  /**
   * Return a {@link ConversionService} for binding JDBC values to bean properties,
   * or {@code null} if none.
   */
  @Nullable
  public ConversionService getConversionService() {
    return beanWrapper.getConversionService();
  }

  /**
   * Return a {@link BeanWrapperImpl} for binding JDBC values to bean properties.
   */
  public BeanWrapperImpl getBeanWrapper() {
    return beanWrapper;
  }

  /**
   * Initialize the mapping meta-data for the given class.
   *
   * @param mappedClass the mapped class
   */
  private void initialize(Class<T> mappedClass) {
    BeanMetadata metadata = BeanMetadata.from(mappedClass);
    initialize(mappedClass, metadata);
    this.metadata = metadata;
  }

  protected void initialize(Class<T> mappedClass, BeanMetadata metadata) {
    this.mappedClass = mappedClass;
    setConversionService(ApplicationConversionService.getSharedInstance());

    HashSet<String> mappedProperties = new HashSet<>();
    HashMap<String, BeanProperty> mappedFields = new HashMap<>();
    for (BeanProperty property : metadata) {
      if (property.isWriteable()) {
        String lowerCaseName = lowerCaseName(property.getName());
        mappedFields.put(lowerCaseName, property);
        String underscoreName = underscoreName(property.getName());
        if (!lowerCaseName.equals(underscoreName)) {
          mappedFields.put(underscoreName, property);
        }
        mappedProperties.add(property.getName());
      }
    }

    this.mappedFields = mappedFields;
    this.mappedProperties = mappedProperties;
  }

  /**
   * Remove the specified property from the mapped fields.
   *
   * @param propertyName the property name (as used by property descriptors)
   */
  protected void suppressProperty(String propertyName) {
    if (this.mappedFields != null) {
      this.mappedFields.remove(lowerCaseName(propertyName));
      this.mappedFields.remove(underscoreName(propertyName));
    }
  }

  /**
   * Convert the given name to lower case.
   * By default, conversions will happen within the US locale.
   *
   * @param name the original name
   * @return the converted name
   */
  protected String lowerCaseName(String name) {
    return name.toLowerCase(Locale.US);
  }

  /**
   * Convert a name in camelCase to an underscored name in lower case.
   * <p>Any upper case letters are converted to lower case with a preceding underscore.
   *
   * @param name the original name
   * @return the converted name
   * @see JdbcUtils#convertPropertyNameToUnderscoreName
   */
  protected String underscoreName(String name) {
    return JdbcUtils.convertPropertyNameToUnderscoreName(name);
  }

  /**
   * Extract the values for all columns in the current row.
   * <p>Utilizes public setters and result set meta-data.
   *
   * @see ResultSetMetaData
   */
  @Override
  public T mapRow(ResultSet rs, int rowNumber) throws SQLException {
    BeanWrapperImpl beanWrapper = this.beanWrapper;

    T mappedObject = constructMappedInstance(rs, beanWrapper);
    beanWrapper.setBeanInstance(mappedObject);

    ResultSetMetaData rsmd = rs.getMetaData();
    int columnCount = rsmd.getColumnCount();
    Set<String> populatedProperties = isCheckFullyPopulated() ? new HashSet<>() : null;

    HashMap<String, BeanProperty> mappedFields = this.mappedFields;
    for (int index = 1; index <= columnCount; index++) {
      String column = JdbcUtils.lookupColumnName(rsmd, index);
      String field = lowerCaseName(StringUtils.delete(column, " "));

      BeanProperty property = mappedFields != null ? mappedFields.get(field) : null;
      if (property != null) {
        try {
          Object value = getColumnValue(rs, index, property); // TODO using TypeHandler
          if (rowNumber == 0 && log.isDebugEnabled()) {
            log.debug("Mapping column '{}' to property '{}' of type '{}'",
                    column, property.getName(), ClassUtils.getQualifiedName(property.getType()));
          }
          try {
            beanWrapper.setPropertyValue(property.getName(), value);
          }
          catch (TypeMismatchException ex) {
            if (value == null && this.primitivesDefaultedForNullValue) {
              if (log.isDebugEnabled()) {
                log.debug("Intercepted TypeMismatchException for row {} and column '{}'" +
                                " with null value when setting property '{}' of type '{}' on object: {}",
                        rowNumber, column, property.getName(), ClassUtils.getQualifiedName(property.getType()), mappedObject, ex);
              }
            }
            else {
              throw ex;
            }
          }
          if (populatedProperties != null) {
            populatedProperties.add(property.getName());
          }
        }
        catch (NotWritablePropertyException ex) {
          throw new DataRetrievalFailureException(
                  "Unable to map column '" + column + "' to property '" + property.getName() + "'", ex);
        }
      }
      else {
        // No BeanProperty found
        if (rowNumber == 0 && log.isDebugEnabled()) {
          log.debug("No property found for column '{}' mapped to field '{}'", column, field);
        }
      }
    }

    if (populatedProperties != null && !populatedProperties.equals(this.mappedProperties)) {
      throw new InvalidDataAccessApiUsageException("Given ResultSet does not contain all fields " +
              "necessary to populate object of " + this.mappedClass + ": " + this.mappedProperties);
    }

    return mappedObject;
  }

  /**
   * Construct an instance of the mapped class for the current row.
   *
   * @param rs the ResultSet to map (pre-initialized for the current row)
   * @param converter a TypeConverter with this RowMapper's conversion service
   * @return a corresponding instance of the mapped class
   * @throws SQLException if an SQLException is encountered
   */
  @SuppressWarnings("unchecked")
  protected T constructMappedInstance(ResultSet rs, TypeConverter converter) throws SQLException {
    Assert.state(metadata != null, "Mapped class was not specified");
    return (T) metadata.newInstance();
  }

  /**
   * Retrieve a JDBC object value for the specified column.
   * <p>The default implementation delegates to
   * {@link #getColumnValue(ResultSet, int, Class)}.
   *
   * @param rs is the ResultSet holding the data
   * @param index is the column index
   * @param pd the bean property that each result object is expected to match
   * @return the Object value
   * @throws SQLException in case of extraction failure
   * @see #getColumnValue(ResultSet, int, Class)
   */
  @Nullable
  protected Object getColumnValue(ResultSet rs, int index, BeanProperty pd) throws SQLException {
    return JdbcUtils.getResultSetValue(rs, index, pd.getType());
  }

  /**
   * Retrieve a JDBC object value for the specified column.
   * <p>The default implementation calls
   * {@link JdbcUtils#getResultSetValue(ResultSet, int, Class)}.
   * Subclasses may override this to check specific value types upfront,
   * or to post-process values return from {@code getResultSetValue}.
   *
   * @param rs is the ResultSet holding the data
   * @param index is the column index
   * @param paramType the target parameter type
   * @return the Object value
   * @throws SQLException in case of extraction failure
   * @see cn.taketoday.jdbc.support.JdbcUtils#getResultSetValue(ResultSet, int, Class)
   */
  @Nullable
  protected Object getColumnValue(ResultSet rs, int index, Class<?> paramType) throws SQLException {
    return JdbcUtils.getResultSetValue(rs, index, paramType);
  }

  /**
   * Static factory method to create a new {@code BeanPropertyRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   * @see #forMappedClass(Class, ConversionService)
   */
  public static <T> BeanPropertyRowMapper<T> forMappedClass(Class<T> mappedClass) {
    return new BeanPropertyRowMapper<>(mappedClass);
  }

  /**
   * Static factory method to create a new {@code BeanPropertyRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   * @param conversionService the {@link ConversionService} for binding
   * JDBC values to bean properties, or {@code null} for none
   * @see #forMappedClass(Class)
   * @see #setConversionService
   */
  public static <T> BeanPropertyRowMapper<T> forMappedClass(
          Class<T> mappedClass, @Nullable ConversionService conversionService) {

    BeanPropertyRowMapper<T> rowMapper = forMappedClass(mappedClass);
    rowMapper.setConversionService(conversionService);
    return rowMapper;
  }

}
