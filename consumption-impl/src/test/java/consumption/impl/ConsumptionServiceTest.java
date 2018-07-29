package consumption.impl;

import static com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup;
import static com.lightbend.lagom.javadsl.testkit.ServiceTest.withServer;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import consumption.api.*;
import org.junit.Test;

public class ConsumptionServiceTest
{
  @Test
  public void testNoValueStored() throws Exception
  {
    withServer(defaultSetup().withCassandra(), server -> {
      ConsumptionService service = server.client(ConsumptionService.class);

      int value1 = service.query()
                              .invoke(new ConsumptionQuery("consumer-1",
                                                           ConsumerEntityTest.createDateTimeString(0, 0)))
                              .toCompletableFuture()
                              .get(5, SECONDS);
      assertEquals(0, value1); // no value stored.
    });
  }

  @Test
  public void shouldStoreConsumption() throws Exception {
    withServer(defaultSetup().withCassandra(), server -> {
      ConsumptionService service = server.client(ConsumptionService.class);
      service.ingest()
             .invoke(new Consumption("consumer-1", ConsumerEntityTest.createDateTimeString(0, 10),
                                     123))
             .toCompletableFuture()
             .get(5, SECONDS);
      int value1 = service.query()
                          .invoke(new ConsumptionQuery("consumer-1",
                                                       ConsumerEntityTest.createDateTimeString(0, 0)))
                          .toCompletableFuture()
                          .get(5, SECONDS);
      assertEquals(123, value1);

      // ensure only consumer 1 has consumption stored.
      int value2 = service.query()
                          .invoke(new ConsumptionQuery("consumer-2",
                                                       ConsumerEntityTest.createDateTimeString(0, 0)))
                          .toCompletableFuture()
                          .get(5, SECONDS);
      assertEquals(0, value2);
    });
  }

}
