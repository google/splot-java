## Pairings

Pairings are automation primitives that connect two [resources][], which
are referred to as the *source* and *destination*. Changes observed to
the source resource can be automatically applied to the destination
resource, and vise-versa.

[resources]: https://tools.ietf.org/html/rfc2616#section-5.2

In *forward propagation*, changes observed to the source are applied
to the destination.
In *reverse propagation*, changes observed to the destination are applied
to the source. Both propagation modes can be enabled simultaneously,
allowing for resource value synchronization.

In addition to basic value mirroring, two custom transforms can be
specified: a *forward transform* that is applied to changes from the
source to the destination, and a *reverse transform* that is applied
to changes from the destination to the source. These transforms
are written using the expression language outlined in (#sae).

All pairings implement the *AutomationPairing* trait, described
in (#trait-automation-pairing).
