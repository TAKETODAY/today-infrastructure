/*
 * MIT License
 *
 * Copyright (c) 2019 1619kHz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package test.demo.ws;

import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.framework.reactive.websocket.WebSocket;
import cn.taketoday.framework.reactive.websocket.WebSocketContext;
import cn.taketoday.framework.reactive.websocket.action.OnClose;
import cn.taketoday.framework.reactive.websocket.action.OnConnect;
import cn.taketoday.framework.reactive.websocket.action.OnError;
import cn.taketoday.framework.reactive.websocket.action.OnMessage;
import cn.taketoday.web.annotation.RequestParam;

/**
 * @author WangYi
 * @since 2020/7/13
 */
@Singleton
@WebSocket("/wrapper")
public class WebSocketWrapper {

  @OnConnect
  public void onConnect(WebSocketContext webSocketContext, @RequestParam String name) {
    System.out.println("websocket open connect:" + name);
  }

  @OnMessage
  public void onMessage(WebSocketContext webSocketContext) {
    System.out.println(webSocketContext.getMessage().text());
  }

  @OnClose
  public void onClose(WebSocketContext webSocketContext) {
    System.out.println("关闭连接:" + webSocketContext.session().getId());
  }

  @OnError
  public void onError(WebSocketContext webSocketContext) {

  }
}
