package com.orientechnologies.orient.server.distributed.impl.structural;

import com.orientechnologies.orient.core.db.OrientDBInternal;
import com.orientechnologies.orient.server.distributed.impl.coordinator.*;
import com.orientechnologies.orient.server.distributed.impl.coordinator.transaction.OSessionOperationId;
import org.junit.Test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OStructuralCoordinatorTest {

  @Test
  public void simpleOperationTest() throws InterruptedException {
    CountDownLatch responseReceived = new CountDownLatch(1);
    OOperationLog operationLog = new MockOperationLog();

    OStructuralCoordinator coordinator = new OStructuralCoordinator(Executors.newSingleThreadExecutor(), operationLog);
    MockDistributedChannel channel = new MockDistributedChannel();
    channel.coordinator = coordinator;
    OStructuralDistributedMember one = new OStructuralDistributedMember("one", channel);
    channel.member = one;
    coordinator.join(one);

    coordinator.submit(one, new OSessionOperationId(), new OStructuralSubmitRequest() {
      @Override
      public void begin(OStructuralDistributedMember member, OSessionOperationId operationId, OStructuralCoordinator coordinator) {
        MockNodeRequest nodeRequest = new MockNodeRequest();
        coordinator.sendOperation(this, nodeRequest, new OStructuralResponseHandler() {
          @Override
          public boolean receive(OStructuralCoordinator coordinator, OStructuralRequestContext context,
              OStructuralDistributedMember member, OStructuralNodeResponse response) {
            if (context.getResponses().size() == 1) {
              responseReceived.countDown();
            }
            return context.getResponses().size() == context.getInvolvedMembers().size();
          }

          @Override
          public boolean timeout(OStructuralCoordinator coordinator, OStructuralRequestContext context) {
            return true;
          }
        });
      }

      @Override
      public void serialize(DataOutput output) {

      }

      @Override
      public void deserialize(DataInput input) {

      }

      @Override
      public int getRequestType() {
        return 0;
      }
    });
    assertTrue(responseReceived.await(1, TimeUnit.SECONDS));
    coordinator.close();
    assertEquals(0, coordinator.getContexts().size());
  }

  @Test
  public void testTwoPhase() throws InterruptedException {
    CountDownLatch responseReceived = new CountDownLatch(1);
    OOperationLog operationLog = new MockOperationLog();

    OStructuralCoordinator coordinator = new OStructuralCoordinator(Executors.newSingleThreadExecutor(), operationLog);
    MockDistributedChannel channel = new MockDistributedChannel();
    channel.coordinator = coordinator;
    channel.reply = responseReceived;
    OStructuralDistributedMember one = new OStructuralDistributedMember("one", channel);
    channel.member = one;
    coordinator.join(one);

    coordinator.submit(one, new OSessionOperationId(), new OStructuralSubmitRequest() {
      @Override
      public void begin(OStructuralDistributedMember member, OSessionOperationId operationId, OStructuralCoordinator coordinator) {
        MockNodeRequest nodeRequest = new MockNodeRequest();
        coordinator.sendOperation(this, nodeRequest, new OStructuralResponseHandler() {
          @Override
          public boolean receive(OStructuralCoordinator coordinator, OStructuralRequestContext context,
              OStructuralDistributedMember member, OStructuralNodeResponse response) {
            if (context.getResponses().size() == 1) {
              coordinator.sendOperation(null, new OStructuralNodeRequest() {
                @Override
                public OStructuralNodeResponse execute(OStructuralDistributedMember nodeFrom, OLogId opId,
                    OStructuralDistributedExecutor executor, OrientDBInternal context) {
                  return null;
                }

                @Override
                public void serialize(DataOutput output) {

                }

                @Override
                public void deserialize(DataInput input) {

                }

                @Override
                public int getRequestType() {
                  return 0;
                }
              }, new OStructuralResponseHandler() {
                @Override
                public boolean receive(OStructuralCoordinator coordinator, OStructuralRequestContext context,
                    OStructuralDistributedMember member, OStructuralNodeResponse response) {
                  if (context.getResponses().size() == 1) {
                    member.reply(new OSessionOperationId(), new OStructuralSubmitResponse() {
                      @Override
                      public void serialize(DataOutput output) throws IOException {

                      }

                      @Override
                      public void deserialize(DataInput input) throws IOException {

                      }

                      @Override
                      public int getResponseType() {
                        return 0;
                      }
                    });
                  }
                  return context.getResponses().size() == context.getInvolvedMembers().size();
                }

                @Override
                public boolean timeout(OStructuralCoordinator coordinator, OStructuralRequestContext context) {
                  return true;
                }
              });
            }
            return context.getResponses().size() == context.getInvolvedMembers().size();
          }

          @Override
          public boolean timeout(OStructuralCoordinator coordinator, OStructuralRequestContext context) {
            return true;
          }
        });
      }

      @Override
      public void serialize(DataOutput output) {

      }

      @Override
      public void deserialize(DataInput input) {

      }

      @Override
      public int getRequestType() {
        return 0;
      }
    });

    assertTrue(responseReceived.await(1, TimeUnit.SECONDS));
    coordinator.close();
    assertEquals(0, coordinator.getContexts().size());
  }

  @Test
  public void simpleTimeoutTest() throws InterruptedException {
    CountDownLatch timedOut = new CountDownLatch(1);
    OOperationLog operationLog = new MockOperationLog();

    OStructuralCoordinator coordinator = new OStructuralCoordinator(Executors.newSingleThreadExecutor(), operationLog);
    MockDistributedChannel channel = new MockDistributedChannel();
    channel.coordinator = coordinator;
    OStructuralDistributedMember one = new OStructuralDistributedMember("one", channel);
    channel.member = one;
    coordinator.join(one);

    coordinator.submit(one, new OSessionOperationId(), new OStructuralSubmitRequest() {
      @Override
      public void begin(OStructuralDistributedMember member, OSessionOperationId operationId, OStructuralCoordinator coordinator) {
        MockNodeRequest nodeRequest = new MockNodeRequest();
        coordinator.sendOperation(this, nodeRequest, new OStructuralResponseHandler() {
          @Override
          public boolean receive(OStructuralCoordinator coordinator, OStructuralRequestContext context,
              OStructuralDistributedMember member, OStructuralNodeResponse response) {
            return false;
          }

          @Override
          public boolean timeout(OStructuralCoordinator coordinator, OStructuralRequestContext context) {
            timedOut.countDown();
            return true;
          }
        });
      }

      @Override
      public void serialize(DataOutput output) throws IOException {

      }

      @Override
      public void deserialize(DataInput input) throws IOException {

      }

      @Override
      public int getRequestType() {
        return 0;
      }
    });

    //This is 2 seconds because timeout is hard coded with 1 sec now
    assertTrue(timedOut.await(2, TimeUnit.SECONDS));
    coordinator.close();
    assertEquals(0, coordinator.getContexts().size());
  }

  private static class MockDistributedChannel implements ODistributedChannel {
    public OStructuralCoordinator       coordinator;
    public CountDownLatch               reply;
    public OStructuralDistributedMember member;

    @Override
    public void sendRequest(String database, OLogId id, ONodeRequest request) {
    }

    @Override
    public void sendResponse(String database, OLogId id, ONodeResponse nodeResponse) {

    }

    @Override
    public void sendResponse(OLogId opId, OStructuralNodeResponse response) {
      assertTrue(false);
    }

    @Override
    public void sendRequest(OLogId id, OStructuralNodeRequest request) {
      coordinator.receive(member, id, new OStructuralNodeResponse() {
        @Override
        public void serialize(DataOutput output) throws IOException {

        }

        @Override
        public void deserialize(DataInput input) throws IOException {

        }

        @Override
        public int getResponseType() {
          return 0;
        }
      });

    }

    @Override
    public void reply(OSessionOperationId operationId, OStructuralSubmitResponse response) {
      reply.countDown();
    }

    @Override
    public void submit(OSessionOperationId operationId, OStructuralSubmitRequest request) {

    }

    @Override
    public void submit(String database, OSessionOperationId operationId, OSubmitRequest request) {

    }

    @Override
    public void reply(String database, OSessionOperationId operationId, OSubmitResponse response) {

    }
  }

  private static class MockNodeRequest implements OStructuralNodeRequest {

    @Override
    public OStructuralNodeResponse execute(OStructuralDistributedMember nodeFrom, OLogId opId,
        OStructuralDistributedExecutor executor, OrientDBInternal context) {
      return null;
    }

    @Override
    public void serialize(DataOutput output) throws IOException {

    }

    @Override
    public void deserialize(DataInput input) throws IOException {

    }

    @Override
    public int getRequestType() {
      return 0;
    }
  }
}
