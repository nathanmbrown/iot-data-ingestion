package consumption.api;

import lombok.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

/**
 * The request body sent to the Consumption Ingestion service ingest() method
 *
 * @author Nathan
 * Created : 17/02/2018
 */

@Value
@JsonDeserialize
public final class Consumption
{
  public final String consumerId;
  public final String timestamp;
  public final int consumption;

  @JsonCreator
  public Consumption(String consumerId, String timestamp, int consumption) {
    this.consumerId = Preconditions.checkNotNull(consumerId, "consumerID");
    this.timestamp = Preconditions.checkNotNull(timestamp, "timestamp");
    this.consumption = consumption;
  }
}
