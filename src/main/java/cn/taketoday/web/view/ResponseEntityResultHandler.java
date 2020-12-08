package cn.taketoday.web.view;

import java.util.List;
import java.util.Map;

import cn.taketoday.web.MessageConverter;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.http.ResponseEntity;
import cn.taketoday.web.view.template.TemplateViewResolver;

/**
 * Handle ResponseEntity
 *
 * @author TODAY
 * @date 2020/12/7 22:46
 * @see ResponseEntity
 */
public class ResponseEntityResultHandler
        extends HandlerMethodResultHandler implements ResultHandler {

  public ResponseEntityResultHandler() {}

  public ResponseEntityResultHandler(TemplateViewResolver viewResolver,
                                     MessageConverter messageConverter,
                                     int downloadFileBuf) {

    setMessageConverter(messageConverter);
    setTemplateViewResolver(viewResolver);
    setDownloadFileBufferSize(downloadFileBuf);
  }

  @Override
  protected boolean supports(final HandlerMethod handler) {
    return handler.is(ResponseEntity.class);
  }

  @Override
  protected void handleInternal(final RequestContext context,
                                final HandlerMethod handler, final Object result) throws Throwable {

    ResponseEntity<?> response = (ResponseEntity<?>) result;
    context.status(response.getStatusCodeValue());

    handleObject(context, response.getBody());
    // apply headers
    for (final Map.Entry<String, List<String>> entry : response.getHeaders().entrySet()) {
      final String headName = entry.getKey();
      for (final String header : entry.getValue()) {
        context.addResponseHeader(headName, header);
      }
    }
  }
}
