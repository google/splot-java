## Security Sessions ##

Secure requests sent from a client to one or more servers (and
responses vise-versa) are performed within the context of a
*security session* established between the devices.

Security sessions are secure channels between a *client* (who
initiates the session and sends requests) and one or more *servers*
(which can respond to the requests). The creation of a security
session is always initiated by the client. If the server needs to send
requests to the client, a new security session SHALL be established
where the roles of the devices are reversed.

On the client side, a security session is uniquely identified by a
tuple of the following information:

*   The URL of the server endpoint
*   The client keychain item used to authenticate the client to the
    server.
*   A (possibly empty) set of acceptable client keychain items that
    the server can use to authenticate itself.

Any request from a client that needs a security session with the same
three properties above can re-use any existing security session with
those properties. Otherwise, a new security session MUST be
established.

On the server side, a security session has the following properties:

*   The *identity* to assume for any incoming request from the
    client, determined from the keychain item on the server that the
    client used to initially authenticate. This *identity*
    determines what actions the *client* may perform on the
    *server* using this security session.
*   *OPTIONAL:* The set of keychain items that the client associated
    with this session has proven it satisfies. Used for attestation.
    First item is the one that determined the identity. Usually only
    contains one item. This may be empty if no authentication has
    taken place.
