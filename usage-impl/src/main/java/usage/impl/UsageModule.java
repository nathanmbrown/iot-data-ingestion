package usage.impl;

import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;
import consumption.api.ConsumptionService;
import usage.api.UsageService;

/**
 * The module that binds the StreamService so that it can be served.
 */
public class UsageModule extends AbstractModule implements ServiceGuiceSupport
{
  @Override
  protected void configure()
  {
    // Bind the StreamService service
    bindService(UsageService.class, UsageServiceImpl.class);
    // Bind the ConsumptionService client
    bindClient(ConsumptionService.class);
    // Bind the subscriber eagerly to ensure it starts up
    bind(ConsumptionSubscriber.class).asEagerSingleton();
  }
}
