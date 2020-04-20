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
package io.reactiverse.es4x.impl;

import io.reactiverse.es4x.ESVerticleFactory;
import io.reactiverse.es4x.Runtime;
import io.vertx.core.*;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public final class JSVerticleFactory extends ESVerticleFactory {

  @Override
  public String prefix() {
    return "js";
  }

  @Override
  protected Verticle createVerticle(Runtime runtime, String fsVerticleName) {

    return new Verticle() {

      private Vertx vertx;
      private Context context;

      @Override
      public Vertx getVertx() {
        return vertx;
      }

      @Override
      public void init(Vertx vertx, Context context) {
        this.vertx = vertx;
        this.context = context;
      }

      @Override
      public void start(Future<Void> startFuture) {
        final String address;
        final boolean worker;
        final Value self;

        if (context != null) {
          address = context.deploymentID();
          worker = context.isWorkerContext();
          // expose config
          if (context.config() != null) {
            runtime.config(context.config());
          }
        } else {
          worker = false;
          address = null;
        }

        // this can take some time to load so it might block the event loop
        // this is usually not a issue as it is a one time operation
        try {
          runtime.enter();
          if (worker) {
            final Value undefined = runtime.eval("[undefined]").getArrayElement(0);
            // workers will follow the browser semantics, they will have an extra global "postMessage"
            runtime.put("postMessage", (ProxyExecutable) arguments -> {
              // this implementation is not totally correct as it should be
              // a shallow copy not a full encode/decode of JSON payload, however
              // this works better in vert.x as we can be interacting with any
              // polyglot language or across the cluster
              vertx.eventBus()
                .send(
                  address + ".in",
                  runtime.get("JSON").invokeMember("stringify", arguments[0]).asString());

              return undefined;
            });
          }
          self = runtime.get("require").execute(mainScript(fsVerticleName));
        } catch (RuntimeException e) {
          startFuture.fail(e);
          return;
        } finally {
          runtime.leave();
        }

        if (self != null) {
          if (worker) {
            final Value onmessage = self.hasMember("onmessage") ? self.getMember("onmessage") : runtime.get("onmessage");
            // if it is a worker and there is a onmessage handler we need to bind it to the eventbus
            if (onmessage != null && onmessage.canExecute()) {
              try {
                // if the worker has specified a onmessage function we need to bind it to the eventbus
                final Value JSON = runtime.eval("JSON");

                vertx.eventBus().consumer(address + ".out", msg -> {
                  // parse the json back to the engine runtime type
                  Value json = JSON.invokeMember("parse", msg.body());
                  // deliver it to the handler
                  onmessage.executeVoid(json);
                });
              } catch (RuntimeException e) {
                startFuture.fail(e);
                return;
              }
            }
          }
        }

        startFuture.complete();
      }

      @Override
      public void stop(Future<Void> stopFuture) {
        try {
          runtime.enter();
          runtime.emit("undeploy");
          stopFuture.complete();
        } catch (RuntimeException e) {
          stopFuture.fail(e);
        } finally {
          runtime.leave();
          runtime.close();
        }
      }
    };
  }
}
