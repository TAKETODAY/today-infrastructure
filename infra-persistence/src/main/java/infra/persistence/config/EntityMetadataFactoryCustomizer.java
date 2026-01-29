package infra.persistence.config;

import infra.persistence.DefaultEntityMetadataFactory;

/**
 * Customizer for {@link DefaultEntityMetadataFactory}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/29 16:29
 */
public interface EntityMetadataFactoryCustomizer {

  /**
   * Customize the given {@link DefaultEntityMetadataFactory}.
   *
   * @param factory the entity metadata factory to customize
   */
  void customize(DefaultEntityMetadataFactory factory);
}
