package consumption.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import lombok.Value;

/**
 * Value object for storing the consumption values for a specific date.
 *
 * @author Nathan
 * Created : 17/02/2018
 */
@Value
@JsonDeserialize
public final class DailyConsumption
{
  public static final int[] EMPTY_PTU_VALUES = new int[48];
  public final String date;
  public final int[] ptuValues;

  public DailyConsumption(String date)
  {
    this(date, EMPTY_PTU_VALUES);
  }

  @JsonCreator
  public DailyConsumption(String date, int[] ptuValues)
  {
    this.date = Preconditions.checkNotNull(date, "date");
    this.ptuValues = Preconditions.checkNotNull(ptuValues, "ptuValues");
  }
}
