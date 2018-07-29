package usage.it;

import akka.Done;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import com.lightbend.lagom.javadsl.client.integration.LagomClientFactory;
import consumption.api.Consumption;
import consumption.api.ConsumptionService;
import org.hamcrest.Matchers;
import org.junit.*;
import usage.api.UsageQuery;
import usage.api.UsageService;

import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.concurrent.*;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

public class UsageIntegrationTest
{
  protected static final LocalDate TEST_DATE = LocalDate.of(2018, Month.APRIL, 1);
  private static final String TEST_DATE_STRING = TEST_DATE.toString();

  private static final String SERVICE_LOCATOR_URI = "http://localhost:9008";

  private static LagomClientFactory clientFactory;
  private static ConsumptionService consumptionService;
  private static UsageService usageService;
  private static ActorSystem system;
  private static Materializer mat;

  @BeforeClass
  public static void setup()
  {
    clientFactory = LagomClientFactory.create("integration-test",
                                              UsageIntegrationTest.class.getClassLoader());
    consumptionService = clientFactory.createDevClient(ConsumptionService.class, URI.create(SERVICE_LOCATOR_URI));
    usageService = clientFactory.createDevClient(UsageService.class, URI.create(SERVICE_LOCATOR_URI));

    system = ActorSystem.create();
    mat = ActorMaterializer.create(system);
  }

  @Test
  public void testUsage() throws Exception
  {
    checkUsage("consumer-1", TEST_DATE_STRING, 0L);
    ingestConsumptionSync(TEST_DATE.atTime(0, 10), "consumer-1", 123);
    checkUsage("consumer-1", TEST_DATE_STRING, 123L);
    ingestConsumptionSync(TEST_DATE.atTime(0, 20), "consumer-1", 100);
    checkUsage("consumer-1", TEST_DATE_STRING, 223L);
    checkUsage("consumer-2", TEST_DATE_STRING, 0L);
    LocalDate date2 = LocalDate.of(2018, Month.APRIL, 2);
    ingestConsumptionSync(date2.atTime(0, 20), "consumer-1", 100);
    checkUsage("consumer-1", TEST_DATE_STRING, 223L);
    checkUsage("consumer-1", date2.toString(), 100L);
  }

  @Test
  public void testConsumeCSV() throws Exception
  {
    Scanner scanner = new Scanner(UsageIntegrationTest.class.getResourceAsStream("/RawUsageData.csv"));
    scanner.nextLine(); // skip header
    int lines = 0;
    long expectedUsageWatts = 0;
    while (scanner.hasNextLine() && lines < 10000) {
      String line = scanner.nextLine();
      String[] fields = line.split(",");
      LocalDateTime timestamp = LocalDateTime.parse(fields[0], DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
      int consumption = Integer.parseInt(fields[1]);
      String consumerId = "House-" + fields[2];
//      System.out.println(timestamp + " : " + consumerId + " : " + consumption);
      ingestConsumptionSync(timestamp, consumerId, consumption);
      if (timestamp.getDayOfMonth() == 20 && "House-1".equals(consumerId))
        expectedUsageWatts += consumption;
      lines++;
    }
    System.out.println(lines + " records ingested.");
    checkUsage("House-1", "2017-09-20", expectedUsageWatts / 1000D);
  }

  private void ingestConsumptionSync(LocalDateTime timestamp, String consumerId, int consumption) throws Exception
  {
    awaitCompletion(ingestConsumptionAsync(timestamp, consumerId, consumption));
  }

  private CompletionStage<Done> ingestConsumptionAsync(LocalDateTime timestamp, String consumerId, int consumption)
  {
    return consumptionService.ingest().invoke(new Consumption(consumerId,
                                                              timestamp.toString(),
                                                              consumption));
  }

  private void checkUsage(String consumerId, String date, double expectedConsumption)
  {
    await().atMost(35, TimeUnit.SECONDS)
           .pollInterval(1, TimeUnit.SECONDS)
           .until(() -> awaitCompletion(usageService.queryUsage().invoke(new UsageQuery(consumerId, date))),
                  Matchers.equalTo(expectedConsumption));
  }

  private <T> T awaitCompletion(CompletionStage<T> future) throws Exception
  {
    return future.toCompletableFuture().get(10, TimeUnit.SECONDS);
  }

  @AfterClass
  public static void tearDown()
  {
    if (clientFactory != null) {
      clientFactory.close();
    }
    if (system != null) {
      system.terminate();
    }
  }


}
