# Remote Monitoring and Control

While not a part of the the original design consideration,
the fact that SMCP is built on top of CoAP provides a straightforward path to
enabling secure, low-latency remote monitoring and control.

There are a few different possible approaches, of which I will sketch out two:

1. Local Reverse CoAP Proxy
2. Direct DTLS tunnel from the device to the cloud service

In both cases, the connection from the remote phone to whatever server is being used
would be secured with DTLS, but in the more secure version OSCORE would additionally
be used inside of that tunnel to secure the messaging with end devices. This ensures
that the access control rules remain in-force.

## Local Reverse CoAP Proxy

In this case, devices would register themselves with a special reverse CoAP proxy on the local network.
These devices would not themselves need internet access, but the reverse CoAP proxy would have internet
access.

Once registered, the reverse proxy can forward remote requests it receives to devices on the local network.
The remote requests themselves can reach the proxy in one of two ways:

1. The local reverse proxy registers itself with a cloud service, which is either another reverse proxy or
   some proprietary protocol.
2. The local reverse proxy uses [Dynamic DNS][] and [NAT-PMP][]/[UPnP][] to become its own cloud server.

[UPnP]: https://en.wikipedia.org/wiki/Universal_Plug_and_Play
[NAT-PMP]: https://tools.ietf.org/html/rfc6886
[Dynamic DNS]: https://en.wikipedia.org/wiki/Dynamic_DNS

### Pros and Cons

* Pros:
   * Controlled devices (except, of course, the reverse proxy) can be firewalled off from direct
     internet access, reducing attack surface area.
   * Remote control of groups is straightforward.
   * Can use [Dynamic DNS][] and [NAT-PMP][]/[UPnP][] to avoid use of cloud proxy server.
   * Relatively off-the-shelf use of CoAP proxy servers.
* Cons:
   * Requires some additional hardware on the local network.

## Direct DTLS Tunnel

In this case, the device would be configured to register itself with a special
cloud-based CoAP caching proxy server. This registration would happen using DTLS
to secure the connection. Once the registration is complete, that DTLS tunnel is
used to send requests back to the device.

### Pros and Cons

* Pros:
   * Very low-latency
   * No additional hardware required
* Cons:
   * Requires each device to send keep-alive packets to ensure firewall doesn't
     close the UDP port. (Not a problem for sleepy devices)
   * Requires each device to have at least some limited amount of internet access.
     Should be heavily firewalled to limit attack surface area.
   * Proper support for groups requires devices to keep cloud proxy informed about
     group membership, adding some mild complexity.
   * Group control operations may "Popcorn".
