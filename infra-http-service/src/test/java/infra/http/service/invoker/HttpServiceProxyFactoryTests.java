package infra.http.service.invoker;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import infra.http.service.annotation.GetExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/2/17 16:44
 */
class HttpServiceProxyFactoryTests {

  @Test
  void proxyFactoryCustomizer() {
    var interceptor = (MethodInterceptor) invocation -> "intercepted";
    var proxyFactory = HttpServiceProxyFactory.forAdapter(mock(HttpExchangeAdapter.class))
            .proxyFactoryCustomizer((factory, serviceType) -> factory.addAdvice(0, interceptor))
            .build();

    String result = proxyFactory.createClient(Service.class).execute();

    assertThat(result).isEqualTo("intercepted");
  }

  private interface Service {

    @GetExchange
    String execute();
  }

}