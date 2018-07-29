package consumption.impl;

import akka.Done;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.serialization.CompressedJsonable;
import com.lightbend.lagom.serialization.Jsonable;
import lombok.Value;

/**
 * This interface defines all the commands that the Consumer entity supports.
 * 
 * By convention, the commands should be inner classes of the interface, which
 * makes it simple to get a complete picture of what commands an entity
 * supports.
 */
public interface ConsumerCommand extends Jsonable {
  /**
   * A command to add the consumption to the consumer's aggregate.
   * <p>
   * It has a reply type of {@link akka.Done}, which is sent back to the caller
   * when all the events emitted by this command are successfully persisted.
   */
  @SuppressWarnings("serial")
  @Value
  @JsonDeserialize
  final class RecordConsumption implements ConsumerCommand, CompressedJsonable, PersistentEntity.ReplyType<Done> {
    public final String timestamp;
    public final int consumption;

    @JsonCreator
    public RecordConsumption(String timestamp, int consumption) {
      this.timestamp = Preconditions.checkNotNull(timestamp, "timestamp");
      this.consumption = consumption;
    }
  }

  /**
   * A command to request the aggregated consumption for the consumer for the PTU specified by the timestamp.
   * <p>
   * It has a reply type of Integer, which is the aggregated consumption in watts.
   */
  @SuppressWarnings("serial")
  @Value
  @JsonDeserialize
  final class GetConsumption implements ConsumerCommand, CompressedJsonable, PersistentEntity.ReplyType<Integer> {
    public final String timestamp;

    @JsonCreator
    public GetConsumption(String timestamp) {
      this.timestamp = Preconditions.checkNotNull(timestamp, "timestamp");
    }
  }
}
