package consumption.impl;

import akka.Done;
import akka.japi.Pair;
import consumption.api.*;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.broker.TopicProducer;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRef;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Implementation of the {@link ConsumptionService}.
 */
public class ConsumptionServiceImpl implements ConsumptionService
{

  private final PersistentEntityRegistry persistentEntityRegistry;

  @Inject
  public ConsumptionServiceImpl(PersistentEntityRegistry persistentEntityRegistry)
  {
    this.persistentEntityRegistry = persistentEntityRegistry;
    persistentEntityRegistry.register(ConsumerEntity.class);
  }

  @Override
  public ServiceCall<ConsumptionQuery, Integer> query()
  {
    return request -> {
      // Look up the consumer entity for the given ID.
      PersistentEntityRef<ConsumerCommand> ref = persistentEntityRegistry.refFor(ConsumerEntity.class,
                                                                                 request.getConsumerId());
      // Ask the entity the GetConsumption command.
      return ref.ask(new ConsumerCommand.GetConsumption(request.getTimestamp()));
    };
  }

  @Override
  public ServiceCall<Consumption, Done> ingest()
  {
    return request -> {
      System.out.println("Ingesting " + request);
      // Look up the Consumer entity for the given ID.
      PersistentEntityRef<ConsumerCommand> ref =
        persistentEntityRegistry.refFor(ConsumerEntity.class, request.consumerId);
      // Tell the entity to update its consumption
      return ref.ask(new ConsumerCommand.RecordConsumption(request.timestamp, request.consumption));
    };

  }

  @Override
  public Topic<consumption.api.ConsumerEvent> consumerEvents()
  {
    // We want to publish all the shards of the ingestion event
    return TopicProducer.taggedStreamWithOffset(
      ConsumerEvent.TAG.allTags(),
      (tag, offset) ->

        // Load the event stream for the passed in shard tag
        persistentEntityRegistry.eventStream(tag, offset)
                                .map(eventAndOffset -> {

                                  // Now we want to convert from the persisted event to the published event.
                                  // Although these two events are currently identical, in future they may
                                  // change and need to evolve separately, by separating them now we save
                                  // a lot of potential trouble in future.
                                  consumption.api.ConsumerEvent eventToPublish;

                                  if (eventAndOffset.first() instanceof ConsumerEvent.ConsumptionRecorded) {
                                    ConsumerEvent.ConsumptionRecorded consumptionRecorded =
                                      (ConsumerEvent.ConsumptionRecorded)eventAndOffset.first();
                                    eventToPublish =
                                      new consumption.api.ConsumerEvent.ConsumptionRecorded(
                                        consumptionRecorded.getConsumerId(),
                                        consumptionRecorded.getTimestamp(),
                                        consumptionRecorded.getConsumption()
                                      );
                                  } else {
                                    throw new IllegalArgumentException(
                                      "Unknown event: " +
                                      eventAndOffset.first());
                                  }

                                  // We return a pair of the translated event, and its offset, so that
                                  // Lagom can track which offsets have been published.
                                  return Pair.create(eventToPublish,
                                                     eventAndOffset.second());
                                })
    );
  }
}
