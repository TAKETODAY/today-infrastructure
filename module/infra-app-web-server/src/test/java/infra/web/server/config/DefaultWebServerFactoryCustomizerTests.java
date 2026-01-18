package infra.web.server.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import infra.app.ssl.DefaultSslBundleRegistry;
import infra.app.ssl.SslBundles;
import infra.test.util.ReflectionTestUtils;
import infra.web.server.Shutdown;
import infra.web.server.Ssl;
import infra.web.server.reactive.ConfigurableReactiveWebServerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/14 13:00
 */
class DefaultWebServerFactoryCustomizerTests {

  private final ServerProperties properties = new ServerProperties();

  private final SslBundles sslBundles = new DefaultSslBundleRegistry();

  private DefaultWebServerFactoryCustomizer customizer;

  @BeforeEach
  void setup() {
    this.customizer = new DefaultWebServerFactoryCustomizer(this.properties, this.sslBundles, null);
  }

  @Test
  void testCustomizeServerPort() {
    ConfigurableReactiveWebServerFactory factory = mock(ConfigurableReactiveWebServerFactory.class);
    this.properties.port = (9000);
    this.customizer.customize(factory);
    then(factory).should().setPort(9000);
  }

  @Test
  void testCustomizeServerAddress() {
    ConfigurableReactiveWebServerFactory factory = mock(ConfigurableReactiveWebServerFactory.class);
    InetAddress address = InetAddress.getLoopbackAddress();
    this.properties.address = (address);
    this.customizer.customize(factory);
    then(factory).should().setAddress(address);
  }

  @Test
  void testCustomizeServerSsl() {
    ConfigurableReactiveWebServerFactory factory = mock(ConfigurableReactiveWebServerFactory.class);
    Ssl ssl = mock(Ssl.class);
    ReflectionTestUtils.setField(properties, "ssl", ssl);
    this.customizer.customize(factory);
    then(factory).should().setSsl(ssl);
    then(factory).should().setSslBundles(this.sslBundles);
  }

  @Test
  void whenShutdownPropertyIsSetThenShutdownIsCustomized() {
    this.properties.shutdown = (Shutdown.GRACEFUL);
    ConfigurableReactiveWebServerFactory factory = mock(ConfigurableReactiveWebServerFactory.class);
    this.customizer.customize(factory);
    then(factory).should().setShutdown(assertArg(shutdown -> assertThat(shutdown).isEqualTo(Shutdown.GRACEFUL)));
  }

}