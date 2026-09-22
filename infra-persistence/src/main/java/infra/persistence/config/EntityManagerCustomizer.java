package infra.persistence.config;

import infra.persistence.DefaultEntityManager;

/**
 * Customizer for {@link infra.persistence.DefaultEntityManager}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/29 14:36
 */
public interface EntityManagerCustomizer {

  /**
   * Customize the given {@link infra.persistence.DefaultEntityManager}.
   *
   * @param manager the default entity manager to customize
   */
  void customize(DefaultEntityManager manager);

}
