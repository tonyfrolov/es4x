/*
 * Copyright 2018 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */
package io.reactiverse.es4x.impl.future;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.Objects;

class ES4XFuture<T> implements Promise<T>, Future<T>, Thenable {

  private static final Logger LOG = LoggerFactory.getLogger(ES4XFuture.class);

  private boolean failed;
  private boolean succeeded;
  private Handler<AsyncResult<T>> handler;
  private T result;
  private Throwable throwable;

  /**
   * Create a future that hasn't completed yet
   */
  ES4XFuture() {
  }

  /**
   * The result of the operation. This will be null if the operation failed.
   */
  public synchronized T result() {
    return result;
  }

  /**
   * An exception describing failure. This will be null if the operation succeeded.
   */
  public synchronized Throwable cause() {
    return throwable;
  }

  /**
   * Did it succeeed?
   */
  public synchronized boolean succeeded() {
    return succeeded;
  }

  /**
   * Did it fail?
   */
  public synchronized boolean failed() {
    return failed;
  }

  /**
   * Has it completed?
   */
  public synchronized boolean isComplete() {
    return failed || succeeded;
  }

  @Override
  public Future<T> onComplete(Handler<AsyncResult<T>> handler) {
    Objects.requireNonNull(handler, "No null handler accepted");
    synchronized (this) {
      if (!isComplete()) {
        if (this.handler == null) {
          this.handler = handler;
        } else {
          addHandler(handler);
        }
        return this;
      }
    }
    dispatch(handler);
    return this;
  }

  private void addHandler(Handler<AsyncResult<T>> h) {
    Handlers<T> handlers;
    if (handler instanceof Handlers) {
      handlers = (Handlers<T>) handler;
    } else {
      handlers = new Handlers<>();
      handlers.add(handler);
      handler = handlers;
    }
    handlers.add(h);
  }

  protected void dispatch(Handler<AsyncResult<T>> handler) {
    if (handler instanceof Handlers) {
      for (Handler<AsyncResult<T>> h : (Handlers<T>)handler) {
        h.handle(this);
      }
    } else {
      handler.handle(this);
    }
  }

  @Override
  public void then(Value onFulfilled, Value onRejected) {
    // Both onFulfilled and onRejected are optional arguments

    onComplete(ar -> {
      if (ar.succeeded()) {
        try {
          if (onFulfilled != null) {
            onFulfilled.executeVoid(ar.result());
          }
        } catch (RuntimeException e) {
          // resolve failed, attempt to reject
          if (onRejected != null) {
            onRejected.execute(e);
          } else {
            LOG.warn("Possible Unhandled Promise Rejection: " + e.getMessage());
          }
        }
      } else {
        if (onRejected != null) {
          onRejected.execute(ar.cause());
        }
      }
    });
  }

  @Override
  public void complete(T result) {
    if (!tryComplete(result)) {
      throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
    }
  }

  @Override
  public void complete() {
    if (!tryComplete()) {
      throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
    }
  }

  @Override
  public void fail(Throwable cause) {
    if (!tryFail(cause)) {
      throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
    }
  }

  @Override
  public void fail(String failureMessage) {
    if (!tryFail(failureMessage)) {
      throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
    }
  }

  @Override
  public boolean tryComplete(T result) {
    Handler<AsyncResult<T>> h;
    synchronized (this) {
      if (succeeded || failed) {
        return false;
      }
      this.result = result;
      succeeded = true;
      h = handler;
      handler = null;
    }
    if (h != null) {
      h.handle(this);
    }
    return true;
  }

  @Override
  public boolean tryComplete() {
    return tryComplete(null);
  }

  public void handle(Future<T> ar) {
    if (ar.succeeded()) {
      complete(ar.result());
    } else {
      fail(ar.cause());
    }
  }

  @Override
  public void handle(AsyncResult<T> asyncResult) {
    if (asyncResult.succeeded()) {
      complete(asyncResult.result());
    } else {
      fail(asyncResult.cause());
    }
  }

  @Override
  public boolean tryFail(Throwable cause) {
    Handler<AsyncResult<T>> h;
    synchronized (this) {
      if (succeeded || failed) {
        return false;
      }
      this.throwable = cause != null ? cause : new NoStackTraceThrowable(null);
      failed = true;
      h = handler;
      handler = null;
    }
    if (h != null) {
      h.handle(this);
    }
    return true;
  }

  @Override
  public boolean tryFail(String failureMessage) {
    return tryFail(new NoStackTraceThrowable(failureMessage));
  }

  @Override
  public Future<T> future() {
    return this;
  }

  @Override
  public String toString() {
    synchronized (this) {
      if (succeeded) {
        return "Future{result=" + result + "}";
      }
      if (failed) {
        return "Future{cause=" + throwable.getMessage() + "}";
      }
      return "Future{unresolved}";
    }
  }

  private class Handlers<T> extends ArrayList<Handler<AsyncResult<T>>> implements Handler<AsyncResult<T>> {
    @Override
    public void handle(AsyncResult<T> res) {
      for (Handler<AsyncResult<T>> handler : this) {
        handler.handle(res);
      }
    }
  }
}
