package infra.jdbc.config;

import infra.jdbc.type.TypeHandlerManager;

/**
 * Customizer for {@link TypeHandlerManager}.
 *
 * @author <a href="https://github.com/TAKETODAY">TAKETODAY</a>
 * @since 5.0 2026/1/28 22:09
 */
public interface TypeHandlerManagerCustomizer {

  /**
   * Customize the given {@link TypeHandlerManager}.
   *
   * @param manager the type handler manager to customize
   */
  void customize(TypeHandlerManager manager);

}
