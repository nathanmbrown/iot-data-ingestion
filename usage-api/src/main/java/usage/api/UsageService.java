package usage.api;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.namedCall;
import static com.lightbend.lagom.javadsl.api.Service.pathCall;

import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;

/**
 * The usage service interface.
 * <p>
 * This describes everything that Lagom needs to know about how to serve and
 * consume the Usage service.
 */
public interface UsageService extends Service {

  /**
   * Example: curl -H "Content-Type: application/json" -X POST -d '{"consumerId" : "ABC123", "date" : "2012-04-23"}' http://localhost:9000/api/usage
   */
  ServiceCall<UsageQuery, Double> queryUsage();

  @Override
  default Descriptor descriptor() {
    return named("usage")
            .withCalls(
              pathCall("/api/usage", this::queryUsage)
            ).withAutoAcl(true);
  }
}
