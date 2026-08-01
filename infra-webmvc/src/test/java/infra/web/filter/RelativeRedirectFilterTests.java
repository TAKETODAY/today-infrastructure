package infra.web.filter;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.web.DecoratingHttpContext;
import infra.web.HttpContext;
import infra.web.mock.MockFilterChain;
import infra.web.mock.MockHttpContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/7/25 09:39
 */
class RelativeRedirectFilterTests {

  private final RelativeRedirectFilter filter = new RelativeRedirectFilter();

  private final HttpContext response = mock();

  @Test
  void sendRedirectHttpStatusWhenNullThenIllegalArgumentException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            this.filter.setRedirectStatus(null));
  }

  @Test
  void sendRedirectHttpStatusWhenNot3xxThenIllegalArgumentException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            this.filter.setRedirectStatus(HttpStatus.OK));
  }

  @Test
  void doFilterSendRedirectWhenDefaultsThenLocationAnd303() throws Exception {
    String location = "/foo";
    sendRedirect(location);

    InOrder inOrder = Mockito.inOrder(this.response);
    inOrder.verify(this.response).reset();
    inOrder.verify(this.response).setStatus(HttpStatus.SEE_OTHER.value());
    inOrder.verify(this.response).setHeader(HttpHeaders.LOCATION, location);
    inOrder.verify(this.response).flush();
  }

  @Test
  void doFilterSendRedirectWhenCustomSendRedirectHttpStatusThenLocationAnd301() throws Exception {
    String location = "/foo";
    assertThat(filter.getRedirectStatus()).isSameAs(HttpStatus.SEE_OTHER);

    HttpStatus status = HttpStatus.MOVED_PERMANENTLY;
    this.filter.setRedirectStatus(status);
    sendRedirect(location);

    assertThat(filter.getRedirectStatus()).isSameAs(HttpStatus.MOVED_PERMANENTLY);
    InOrder inOrder = Mockito.inOrder(this.response);
    inOrder.verify(this.response).reset();
    inOrder.verify(this.response).setStatus(status.value());
    inOrder.verify(this.response).setHeader(HttpHeaders.LOCATION, location);
    inOrder.verify(this.response).flush();
  }

  @Test
  void wrapOnceOnly() throws Exception {
    HttpContext original = new MockHttpContext();

    MockFilterChain chain = new MockFilterChain();
    this.filter.doFilter(original, chain);

    HttpContext wrapped1 = chain.getContext();
    assertThat(wrapped1).isNotSameAs(original);

    chain.reset();
    this.filter.doFilter(wrapped1, chain);
    HttpContext current = chain.getContext();
    assertThat(current).isSameAs(wrapped1);

    chain.reset();
    HttpContext wrapped2 = new DecoratingHttpContext(wrapped1);
    this.filter.doFilter(wrapped2, chain);
    current = chain.getContext();
    assertThat(current).isSameAs(wrapped2);
  }

  private void sendRedirect(String location) throws Exception {
    MockFilterChain chain = new MockFilterChain();
    this.filter.doFilter(this.response, chain);

    HttpContext wrappedResponse = chain.getContext();
    wrappedResponse.sendRedirect(location);
  }

}