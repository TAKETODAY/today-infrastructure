package infra.persistence.config;

import infra.context.properties.ConfigurationProperties;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/29 14:31
 */
@ConfigurationProperties("persistence")
public class PersistenceProperties {

  /**
   * Sets the number of batched commands this Query allows
   * to be added before implicitly calling executeBatch() from addToBatch().
   * <p>
   * When set to 0, executeBatch is not called implicitly. This is the default behaviour.
   */
  public int maxBatchRecords = 0;

  /**
   * a flag indicating whether auto-generated keys should be returned;
   */
  public boolean autoGenerateId = true;

}
