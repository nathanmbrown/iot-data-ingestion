package consumption.impl;

import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;
import consumption.api.ConsumptionService;

/**
 * The module that binds the ConsumptionIngestionService so that it can be served.
 */
public class ConsumptionModule extends AbstractModule implements ServiceGuiceSupport {
  @Override
  protected void configure() {
    bindService(ConsumptionService.class, ConsumptionServiceImpl.class);
  }
}
