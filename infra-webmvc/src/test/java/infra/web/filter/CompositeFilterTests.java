package infra.web.filter;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.web.Filter;
import infra.web.FilterChain;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/6/1 11:01
 */
class CompositeFilterTests {

  @Test
  void compositeFilter() throws Throwable {
    MockFilter targetFilter = new MockFilter();

    CompositeFilter filterProxy = new CompositeFilter();
    filterProxy.setFilters(List.of(targetFilter));

    MockRequestContext context = new MockRequestContext();
    filterProxy.doFilter(context, null);

    assertThat(context.getRequest().getAttribute("called")).isEqualTo(Boolean.TRUE);
  }

  public static class MockFilter implements Filter {

    @Override
    public void doFilter(RequestContext request, FilterChain chain) throws Throwable {
      request.setAttribute("called", Boolean.TRUE);
    }

  }

}