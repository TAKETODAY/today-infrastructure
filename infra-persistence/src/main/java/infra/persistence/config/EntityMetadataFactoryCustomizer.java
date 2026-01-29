package infra.persistence.config;

import infra.persistence.DefaultEntityMetadataFactory;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/29 16:29
 */
public interface EntityMetadataFactoryCustomizer {

  void customize(DefaultEntityMetadataFactory factory);
}
