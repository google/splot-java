## Lost Secret Recovery ##

Keychain items can optionally support the ability for someone with
physical access to *n* of *m* devices within a certain window of time
to recover the underlying secret or private key.

Each keychain item has a nominally write-only field that is intended
to be used to contain a share of the secret (calculated using
[libgfshare](http://www.digital-scurf.org/software/libgfshare)). The
value of this field can only be read by the "init" identity, but can
be written by the “admin” identity.

This allows for a new owner of a house to securely take over the
network without performing a factory reset on every single device, as
long as the new owner can physically manipulate at least *n* devices
within the share secret reset period (generally months, but may be
indefinite).

The shares are periodically recalculated, such as when a device is
removed from the network or when a new device is added to the network.
