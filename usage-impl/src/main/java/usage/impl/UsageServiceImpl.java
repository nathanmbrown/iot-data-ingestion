package usage.impl;

import com.lightbend.lagom.javadsl.api.ServiceCall;
import consumption.api.ConsumptionService;
import usage.api.*;

import javax.inject.Inject;

/**
 * Implementation of the UsageService.
 */
public class UsageServiceImpl implements UsageService
{
  private final ConsumptionService consumptionService;
  private final UsageRepository repository;

  @Inject
  public UsageServiceImpl(ConsumptionService consumptionService, UsageRepository repository) {
    this.consumptionService = consumptionService;
    this.repository = repository;
  }

  @Override
  public ServiceCall<UsageQuery, Double> queryUsage()
  {
    return usageQuery -> repository.getUsage(usageQuery.getConsumerId(), usageQuery.getDate());
  }
}
