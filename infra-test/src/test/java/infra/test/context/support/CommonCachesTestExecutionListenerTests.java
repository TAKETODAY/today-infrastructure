package infra.test.context.support;

import org.junit.jupiter.api.Test;

import infra.context.support.AbstractApplicationContext;
import infra.test.context.TestContext;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/3/29 21:30
 */
class CommonCachesTestExecutionListenerTests {

  private final CommonCachesTestExecutionListener listener = new CommonCachesTestExecutionListener();

  @Test
  void afterTestClassWhenContextIsAvailable() throws Exception {
    AbstractApplicationContext applicationContext = mock();
    TestContext testContext = mock(TestContext.class);
    given(testContext.hasApplicationContext()).willReturn(true);
    given(testContext.getApplicationContext()).willReturn(applicationContext);
    listener.afterTestClass(testContext);
    verify(applicationContext).clearResourceCaches();
  }

  @Test
  void afterTestClassCWhenContextIsNotAvailable() throws Exception {
    TestContext testContext = mock();
    given(testContext.hasApplicationContext()).willReturn(false);
    listener.afterTestClass(testContext);
    verify(testContext).hasApplicationContext();
    verifyNoMoreInteractions(testContext);
  }

}