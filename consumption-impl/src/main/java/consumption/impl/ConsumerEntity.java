package consumption.impl;

import akka.Done;
import consumption.impl.ConsumerEvent.ConsumptionRecorded;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * This is an event sourced entity. It has a state, {@link ConsumerState}, which
 * stores the Consumption values aggregated by PTU.
 * <p>
 * Event sourced entities are interacted with by sending them commands. This entity supports two commands,
 * an {@link ConsumerCommand.RecordConsumption} command which is used to update the aggregated consumption for
 * the Consumer, and a {@link ConsumerCommand.GetConsumption} command which is used to query the Consumers consumption state.
 * <p>
 * Commands get translated to events, and it's the events that get persisted by
 * the entity. Each event will have an event handler registered for it, and an
 * event handler simply applies an event to the current state. This will be done
 * when the event is first created, and it will also be done when the entity is
 * loaded from the database - each event will be replayed to recreate the state
 * of the entity.
 * <p>
 * This entity defines one event, the {@link ConsumptionRecorded} event,
 * which is emitted when a {@link ConsumerCommand.RecordConsumption} command is received.
 */
public class ConsumerEntity extends PersistentEntity<ConsumerCommand, ConsumerEvent, ConsumerState>
{
  /**
   * An entity can define different behaviours for different states, but it will
   * always start with an initial behaviour. This entity only has one behaviour.
   */
  @Override
  public Behavior initialBehavior(Optional<ConsumerState> snapshotState)
  {

    /*
     * Behaviour is defined using a behaviour builder. The behaviour builder
     * starts with a state, if this entity supports snapshotting (an
     * optimisation that allows the state itself to be persisted to combine many
     * events into one), then the passed in snapshotState may have a value that
     * can be used.
     *
     * Otherwise, the default state is empty.
     */
    BehaviorBuilder b = newBehaviorBuilder(
      snapshotState.orElse(new ConsumerState(new DailyConsumption[0])));

    /*
     * Command handler for the RecordConsumption command.
     */
    b.setCommandHandler(ConsumerCommand.RecordConsumption.class, (cmd, ctx) ->
    {
      // In response to this command, first persist it as a ConsumptionRecorded event
      try {
        LocalDateTime.parse(cmd.getTimestamp()); // check it's valid
        ConsumptionRecorded event = new ConsumptionRecorded(entityId(), cmd.getTimestamp(), cmd.getConsumption());
        System.out.println("Storing " + event);
        return ctx.thenPersist(event,
                               // Then once the event is successfully persisted, we respond with done.
                               evt -> ctx.reply(Done.getInstance()));
      } catch (DateTimeParseException e) {
        ctx.invalidCommand("Invalid timestamp format : " + cmd.getTimestamp());
        return ctx.done();
      }
    });

    /*
     * Event handler for the ConsumptionRecorded event.
     */
    b.setEventHandler(ConsumptionRecorded.class,
                      // We aggregate the current state with the consumption from the event.
                      evt ->
                      {
                        System.out.println("Handling " + evt);
                        return getUpdatedState(state(),
                                               LocalDateTime.parse(evt.getTimestamp()),
                                               evt.getConsumption());
                      });

    b.setReadOnlyCommandHandler(ConsumerCommand.GetConsumption.class, (cmd, ctx) -> {
      try {
        LocalDateTime timestamp = LocalDateTime.parse(cmd.getTimestamp());
        ctx.reply(getPTUValue(state().getDailyConsumptions(), timestamp));
      } catch (DateTimeParseException e) {
        ctx.invalidCommand("Invalid timestamp format : " + cmd.getTimestamp());
      }
    });

    /*
     * We've defined all our behaviour, so build and return it.
     */
    return b.build();
  }

  public static int getPTUValue(DailyConsumption[] dailyConsumptions,
                                LocalDateTime timestamp)
  {
    LocalDate date = timestamp.toLocalDate();
    // find the existing daily consumption for the date
    int index = Arrays.binarySearch(dailyConsumptions,
                                    new DailyConsumption(date.toString()),
                                    Comparator.comparing(dailyConsumption ->
                                                           LocalDate.parse(dailyConsumption.getDate())));
    // extract the current PTU value array
    int[] currentPTUValues = index >= 0 ?
                             dailyConsumptions[index].getPtuValues() :
                             DailyConsumption.EMPTY_PTU_VALUES;
    return currentPTUValues[getPTUValueIndex(timestamp)];
  }

  @NotNull
  public static ConsumerState getUpdatedState(@NotNull ConsumerState state,
                                              @NotNull LocalDateTime timestamp,
                                              int consumption)
  {
    LocalDate date = timestamp.toLocalDate();
    DailyConsumption[] currentDailyConsumptions = state.getDailyConsumptions();
    // find the existing daily consumption for the date
    int index = Arrays.binarySearch(currentDailyConsumptions,
                                    new DailyConsumption(date.toString()),
                                    Comparator.comparing(DailyConsumption::getDate));
    // extract the current PTU value array
    int[] currentPTUValues = index >= 0 ?
                             currentDailyConsumptions[index].getPtuValues() :
                             DailyConsumption.EMPTY_PTU_VALUES;
    // clone as state must be immutable
    int[] newPTUValues = Arrays.copyOf(currentPTUValues, currentPTUValues.length);

    int ptuIndex = getPTUValueIndex(timestamp);
    // update the new PTU value
    newPTUValues[ptuIndex] = currentPTUValues[ptuIndex] + consumption;
    DailyConsumption newDailyConsumption = new DailyConsumption(date.toString(), newPTUValues);

    // clone consumptions as state must be immutable
    DailyConsumption[] newDailyConsumptions =
      Arrays.copyOf(currentDailyConsumptions, currentDailyConsumptions.length);
    if (index < 0) {
      // date not recorded yet
      int insertionIndex = -index - 1;
      newDailyConsumptions = ArrayUtils.insert(insertionIndex,
                                               newDailyConsumptions,
                                               newDailyConsumption);
    } else {
      newDailyConsumptions[index] = newDailyConsumption;
    }
    return new ConsumerState(newDailyConsumptions);
  }

  public static int getPTUValueIndex(LocalDateTime timestamp)
  {
    int hour = timestamp.getHour();
    // 2 PTUs per hour
    int result = 2 * hour;
    int minute = timestamp.getMinute();
    if (minute >= 30) {
      result++;
    }
    return result;
  }
}
