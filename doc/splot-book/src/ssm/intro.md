# Splot Security Model

This document outlines the proposed overall architecture of the Splot Security
Model (SSM), which defines the mechanisms for how the security of the
Splot Object Model (SOM) can be managed in-band, as well as some hints
about how native implementations of the Splot Object Model (like SMCP)
can be secured on the wire.

The SSM provides:

*   Framework and nomenclature for Splot Object Model security
*   Decentralized, flexible mandatory access controls
*   In-band Management via the Splot Object Model

## Use Cases

There are generally three different types of interactions with Splot
devices:

*   Initial *commissioning* and device setup
*   General-purpose *monitoring/control* (like from a dedicated app)
*   *Device-to-Device* Automation

These use cases present significantly different requirements with
respect to security:

* Initial commissioning requires a relatively
simple (but secure) way to authenticate to the device when physical
proximity can be proven.
* Monitoring/control should support delegation,
adding/removing authorized devices, as well as differing levels of
access.
* Device-to-device automation does not need any of this and instead require
extremely narrow access.

Both monitoring/control and device-to-device
require the use of secure multicast, but for different reasons. The
SSM attempts to satisfy all of these use cases with a
single security framework.

## Design Goals

*   Vendor neutral: Does not require a Google account to work
*   Decentralized: Not dependent on internet access
*   Implementable on constrained devices
*   Enable long-lived behavioral associations between devices:
    The ability to survive device ownership changes while remaining secure

These are features which are informing the design and architecture of
the Splot Security Model, but not yet formally defined:

*   Delegation: Give temporary, limited, and revocable access to
    visitors without interacting with every device.
*   Attestation: Allows devices to determine with high confidence if
    other devices are certified for a given purpose.

## Terminology ##

Access Rule:
A rule that defines what actions are allowed or disallowed for a given *identity*.

Attestation Certificate:
A certificate that is used to strongly indicate that the given
device meets certain requirements, such as being manufactured
by a specific manufacturer or meeting certain regulatory requirements.

Certificate
: A data structure that contains a public key and additional data.

Client
: A device that sends requests to servers.

DTLS
: Datagram Transport Layer Security, as defined in [RFC6347][https://tools.ietf.org/html/rfc6347].

Identity
: A named set of mandatory access control rules on a given
*server*. The scope of an identity is limited to the server.

Keychain
: Mechanisms for managing a collection of keychain items.

Keychain Item
: Either a *shared secret*, *password*, or *certificate*.

Keychain item identifier
: A string uniquely identifying a keychain
item on a given device.

OSCORE
: A method for application-layer protection of the Constrained
Application Protocol (CoAP) using CBOR Object Signing and Encryption
(COSE), providing end-to-end protection between endpoints
communicating using CoAP or CoAP-mappable HTTP. Defined in
[RFC8613][https://tools.ietf.org/html/rfc8613].

PAKE
: Password Authenticated Key Exchange. Describes any secure
mechanism for establishing a strong, high-entropy shared key using a
low-entropy password.

Password
: A low-entropy secret that is shared between two parties to
allow them to mutually authenticate to each other.

SPSKE
: Single-Purpose Shared Key Establishment, a way for two devices to
  generate a shared key without the administrator knowing the key.

Security Session
: A secure channel between a *client* and one or more *servers*

Server
: A device that responds to requests from *clients*.

Shared Secret
: A high-entropy secret that is shared amongst two or
more devices that allow them to mutually authenticate to each other.
