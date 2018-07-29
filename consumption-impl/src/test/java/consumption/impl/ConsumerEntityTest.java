package consumption.impl;

import akka.Done;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver.Outcome;
import org.jetbrains.annotations.NotNull;
import org.junit.*;

import java.time.*;
import java.util.Collections;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class ConsumerEntityTest
{
  protected static final LocalDate TEST_DATE = LocalDate.of(2018, Month.APRIL, 1);
  private static final String TEST_DATE_STRING = TEST_DATE.toString();
  static ActorSystem system;

  @BeforeClass
  public static void setup()
  {
    system = ActorSystem.create("ConsumerEntityTest");
  }

  @AfterClass
  public static void teardown()
  {
    JavaTestKit.shutdownActorSystem(system);
    system = null;
  }

  @Test
  public void testNoState() throws Exception
  {
    PersistentEntityTestDriver<ConsumerCommand, ConsumerEvent, ConsumerState> driver =
      new PersistentEntityTestDriver<>(system,
                                       new ConsumerEntity(),
                                       "consumer-1");

    Outcome<ConsumerEvent, ConsumerState> outcome1 =
      driver.run(new ConsumerCommand.GetConsumption(createDateTimeString(0, 0)));
    assertEquals(0, outcome1.getReplies().get(0));
    assertEquals(Collections.emptyList(), outcome1.issues());
  }

  @Test
  public void testGetConsumptionForIllegalTimestamp() throws Exception
  {
    PersistentEntityTestDriver<ConsumerCommand, ConsumerEvent, ConsumerState> driver =
      new PersistentEntityTestDriver<>(system,
                                       new ConsumerEntity(),
                                       "consumer-1");

    Outcome<ConsumerEvent, ConsumerState> outcome1 =
      driver.run(new ConsumerCommand.GetConsumption("3:55pm 24th June 2018"));
    assertTrue(outcome1.getReplies().get(0) instanceof PersistentEntity.InvalidCommandException);
  }

  @Test
  public void testSingleConsumptionIngestionAndQuery()
  {
    PersistentEntityTestDriver<ConsumerCommand, ConsumerEvent, ConsumerState> driver =
      new PersistentEntityTestDriver<>(system,
                                       new ConsumerEntity(),
                                       "consumer-1");

    LocalDateTime timestamp = createTestDateTime(0, 10);
    String timestampStr = timestamp.toString();
    Outcome<ConsumerEvent, ConsumerState> outcome1 =
      driver.run(new ConsumerCommand.RecordConsumption(timestampStr, 123),
                 new ConsumerCommand.GetConsumption(createDateTimeString(0, 0)));
    assertEquals(1, outcome1.events().size());
    assertEquals(new ConsumerEvent.ConsumptionRecorded("consumer-1", timestampStr, 123),
                 outcome1.events().get(0));
    assertArrayEquals(new Object[]{new DailyConsumption(TEST_DATE_STRING, createPTUValues(0, 123))},
                      outcome1.state().getDailyConsumptions());
    assertEquals(2, outcome1.getReplies().size());
    assertEquals(Done.getInstance(), outcome1.getReplies().get(0));
    assertEquals(123, outcome1.getReplies().get(1));
    assertEquals(Collections.emptyList(), outcome1.issues());
  }

  @Test
  public void testDoubleConsumptionIngestionAndQuery()
  {
    PersistentEntityTestDriver<ConsumerCommand, ConsumerEvent, ConsumerState> driver =
      new PersistentEntityTestDriver<>(system,
                                       new ConsumerEntity(),
                                       "consumer-1");

    String timestamp1Str = createTestDateTime(0, 10).toString();
    String timestamp2Str = createTestDateTime(0, 20).toString();
    Outcome<ConsumerEvent, ConsumerState> outcome1 =
      driver.run(new ConsumerCommand.RecordConsumption(timestamp1Str, 123),
                 new ConsumerCommand.RecordConsumption(timestamp2Str, 100),
                 new ConsumerCommand.GetConsumption(createDateTimeString(0, 0)));
    assertEquals(2, outcome1.events().size());
    assertEquals(new ConsumerEvent.ConsumptionRecorded("consumer-1", timestamp1Str, 123),
                 outcome1.events().get(0));
    assertEquals(new ConsumerEvent.ConsumptionRecorded("consumer-1", timestamp2Str, 100),
                 outcome1.events().get(1));
    assertArrayEquals(new Object[]{new DailyConsumption(TEST_DATE_STRING, createPTUValues(0, 223))},
                      outcome1.state().getDailyConsumptions());
    assertEquals(3, outcome1.getReplies().size());
    assertEquals(Done.getInstance(), outcome1.getReplies().get(0));
    assertEquals(Done.getInstance(), outcome1.getReplies().get(1));
    assertEquals(223, outcome1.getReplies().get(2));
    assertEquals(Collections.emptyList(), outcome1.issues());
  }

  @Test
  public void testSingleConsumptionIngestionForIllegalTimestamp() throws Exception
  {
    PersistentEntityTestDriver<ConsumerCommand, ConsumerEvent, ConsumerState> driver =
      new PersistentEntityTestDriver<>(system,
                                       new ConsumerEntity(),
                                       "consumer-1");

    Outcome<ConsumerEvent, ConsumerState> outcome1 =
      driver.run(new ConsumerCommand.RecordConsumption("3:55pm 24th June 2018", 123));
    assertTrue(outcome1.getReplies().get(0) instanceof PersistentEntity.InvalidCommandException);
  }

  private static int[] createPTUValues(int index1, int value1)
  {
    int[] result = new int[48];
    result[index1] = value1;
    return result;
  }

  @NotNull
  public static String createDateTimeString(int hour, int minute)
  {
    return createTestDateTime(hour, minute).toString();
  }

  @Test
  public void testGetPTUValueIndex() throws Exception
  {
    Assert.assertEquals(0, ConsumerEntity.getPTUValueIndex(createTestDateTime(0, 0)));
    Assert.assertEquals(0, ConsumerEntity.getPTUValueIndex(createTestDateTime(0, 10)));
    Assert.assertEquals(0, ConsumerEntity.getPTUValueIndex(createTestDateTime(0, 29)));
    Assert.assertEquals(1, ConsumerEntity.getPTUValueIndex(createTestDateTime(0, 30)));
    Assert.assertEquals(24, ConsumerEntity.getPTUValueIndex(createTestDateTime(12, 0)));
    Assert.assertEquals(47, ConsumerEntity.getPTUValueIndex(createTestDateTime(23, 59)));
  }

  @NotNull
  private static LocalDateTime createTestDateTime(int hour, int minute)
  {
    return TEST_DATE.atTime(hour, minute);
  }

  @Test
  public void testGetPTUValue() throws Exception
  {
    int[] testPTUValues = IntStream.rangeClosed(0, 47).toArray();
    DailyConsumption[] dailyConsumptions = {
      new DailyConsumption(LocalDate.of(2018, Month.APRIL, 1).toString()),
      new DailyConsumption(LocalDate.of(2018, Month.APRIL, 2).toString(), testPTUValues),
      new DailyConsumption(LocalDate.of(2018, Month.APRIL, 3).toString()),
      new DailyConsumption(LocalDate.of(2018, Month.APRIL, 4).toString()),
      };
    assertEquals(24, ConsumerEntity.getPTUValue(dailyConsumptions,
                                                LocalDateTime.of(2018, Month.APRIL, 2, 12, 1)));
    assertEquals(0, ConsumerEntity.getPTUValue(dailyConsumptions,
                                               LocalDateTime.of(2018, Month.APRIL, 1, 12, 1)));
  }

  @Test
  public void testGetUpdatedStateWithExistingDailyConsumption() throws Exception
  {
    int[] testPTUValues = IntStream.rangeClosed(0, 47).toArray();
    DailyConsumption[] dailyConsumptions = {
      new DailyConsumption(LocalDate.of(2018, Month.APRIL, 1).toString()),
      new DailyConsumption(LocalDate.of(2018, Month.APRIL, 2).toString(), testPTUValues),
      new DailyConsumption(LocalDate.of(2018, Month.APRIL, 3).toString()),
      new DailyConsumption(LocalDate.of(2018, Month.APRIL, 4).toString())
    };
    ConsumerState testState = new ConsumerState(dailyConsumptions);
    testState = testUpdateState(testState, 123, 123);
    testUpdateState(testState, 100, 223);
  }

  private ConsumerState testUpdateState(ConsumerState testState, int consumption, int expectedAggregateConsumption)
  {
    LocalDateTime testDateTime = LocalDateTime.of(2018, Month.APRIL, 1, 12, 1);
    ConsumerState updatedState = ConsumerEntity.getUpdatedState(testState, testDateTime, consumption);
    assertNotSame(updatedState, testState);
    assertNotSame(updatedState.getDailyConsumptions(), testState.getDailyConsumptions());
    // check unmodified haven't changed
    IntStream.range(1, 4).forEach(i -> {
      assertSame(updatedState.getDailyConsumptions()[i], testState.getDailyConsumptions()[i]);
      int[] updatedPTUValues = updatedState.getDailyConsumptions()[i].getPtuValues();
      int[] originalPTUValues = testState.getDailyConsumptions()[i].getPtuValues();
      assertSame(updatedPTUValues, originalPTUValues);
    });
    assertNotSame(updatedState.getDailyConsumptions()[0], testState.getDailyConsumptions()[0]);
    int[] updatedPTUValues = updatedState.getDailyConsumptions()[0].getPtuValues();
    int[] originalPTUValues = testState.getDailyConsumptions()[0].getPtuValues();
    assertNotSame(updatedPTUValues, originalPTUValues);
    IntStream.range(0, 48).forEach(i -> {
      if (i != 24) {
        assertEquals(updatedPTUValues[i], originalPTUValues[i]);
      } else {
        assertNotEquals(updatedPTUValues[i], originalPTUValues[i]);
        assertEquals(updatedPTUValues[i], expectedAggregateConsumption);
      }
    });
    return updatedState;
  }

  @Test
  public void testGetUpdatedStateWithNoDailyConsumption() throws Exception
  {
    int[] testPTUValues = IntStream.rangeClosed(0, 47).toArray();
    DailyConsumption[] dailyConsumptions = {
      new DailyConsumption(LocalDate.of(2018, Month.APRIL, 1).toString()),
      new DailyConsumption(LocalDate.of(2018, Month.APRIL, 2).toString(), testPTUValues),
      new DailyConsumption(LocalDate.of(2018, Month.APRIL, 3).toString()),
      new DailyConsumption(LocalDate.of(2018, Month.APRIL, 4).toString())
    };
    ConsumerState testState = new ConsumerState(dailyConsumptions);
    LocalDateTime testDateTime = LocalDateTime.of(2018, Month.MARCH, 31, 12, 1);
    ConsumerState updatedState = ConsumerEntity.getUpdatedState(testState, testDateTime, 123);
    assertNotSame(updatedState, testState);
    assertNotSame(updatedState.getDailyConsumptions(), testState.getDailyConsumptions());
    // check pre-existing haven't changed (however index shifted)
    IntStream.range(1, 5).forEach(i -> {
      assertSame(updatedState.getDailyConsumptions()[i], testState.getDailyConsumptions()[i - 1]);
      int[] updatedPTUValues = updatedState.getDailyConsumptions()[i].getPtuValues();
      int[] originalPTUValues = testState.getDailyConsumptions()[i - 1].getPtuValues();
      assertSame(updatedPTUValues, originalPTUValues);
    });
    assertNotSame(updatedState.getDailyConsumptions()[0], testState.getDailyConsumptions()[0]);
    int[] updatedPTUValues = updatedState.getDailyConsumptions()[0].getPtuValues();
    IntStream.range(0, 48).forEach(i -> {
      if (i != 24) {
        assertEquals(updatedPTUValues[i], 0);
      } else {
        assertEquals(updatedPTUValues[i], 123);
      }
    });
  }

}
