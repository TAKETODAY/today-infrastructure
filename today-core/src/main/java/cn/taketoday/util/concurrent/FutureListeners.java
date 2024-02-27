/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.util.concurrent;

import java.util.ArrayList;
import java.util.Arrays;

import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/26 17:30
 */
final class FutureListeners {

  public FutureListener<? extends ListenableFuture<?>>[] listeners;

  @Nullable
  public Object progressiveListeners;

  public int size;

  @SuppressWarnings("unchecked")
  FutureListeners(FutureListener<? extends ListenableFuture<?>> first, FutureListener<? extends ListenableFuture<?>> second) {
    listeners = new FutureListener[2];
    listeners[0] = first;
    listeners[1] = second;
    size = 2;
    if (first instanceof ProgressiveFutureListener) {
      if (second instanceof ProgressiveFutureListener) {
        progressiveListeners = new ProgressiveFutureListener[] {
                (ProgressiveFutureListener<?>) first, (ProgressiveFutureListener<?>) second
        };
      }
      else {
        progressiveListeners = first;
      }
    }
    else if (second instanceof ProgressiveFutureListener) {
      progressiveListeners = second;
    }
  }

  public void add(FutureListener<? extends ListenableFuture<?>> l) {
    var listeners = this.listeners;
    final int size = this.size;
    if (size == listeners.length) {
      this.listeners = listeners = Arrays.copyOf(listeners, size << 1);
    }
    listeners[size] = l;
    this.size = size + 1;

    if (l instanceof ProgressiveFutureListener<?> pfl) {
      Object progressive = this.progressiveListeners;
      if (progressive instanceof ProgressiveFutureListener) {
        this.progressiveListeners = new ProgressiveFutureListener[] {
                (ProgressiveFutureListener<?>) progressive, pfl
        };
      }
      else if (progressive instanceof ProgressiveFutureListener<?>[] array) {
        var newArr = new ProgressiveFutureListener<?>[array.length + 1];
        System.arraycopy(array, 0, newArr, 0, array.length);
        newArr[array.length] = pfl;
        this.progressiveListeners = newArr;
      }
      else {
        this.progressiveListeners = pfl;
      }
    }
  }

  public void remove(FutureListener<? extends ListenableFuture<?>> l) {
    final var listeners = this.listeners;
    int size = this.size;
    for (int i = 0; i < size; i++) {
      if (listeners[i] == l) {
        int listenersToMove = size - i - 1;
        if (listenersToMove > 0) {
          System.arraycopy(listeners, i + 1, listeners, i, listenersToMove);
        }
        listeners[--size] = null;
        this.size = size;

        if (l instanceof ProgressiveFutureListener) {
          Object progressive = this.progressiveListeners;
          if (l == progressive) {
            this.progressiveListeners = null;
          }
          else if (progressive instanceof ProgressiveFutureListener<?>[] array) {
            ArrayList<ProgressiveFutureListener<?>> list = new ArrayList<>(array.length);
            for (ProgressiveFutureListener<?> listener : array) {
              if (listener != l) {
                list.add(listener);
              }
            }
            if (list.size() == 1) {
              this.progressiveListeners = list.get(0);
            }
            else {
              this.progressiveListeners = list.toArray(new ProgressiveFutureListener<?>[list.size()]);
            }
          }
        }
        return;
      }
    }
  }

}
