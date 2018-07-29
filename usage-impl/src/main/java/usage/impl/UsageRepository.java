package usage.impl;

import akka.Done;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
public class UsageRepository
{
  private final CassandraSession uninitialisedSession;

  // Will return the session when the Cassandra tables have been successfully created
  private volatile CompletableFuture<CassandraSession> initialisedSession;

  @Inject
  public UsageRepository(CassandraSession uninitialisedSession)
  {
    this.uninitialisedSession = uninitialisedSession;
    // Eagerly create the session
    session();
  }

  private CompletionStage<CassandraSession> session()
  {
    // If there's no initialised session, or if the initialised session future completed
    // with an exception, then reinitialise the session and attempt to create the tables
    if (initialisedSession == null || initialisedSession.isCompletedExceptionally()) {
      initialisedSession = uninitialisedSession.executeCreateTable(
        "CREATE TABLE IF NOT EXISTS usage (consumerId text, date text, consumption counter, PRIMARY KEY (consumerId, date))"
      ).thenApply(done -> uninitialisedSession).toCompletableFuture();
    }
    return initialisedSession;
  }

  public CompletionStage<Double> getUsage(String consumerId, String date)
  {
    System.out.println("Querying consumption for consumer " + consumerId + " at date " + date);
    return session().thenCompose(session ->
                                   session.selectOne("SELECT consumption FROM usage " +
                                                     "WHERE consumerId = ? AND date = ?",
                                                     consumerId,
                                                     date))
                    .thenApply(rowMaybe -> rowMaybe.map(row -> row.getLong("consumption")).orElse(0L))
                    .thenApply(watts -> watts / 1000D);
  }

  public CompletionStage<Done> updateUsage(String consumerId, LocalDateTime timestamp, int consumption)
  {
    String date = LocalDate.from(timestamp).toString();
    System.out.println("Storing " + consumption + " for consumer " + consumerId + " at date " + date);
    return session().thenCompose(session ->
                                   session.executeWrite("UPDATE usage " +
                                                        "SET consumption = consumption + " + consumption + " " +
                                                        "WHERE consumerId = ? AND date = ?",
                                                        consumerId, date)
    );
  }
}
