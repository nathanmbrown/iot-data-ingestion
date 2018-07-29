# The Challenge:

## Objective

Build a solution comprised of a few microservices.  The solution purpose is to store household energy usage data and provide a daily total for each household.

The exercise is designed to provide you with the opportunity to demonstrate your practical understanding of microservices and provide the opportunity to present your engineering skills against best practise and quality.

## Context

As a System Operator I want to be able to retrieve daily usage data for any household so that I can calculate their usage bill.

To help development, a CSV containing dummy data for 3 households has been provided

## Constraints

The first service should be responsible for receiving household consumption data, aggregating it into time units of 30 minutes (PTU) and persisting it.

The second service is responsible for providing usage data second for calculating daily usage.

Quality acceptance criteria have not been specified but should be demonstrated through appropriate use of testing.

## Notes

- Energy usage data provided is in W (watts) and represents the energy used since the last observation.  Aggregation up to the desired PTU level is therefore a simple addition of all observations.
- Household daily usage should be available on request and provided in kWh.
- A day is defined from 00:00 to 23:59.
- The solution should use common microservices patterns and governance.
- Interface to both services can be via an API or GUI.
- You should be prepared to present and briefly talk through your solution at the interview (10 minutes).
- You should consider both the approach to building this and how you would maintain quality control.


