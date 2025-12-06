# Phone-to-car trip handoff

Transfer a trip from a phone to a simulated car display and keep its order and destination intact across retries.

## First acceptance test

Send a trip with two stops, drop the acknowledgement, and resend the same trip revision. The car display should show exactly one trip with both stops in order and the phone should eventually mark it delivered.