package usage.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import lombok.Value;

/**
 * The request body sent to the Usage service
 *
 * @author Nathan
 * Created : 17/02/2018
 */

@Value
@JsonDeserialize
public final class UsageQuery
{
  public final String consumerId;
  public final String date;

  @JsonCreator
  public UsageQuery(String consumerId, String date)
  {
    this.consumerId = Preconditions.checkNotNull(consumerId, "consumerId");
    this.date = Preconditions.checkNotNull(date, "fromDate");
  }
}
