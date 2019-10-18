## Actionable Primitives

Rules and Timers are both actionable automation primitives, meaning
that they when either are "triggered" they invoke actions. The
machinery and interface that performs these actions is identical
between the two. When an actionable automation primitive is triggered,
it performs zero or more "actions". You can think of an action as
similar to a [webhook](https://en.wikipedia.org/wiki/Webhook).

An action is specified by the following important parameters:

*   A URI. Required.
*   A REST method (PUT, POST, DELETE, etc) to perform on that URI.
    Optional: defaults to POST.
*   A body to pass along while performing the method. Optional:
    defaults to empty.
*   An enumeration value that determines if the action should be
    evaluated synchronously or asynchronously, as well as what to do
    in the event of an error. Optional: defaults to asynchronous
    firing (later actions don't wait for this action to finish before
    firing).
*   Security context information. Optional: defaults to no security
    context.
