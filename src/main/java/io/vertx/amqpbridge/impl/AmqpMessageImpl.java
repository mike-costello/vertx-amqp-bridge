/*
* Copyright 2016 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.vertx.amqpbridge.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonHelper;

public class AmqpMessageImpl implements Message<JsonObject> {

  private final JsonObject body;
  private final AmqpBridgeImpl bridge;
  private final org.apache.qpid.proton.message.Message protonMessage;
  private final ProtonDelivery delivery;
  private final String amqpAddress;
  private final String amqpReplyAddress;

  public AmqpMessageImpl(JsonObject body, AmqpBridgeImpl bridge, org.apache.qpid.proton.message.Message protonMessage,
      ProtonDelivery delivery, String amqpAddress, String amqpReplyAddress) {
    this.body = body;
    this.bridge = bridge;
    this.protonMessage = protonMessage;
    this.delivery = delivery;
    this.amqpAddress = amqpAddress;
    this.amqpReplyAddress = amqpReplyAddress;
  }

  @Override
  public String address() {
    return amqpAddress;
  }

  @Override
  public MultiMap headers() {
    throw new UnsupportedOperationException(
        "Use the AMQP application-properties section via the JsonObject payload body, headers method is not supported");
  }

  @Override
  public JsonObject body() {
    return body;
  }

  @Override
  public String replyAddress() {
    return amqpReplyAddress;
  }

  @Override
  public boolean isSend() {
    // EventBus 'send vs publish' is semantically different than in AMQP, where the node at a given
    // address governs behaviour but addresses can be arbitrary. Just return true to say it isSend.
    return true;
  }

  private <R> void doReply(Object replyMessageBody, Handler<AsyncResult<Message<R>>> replyHandler) {
    if(!(replyMessageBody instanceof JsonObject)) {
      throw new IllegalArgumentException("The reply body must be an instance of JsonObject");
    }

    bridge.sendReply(protonMessage, (JsonObject) replyMessageBody, replyHandler);
  }

  @Override
  public void reply(Object replyMessageBody) {
    doReply(replyMessageBody, null);
  }

  @Override
  public <R> void reply(Object replyMessageBody, Handler<AsyncResult<Message<R>>> replyHandler) {
    doReply(replyMessageBody, replyHandler);
  }

  @Override
  public void reply(Object messageBody, DeliveryOptions options) {
    throw new UnsupportedOperationException("DeliveryOptions are not supported");
  }

  @Override
  public <R> void reply(Object messageBody, DeliveryOptions options, Handler<AsyncResult<Message<R>>> replyHandler) {
    throw new UnsupportedOperationException("DeliveryOptions are not supported");
  }

  @Override
  public void fail(int failureCode, String message) {
    throw new UnsupportedOperationException("Implicit failure responses are not supported, send a message explicitly.");
  }

  void accept() {
    ProtonHelper.accepted(delivery, true);
  }
}
