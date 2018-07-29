package consumption.impl;

import com.lightbend.lagom.javadsl.persistence.AggregateEvent;
import com.lightbend.lagom.javadsl.persistence.AggregateEventShards;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTagger;
import lombok.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.lightbend.lagom.serialization.Jsonable;

/**
 * This interface defines all the events that the Consumer entity supports.
 * <p>
 * By convention, the events should be inner classes of the interface, which
 * makes it simple to get a complete picture of what events an entity has.
 */
public interface ConsumerEvent extends Jsonable, AggregateEvent<ConsumerEvent> {

  /**
   * Tags are used for getting and publishing streams of events. Each event
   * will have this tag, and in this case, we are partitioning the tags into
   * 4 shards, which means we can have 4 concurrent processors/publishers of
   * events.
   */
  AggregateEventShards<ConsumerEvent> TAG = AggregateEventTag.sharded(ConsumerEvent.class, 4);

  /**
   * An event that represents a change in the consumer's consumption.
   */
  @SuppressWarnings("serial")
  @Value
  @JsonDeserialize
  final class ConsumptionRecorded implements ConsumerEvent
  {
    public final String consumerId;
    public final String timestamp;
    public final int consumption;

    @JsonCreator
    public ConsumptionRecorded(String consumerId, String timestamp, int consumption) {
      this.consumerId = Preconditions.checkNotNull(consumerId, "consumerId");
      this.timestamp = Preconditions.checkNotNull(timestamp, "timestamp");
      this.consumption = consumption;
    }
  }


  @Override
  default AggregateEventTagger<ConsumerEvent> aggregateTag() {
    return TAG;
  }

}
