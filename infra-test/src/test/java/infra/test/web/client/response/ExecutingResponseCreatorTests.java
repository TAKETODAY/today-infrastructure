package infra.test.web.client.response;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import infra.http.HttpMethod;
import infra.http.client.ClientHttpRequest;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpResponse;
import infra.mock.http.client.MockClientHttpRequest;
import infra.mock.http.client.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/4/5 17:11
 */
class ExecutingResponseCreatorTests {

  @Test
  void ensureRequestNotNull() {
    ExecutingResponseCreator responseCreator = new ExecutingResponseCreator((uri, method) -> null);

    assertThatIllegalStateException()
            .isThrownBy(() -> responseCreator.createResponse(null))
            .withMessage("Expected a MockClientHttpRequest");
  }

  @Test
  void ensureRequestIsMock() {
    ExecutingResponseCreator responseCreator = new ExecutingResponseCreator((uri, method) -> null);
    ClientHttpRequest mockitoMockRequest = mock();

    assertThatIllegalStateException()
            .isThrownBy(() -> responseCreator.createResponse(mockitoMockRequest))
            .withMessage("Expected a MockClientHttpRequest");
  }

  @Test
  void requestIsCopied() throws IOException {
    MockClientHttpRequest originalRequest = new MockClientHttpRequest(HttpMethod.POST, "https://example.org");
    originalRequest.getHeaders().add("X-example", "original");
    originalRequest.getBody().write("original body".getBytes(StandardCharsets.UTF_8));

    MockClientHttpResponse originalResponse = new MockClientHttpResponse(new byte[0], 500);
    List<MockClientHttpRequest> factoryRequests = new ArrayList<>();
    ClientHttpRequestFactory originalFactory = (uri, httpMethod) -> {
      MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri);
      request.setResponse(originalResponse);
      factoryRequests.add(request);
      return request;
    };

    ExecutingResponseCreator responseCreator = new ExecutingResponseCreator(originalFactory);
    ClientHttpResponse response = responseCreator.createResponse(originalRequest);

    assertThat(response).as("response").isSameAs(originalResponse);
    assertThat(originalRequest.isExecuted()).as("originalRequest.isExecuted").isFalse();

    assertThat(factoryRequests)
            .hasSize(1)
            .first()
            .isNotSameAs(originalRequest)
            .satisfies(request -> {
              assertThat(request.isExecuted()).isTrue();
              assertThat(request.getBody()).isNotSameAs(originalRequest.getBody());
              assertThat(request.getHeaders()).isNotSameAs(originalRequest.getHeaders());
            });
  }

}