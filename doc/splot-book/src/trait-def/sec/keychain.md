# Keychain Trait (`keyc`)


Trait for managing a cryptographic keychain.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:keychain:v1:v0#r0` |
| Short-Id | `keyc` |
| Has-Children | yes |



## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/keyc/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |

### `m/keyc/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|



## Methods

| Key | Description |
|-----|-------------|
| `f/keyc?create` | Creates a new keychain item |

### `f/keyc?create` : Create

Creates a new keychain item.

| Arg | Req | Returns | Description |
|-----|-----|---------|-------------|
| `type` | X | integer | Keychain item type parameter. |
| `secr` |  | byte string | Symmetric shared secret, or private key if `cert` is present. |
| `cert` |  | byte string | Certificate parameter. |
| `iden` |  | text string | Identity to assume for any client using this item. |


Returns URI for the created keychain item.
