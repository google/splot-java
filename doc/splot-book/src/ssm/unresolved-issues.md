# Unresolved Issues #

## Secure Multicast Discovery ##

It would be desirable for someone with the appropriate credentials to
be able to quickly discover devices that they can monitor/control.
However, in order to avoid the case where the loss of a single device
leads to the compromise of the entire network, monitoring/control
credentials are typically asymmetric. While OSCORE does support secure
multicast, it only works with a symmetric key.

Possible solutions:

*   Have two keys: An asymmetric one for monitoring/control, and a
    symmetric one for multicast discovery.
    *   Pros: Fast.
    *   Cons: Difficult to manage or revoke discovery key.
*   Use unsecured multicast to discover devices which support SOM+SSM
    and then follow up each one with a secure unicast discovery
    request.
    *   Pros: Secure. Single certificate means easier key management.
    *   Cons: Slow when there are lots of devices. Must negotiate a
        new security session with each device.

Either approach could be adopted without any changes to the underlying
security model.

## Certificate Format ##

The certificate format is currently undefined, but assumed to
ultimately be based on X.509. However, there could be benefits to
having a compressed certificate format that expands to X.509, similar
to [Nest Weave Digital Certificates][NWDC].

[NWDC]: https://github.com/openweave/openweave-core/blob/b506d9e3c28eee887517c1f31f605f6e8151c039/doc/specs/protocol-specification-weave-digital-certificates.pdf
