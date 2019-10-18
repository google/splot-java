# Keychain

The keychain is the manager of cryptographic secrets and certificates,
referred to as *keychain items*.

## Keychain Items ##

A *keychain item* can be either a *shared secret*,
*password*, or *certificate* (with or without a private key).
Conceptually, keychain items are used to authenticate devices to each
other, and in practice may also be simultaneously used for
establishing cryptographic session keys.

All keychain items have at least the following attributes:

*   Identifier used to identify the item during establishment of the
    *security session*.
*   Human-readable Administrative label
*   Object type (Shared Secret, Password, or Certificate)
*   Associated local *identity* (Default: "anon")
*   A flag indicating if the keychain item is enabled.
*   A flag indicating if the keychain item is permanent.
*   *Lost secret recovery share*

Ideally, the private/secret part of each keychain item would be stored
in a secure element ("hardware backed").

### Shared Secrets ###

*Shared Secrets* allow for mutual authentication of client and
server. They are *high-entropy* and can be used directly in the
calculation of a shared session key. These are typically used to
secure device-to-device relationships and are associated with
identities which are very limited in scope.

### Passwords ###

*Passwords* also allow for mutual authentication of client and
server. They are *low-entropy*, so they must be used with a PAKE
algorithm to establish a strong shared session key. These items are
generally only used for initial device configuration, but the security
model defines them as a top-level keychain item type. From a
high-level perspective, they behave identically to *shared
secrets*.

### Certificates ###

*Certificates* allow for one-way authentication between a server
and a client. The exact implementation details are TBD, but they are
expected to be based on X.509. Certificates are generally used for
direct administration rather than for automation.

Conceptually, there are two types of certificate keychain items: those
with private keys and those without. A *certificate with a private
key* allows the device to authenticate itself to other devices. It
is not used to authenticate other devices, and thus these keychain
items are not typically associated with an identity other than "anon".

A *certificate without a private key* only allows the device to
authenticate other devices that have a private key with this
certificate or one signed by it.

For example, the administrator device (smart phone) has a self-signed
admin certificate for devices in its domain. The administrator would
install this admin certificate (without a private key) onto a managed
device at commissioning and associate it with the "admin" identity.
The administrator device can then be used to securely manage any
devices configured in this way.

## Permanent Keychain Items ##

There are currently three types of "permanent" keychain items that are
used for device identification and initial commissioning:

### Self-signed Certificate + Private Key ###

The only purpose of this keychain item is to uniquely identify the
device. It is recalculated at factory reset to ensure privacy. This
certificate is used by a server when the client wants to use a client
certificate and doesn’t specify a key identifier for the server.
This keychain item has an identifier of "self" if direct selection is
desired.

### Attestation Certificate + Private Key ###

This certificate is anonymized using batch keys, similar to FIDO
tokens. It is not associated with an identity: It is used for
additional attestation (for things like certification requirements) of
an already established security session. The keychain identity of
attestation certificates is largely defined by the manufacturer, but
the following values are recommended:

*   "mfgr" for manufacturer attestation

*   "ecos" for ecosystem attestation

*   "reg" for regulatory attestation

### Device Setup Code/Password ###

Security sessions created using this keychain item are always
associated with the "init" identity. It shall only be used when
physical proximity can be assured. This keychain item has an
identifier of “init”. The value for this secret may be permanent
(read off of a label on the device) or calculated dynamically
(displayed after pressing a button).
