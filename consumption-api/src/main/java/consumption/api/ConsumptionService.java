package consumption.api;

import akka.Done;
import com.lightbend.lagom.javadsl.api.*;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.api.broker.kafka.KafkaProperties;

import static com.lightbend.lagom.javadsl.api.Service.*;

/**
 * The Consumption Ingestion service interface.
 * <p>
 * This describes everything that Lagom needs to know about how to serve and
 * consume ConsumptionMessages.
 */
public interface ConsumptionService extends Service
{

  /**
   * Example: curl -H "Content-Type: application/json" -X POST -d '{"consumerId" : "ABC123", "timestamp" : "2012-04-23T18:25:43.511Z", "consumption" : 123}' http://localhost:9000/api/consumption/notify
   */
  ServiceCall<Consumption, Done> ingest();

  /**
   * Example: curl -H "Content-Type: application/json" -X POST -d '{"consumerId" : "ABC123", "timestamp" : "2012-04-23T18:25:43.511Z"}' http://localhost:9000/api/consumption/query
   */
  ServiceCall<ConsumptionQuery, Integer> query();

  /**
   * This gets published to Kafka.
   */
  Topic<ConsumerEvent> consumerEvents();

  @Override
  default Descriptor descriptor()
  {
    return named("consumption-ingestion").withCalls(
      pathCall("/api/consumption/notify", this::ingest),
      pathCall("/api/consumption/query", this::query)
    ).withTopics(
      topic("consumption-ingestion-events", this::consumerEvents)
        // Kafka partitions messages, messages within the same partition will
        // be delivered in order, to ensure that all messages for the same consumer
        // go to the same partition (and hence are delivered in order with respect
        // to that user), we configure a partition key strategy that extracts the
        // consumer id as the partition key.
        .withProperty(KafkaProperties.partitionKeyStrategy(), ConsumerEvent::getConsumerId)
    ).withAutoAcl(true);
  }
}
