package cn.taketoday.framework.reactive.websocket;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.framework.reactive.websocket.action.OnClose;
import cn.taketoday.framework.reactive.websocket.action.OnConnect;
import cn.taketoday.framework.reactive.websocket.action.OnError;
import cn.taketoday.framework.reactive.websocket.action.OnMessage;

/**
 * @author WangYi
 * @since 2020/8/13
 */
public class WebSocketWrapper implements WebSocketChannel {
  private static final Logger log = LoggerFactory.getLogger(WebSocketWrapper.class);

  private final MethodHandles.Lookup lookup = MethodHandles.lookup();
  private final Map<Class<? extends Annotation>, Method> methodCache;
  private Class<?> webSocketClass;

  public WebSocketWrapper() {
    this.methodCache = new HashMap<>();
  }

  public void initialize(Class<?> webSocketClass) {
    this.webSocketClass = webSocketClass;
    Method[] methods = webSocketClass.getMethods();
    if (methods.length == 0) {
      return;
    }
    final Map<Class<? extends Annotation>, Method>
            cache = new HashMap<>(4);

    cacheMethod(cache, methods, OnClose.class);
    cacheMethod(cache, methods, OnConnect.class);
    cacheMethod(cache, methods, OnError.class);
    cacheMethod(cache, methods, OnMessage.class);

    if (cache.isEmpty()) {
      throw new RuntimeException("Do not found any annotation of " +
                                         "[@OnClose / @OnConnect / @OnError /@OnMessage] in class: " + webSocketClass.getName());
    }
    this.methodCache.putAll(cache);
  }

  private void cacheMethod(Map<Class<? extends Annotation>, Method> cache, Method[] methods,
                           Class<? extends Annotation> filter) {
    List<Method> methodList = Stream.of(methods)
            .filter(method -> method.isAnnotationPresent(filter))
            .collect(Collectors.toList());

    if (methodList.size() == 1) {
      cache.put(filter, methodList.get(0));
    }
    else if (methodList.size() > 1) {
      throw new RuntimeException("Duplicate annotation @" + filter.getSimpleName()
                                         + " in class: " + methodList.get(0).getDeclaringClass().getName());
    }
  }

  @Override
  public void onConnect(WebSocketContext webSocketContext) {
    invokeAction(OnConnect.class, webSocketContext);
  }

  @Override
  public void onMessage(WebSocketContext webSocketContext) {
    invokeAction(OnMessage.class, webSocketContext);
  }

  @Override
  public void onClose(WebSocketContext webSocketContext) {
    invokeAction(OnClose.class, webSocketContext);
  }

  @Override
  public void onError(WebSocketContext webSocketContext) {
    invokeAction(OnError.class, webSocketContext);
  }

  private void invokeAction(Class<? extends Annotation> actionAnnotation, WebSocketContext webSocketContext) {
    if (!methodCache.containsKey(actionAnnotation)) {
      return;
    }
    Method method = methodCache.get(actionAnnotation);
    try {
      final List<Object> invokeArguments = new ArrayList<>();
      for (Parameter parameter : method.getParameters()) {
        invokeArguments.add(parameter.getName());
      }
      this.lookup.unreflect(method).bindTo(webSocketClass.newInstance())
              .invokeWithArguments(invokeArguments);
    }
    catch (Throwable e) {
      log.error("An exception occurred when obtaining invoke param", e);
      try {
        webSocketContext.requestContext().flush();
      }
      catch (IOException ioException) {
        ioException.printStackTrace();
      }
    }
  }
}
