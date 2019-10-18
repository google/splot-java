## Identities ##

An *identity* is a named set of mandatory access control rules on
a given *server*. The scope of an identity is limited to that
server. Identities can be managed in-band as *Things*.

When establishing a session, a client uses the *keychain item
identifier* to establish the session, *not the *identity*
(which is purely a server-side concept). An identity can be
associated with multiple *keychain items*, whereas a keychain
item identifier identifies a single keychain item on a device.

### Standard Identities ###

The security model has five pre-defined identities with standardized
names and behaviors: Admin (NOTE:  The name "root" was specifically
avoided to prevent confusion about what an identity with such a name
would be capable of.), Init, User, Read, and Anon.

*   `admin` — Used by the administrator of the device. `admin`
    may read, write, or mutate any property in any section of any Thing
    on this device. May initiate software updates, manage the
    keychain, manage permissions, and manage connectivity. There are
    two actions that are not allowed: A factory reset MUST NOT be
    allowed. Also, this identity MUST NOT be allowed to read the
    secret share of any keychain item (writing is allowed).
*   `init` — Special identity that can only be assumed when the
    device pairing code is used to authenticate. It assumes all of the
    permissions of “admin”, but can also perform a factory reset
    and/or read secret shares from items in the keychain. The ability
    to use this identity generally requires physical access to the
    device to put it into a particular state where you can
    authenticate using the pairing code. Intended only for initial
    commissioning and recovering administrative credentials. Unlike
    all other built-in identities, capabilities associated with this
    identity cannot be changed.
*   `user` — For identities of the device. `user` may read,
    write, or mutate all properties in the operational state of any Thing
    on this device, and may read any property from any Thing on this
    device (same exceptions as `admin`). No access is granted to
    any other resource.
*   `read` — For read-only identities of the device. `read` may
    read any property in any section of any Thing on this device (same
    exceptions as `admin`).  No access is granted to any other
    resource.
*   `anon` — Used when the client did not authenticate itself to the
    server. By default, all requests will be denied (can be overridden
    for certain circumstances).

Additional custom identities can be created with very limited scopes.
All standardized identities (except `init`) can have their ACLs
overridden.

### Access Rules ###

The access rules for an identity allow access control decisions based
on:

*   REST Method (GET, PUT, POST, DELETE, etc)
*   Path (Can start or end with a wildcard)
*   Query (can match individual fields)
*   Presence of CoAP observation/block-transfer options
*   Match rules from another *identity*
