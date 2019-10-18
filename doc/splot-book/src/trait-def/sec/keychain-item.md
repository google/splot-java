# KeychainItem Trait (`kcit`)


Trait for managing an item in a cryptographic keychain.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:keychain-item:v1:v0#r0` |
| Short-Id | `kcit` |
| Has-Children | no |
| Requires | `tag:google.com,2018:m2m:traits:base:v1:v0#r0`|



## Config Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Identity | `c/kcit/iden` | X | X |   | The identity associated with this key. |

### `c/kcit/iden` : Identity

The identity associated with this key.

| Attribute | Value |
|----:|-------------|
| Value Type | nullable text string |
| Flags | `RW`|

This field determines what identity to assume when a client uses this keychain item to authenticate.

Default value is `anon`.

This may be absent if the certificate is not associated with an identity.

## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/kcit/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |
| Type | `m/kcit/type` | X |   | X | Specifies what type of key this item contains. |
| Ours | `m/kcit/ours` | X |   |   | Determines if this certificate item has a private key. |
| Certificate | `m/kcit/cert` | X |   |   | DER-encoded X.509 Certificate |
| HashSha256 | `m/kcit/sha2` | X |   |   | SHA256 hash of the certificate. |
| SecretShare | `m/kcit/sssh` | ? | X |   | Contains one (of *m*) shares of the secret. |
| SecretShareVersion | `m/kcit/sssv` | X | X |   | The version of the secret share |

### `m/kcit/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|



### `m/kcit/type` : Type

Specifies what type of key this item contains.

| Attribute | Value |
|----:|-------------|
| Value Type | integer |
| Flags | `CONST`, `REQ`|



* 0 = x.509 certificate
* 1 = password
* 2 = AES128 key

### `m/kcit/ours` : Ours

Determines if this certificate item has a private key.

| Attribute | Value |
|----:|-------------|
| Value Type | boolean |
| Flags | `CONST`|

If this item is a certificate without a private key, this value is false. Otherwise it is true. This field is only required when the contained key is asymmetric (public/private).

### `m/kcit/cert` : Certificate

DER-encoded X.509 Certificate.

| Attribute | Value |
|----:|-------------|
| Value Type | byte string |
| Flags | `CONST`|

If the key for this item is asymmetric, then this property contains the public portion. For example, for X.509 keys this would contain the public certificate. If the underlying key is symmetric, then this property is absent.

### `m/kcit/sha2` : HashSha256

SHA256 hash of the certificate.

| Attribute | Value |
|----:|-------------|
| Value Type | byte string |
| Flags | `CONST`|



### `m/kcit/sssh` : SecretShare

Contains one (of *m*) shares of the secret.

| Attribute | Value |
|----:|-------------|
| Value Type | nullable byte string |
| Flags | `OPT_GET`, `SET`|

This property can only be read by someone who has authenticated with the “init” identity. It is only present on keychain items where `m/kcit/ours` is *true*. It allows someone to reconstruct the administrative credentials of the network without requiring a factory reset of every device. This allows an administrator to reconstruct their credential by only physically interacting with a subset of the devices in the administrative domain.

### `m/kcit/sssv` : SecretShareVersion

The version of the secret share.

| Attribute | Value |
|----:|-------------|
| Value Type | nullable integer |
| Flags | `RW`|

This property MUST be updated every time the secret share is updated.
