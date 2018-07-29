package consumption.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import lombok.Value;

/**
 * The request body sent to the Consumption Ingestion service query() method
 *
 * @author Nathan
 * Created : 17/02/2018
 */

@Value
@JsonDeserialize
public final class ConsumptionQuery
{
  public final String consumerId;
  public final String timestamp;

  @JsonCreator
  public ConsumptionQuery(String consumerId, String timestamp) {
    this.consumerId = Preconditions.checkNotNull(consumerId, "consumerID");
    this.timestamp = Preconditions.checkNotNull(timestamp, "timestamp");
  }
}
