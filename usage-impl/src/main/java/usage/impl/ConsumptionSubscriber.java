package usage.impl;

import akka.Done;
import akka.stream.javadsl.Flow;
import consumption.api.ConsumerEvent;
import consumption.api.ConsumptionService;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * This subscribes to the ConsumptionService event stream.
 */
public class ConsumptionSubscriber
{
  @Inject
  public ConsumptionSubscriber(ConsumptionService consumptionService, UsageRepository repository)
  {
    // Create a subscriber
    consumptionService.consumerEvents().subscribe()
                      // And subscribe to it with at least once processing semantics.
                      .atLeastOnce(
                        // Create a flow that emits a Done for each message it processes
                        Flow.<ConsumerEvent>create().mapAsync(1, event -> {
                          System.out.println("Usage Service received " + event);
                          if (event instanceof ConsumerEvent.ConsumptionRecorded) {
                            ConsumerEvent.ConsumptionRecorded consumptionRecorded =
                              (ConsumerEvent.ConsumptionRecorded)event;
                            // Update the message
                            LocalDateTime timestamp = LocalDateTime.parse(consumptionRecorded.getTimestamp());
                            return repository.updateUsage(consumptionRecorded.getConsumerId(),
                                                          timestamp,
                                                          consumptionRecorded.getConsumption());
                          } else {
                            // Ignore all other events
                            return CompletableFuture.completedFuture(Done.getInstance());
                          }
                        })
                      );

  }
}
