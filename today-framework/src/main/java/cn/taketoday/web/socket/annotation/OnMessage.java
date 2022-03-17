/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.socket.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.websocket.Decoder;
import jakarta.websocket.PongMessage;
import jakarta.websocket.Session;

/**
 * This method level annotation can be used to make a Java method receive incoming web socket messages. Each websocket
 * endpoint may only have one message handling method for each of the native websocket message formats: text, binary and pong. Methods
 * using this annotation are allowed to have
 * parameters of types described below, otherwise the container will generate an error at deployment time.
 * <p>The allowed parameters are:
 * <ol>
 * <li>Exactly one of any of the following choices
 * <ul>
 * <li>if the method is handling text messages:
 * <ul>
 * <li> {@link java.lang.String} to receive the whole message</li>
 * <li> Java primitive or class equivalent to receive the whole message converted to that type</li>
 * <li> String and boolean pair to receive the message in parts</li>
 * <li> {@link java.io.Reader} to receive the whole message as a blocking stream</li>
 * <li>any object parameter for which the endpoint has a text decoder ({@link Decoder.Text} or
 * {@link Decoder.TextStream}).</li>
 * </ul>
 * </li>
 * <li>if the method is handling binary messages:
 * <ul>
 * <li> byte[] or {@link java.nio.ByteBuffer} to receive the whole message</li>
 * <li> byte[] and boolean pair, or {@link java.nio.ByteBuffer} and boolean pair to receive the message in parts</li>
 * <li> {@link java.io.InputStream} to receive the whole message as a blocking stream</li>
 * <li> any object parameter for which the endpoint has a binary decoder ({@link Decoder.Binary} or
 * {@link Decoder.BinaryStream}).</li>
 * </ul>
 * </li>
 * <li>if the method is handling pong messages:
 * <ul>
 * <li> {@link PongMessage} for handling pong messages</li>
 * </ul>
 * </li>
 * </ul>
 * </li>
 * <li> and Zero to n String or Java primitive parameters
 * annotated with the {@link jakarta.websocket.server.PathParam} annotation for server endpoints.</li>
 * <li> and an optional {@link Session} parameter</li>
 * </ol>
 * <p/>
 * The parameters may be listed in any order.
 *
 * <p>The method may have a non-void return type, in which case the web socket
 * runtime must interpret this as a web socket message to return to the peer.
 * The allowed data types for this return type, other than void, are String,
 * ByteBuffer, byte[], any Java primitive or class equivalent, and anything for
 * which there is an encoder. If the method uses a Java primitive as a return
 * value, the implementation must construct the text message to send using the
 * standard Java string representation of the Java primitive unless there developer
 * provided encoder for the type configured for this endpoint, in which
 * case that encoder must be used. If the method uses
 * a class equivalent of a Java primitive as a return value, the implementation
 * must construct the text message from the Java primitive equivalent as
 * described above.
 *
 * <p>Developers should
 * note that if developer closes the session during the invocation of a method with a return type, the method will complete but the
 * return value will not be delivered to the remote endpoint. The send failure will be passed back into the endpoint's error handling method.
 *
 * <p>For example:
 * <pre><code>
 * &#64;OnMessage
 * public void processGreeting(String message, Session session) {
 *     System.out.println("Greeting received:" + message);
 * }
 * </code></pre>
 * For example:
 * <pre><code>
 * &#64;OnMessage
 * public void processUpload(byte[] b, boolean last, Session session) {
 *     // process partial data here, which check on last to see if these is more on the way
 * }
 * </code></pre>
 * Developers should not continue to reference message objects of type {@link java.io.Reader}, {@link java.nio.ByteBuffer}
 * or {@link java.io.InputStream} after the annotated method has completed, since they
 * may be recycled by the implementation.
 *
 * @author dannycoward
 * @author TODAY 2021/5/7 13:24
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnMessage {

  /**
   * Specifies the maximum size of message in bytes that the method
   * this annotates will be able to process, or -1 to indicate
   * that there is no maximum. The default is -1. This attribute only
   * applies when the annotation is used to process whole messages, not to
   * those methods that process messages in parts or use a stream or reader
   * parameter to handle the incoming message.
   * If the incoming whole message exceeds this limit, then the implementation
   * generates an error and closes the connection using the reason that
   * the message was too big.
   *
   * @return the maximum size in bytes.
   */
  public long maxMessageSize() default -1;
}
