package infra.jdbc.config;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.context.properties.ConfigurationProperties;

/**
 * Properties for configuring repository behavior.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/28 21:55
 */
@ConfigurationProperties("repository")
public class RepositoryProperties {

  /**
   * Default case sensitivity setting for queries.
   * When set to false (default), queries will be case insensitive.
   */
  public boolean defaultCaseSensitive = false;

  /**
   * Whether cleanup paths that honor this setting translate and propagate
   * resource close SQL exceptions. When true, close failures are translated into
   * data access exceptions. When false (default), they are logged without propagation.
   */
  public boolean catchResourceCloseErrors = false;

  /**
   * Default column mappings used across repositories.
   * Key-value pairs where key is the entity property name and value is the corresponding column name.
   */
  public @Nullable Map<String, String> defaultColumnMappings;

}
