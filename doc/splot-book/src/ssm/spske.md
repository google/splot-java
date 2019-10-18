## Single-Purpose Shared Key Establishment ##

Single-Purpose Shared Key Establishment (SPSKE) is a mechanism for
establishing a shared-secret keychain item on two or more devices
in-band without an administrator ever learning the shared secret. This
enables the establishment of long-lived automation relationships. This
shared key can then be associated with a very limited single-purpose
identity to allow devices to perform actions on each other without
granting the ability to perform unauthorized actions. The intent is to
enable a "capability-like" security model.

There are two variants: one for establishing a shared key between two
devices, and one for establishing a shared key between a group of
devices. The exact mechanism is TBD, but it would generally work like
this:

*   The administrator calculates a temporary shared secret, *P*.
*   A "prepare-SPSKE" command is sent to Device A with the desired key
    identifier and the value of *P*.
    *   Upon success, a location *LOC* is returned, which (if
        relative) is expanded to a fully specified URI. When the
        process is finished, this will be the address of the newly
        created keychain item for Device A. Next step must be
        completed within five minutes or *LOC* will become
        invalid.
*   A "calc-SPSKE" command is sent to Device B with the desired key
    identifier, the value of *P*, and the value of *LOC*.
    *   Upon success, a location to the newly created keychain item
        for Device B is returned. *LOC* can then be considered the
        location of the newly created keychain item for Device A.
*   The administrator can then configure the newly created keychain
    items as appropriate, without ever learning the shared secret they
    contain.
