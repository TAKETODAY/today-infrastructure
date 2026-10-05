package infra.persistence.config;

import infra.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the auto-configured persistence entity manager,
 * bound to the {@code persistence} prefix.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see EntityManagerAutoConfiguration
 * @since 5.0 2026/1/29 14:31
 */
@ConfigurationProperties("persistence")
public class PersistenceProperties {

  /**
   * Default number of rows per page when no pagination details are supplied.
   * <p>Defaults to {@code 10}, with page number {@code 1}. Must be positive.
   * Explicit pagination details take precedence over this default.
   */
  public int pageSize = 10;

  /**
   * Number of records accumulated per JDBC insert batch before it is executed
   * while iterating over entities. Must be non-negative.
   * <p>Defaults to {@code 0}, disabling intermediate batch execution. Remaining
   * records are executed after the input has been consumed in either case.
   * <p>The threshold applies separately to each prepared-statement batch, not
   * to the total number of records buffered by the persistence operation.
   * Executing an intermediate batch does not commit the transaction.
   */
  public int maxBatchRecords = 0;

  /**
   * Enable annotation-based entity auditing. Defaults to {@code false}.
   * Uses a {@code Clock} bean when available, otherwise the UTC system clock;
   * an optional {@code AuditorAware} bean supplies the current auditor.
   */
  public boolean auditingEnabled = false;

}
