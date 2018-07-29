package consumption.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Preconditions;
import lombok.Value;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = ConsumerEvent.ConsumptionRecorded.class, name = "consumption-recorded")
})
public interface ConsumerEvent
{
  String getConsumerId();

  @Value
  final class ConsumptionRecorded implements ConsumerEvent
  {
    public final String consumerId;
    public final String timestamp;
    public final int consumption;

    @JsonCreator
    public ConsumptionRecorded(String consumerId, String timestamp, int consumption)
    {
      this.consumerId = Preconditions.checkNotNull(consumerId, "consumerId");
      this.timestamp = Preconditions.checkNotNull(timestamp, "timestamp");
      this.consumption = consumption;
    }
  }
}
