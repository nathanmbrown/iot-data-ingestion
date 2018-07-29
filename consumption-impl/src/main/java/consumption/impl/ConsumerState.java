package consumption.impl;

import lombok.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.lightbend.lagom.serialization.CompressedJsonable;

/**
 * The state for the {@link ConsumerEntity}.
 */
@SuppressWarnings("serial")
@Value
@JsonDeserialize
public final class ConsumerState implements CompressedJsonable {

  public final DailyConsumption[] dailyConsumptions;

  @JsonCreator
  public ConsumerState(DailyConsumption[] dailyConsumptions) {
    this.dailyConsumptions = Preconditions.checkNotNull(dailyConsumptions, "ptuConsumptionValues");
  }
}
