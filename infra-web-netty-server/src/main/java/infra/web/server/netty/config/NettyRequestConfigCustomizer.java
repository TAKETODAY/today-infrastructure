package infra.web.server.netty.config;

import infra.web.server.netty.NettyRequestConfig;

/**
 * Callback interface that can be used to customize the {@link NettyRequestConfig.Builder}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/2/11 11:20
 */
public interface NettyRequestConfigCustomizer {

  /**
   * Customize the {@link NettyRequestConfig.Builder}.
   *
   * @param builder the builder to customize
   */
  void customize(NettyRequestConfig.Builder builder);

}
