# Identity Trait (`iden`)


Trait representing an identity.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:identity:v1:v0#r0` |
| Short-Id | `iden` |
| Has-Children | yes |
| Requires | `tag:google.com,2018:m2m:traits:base:v1:v0#r0`|

 This trait is a work in progress.

## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/iden/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |

### `m/iden/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|



## Methods

| Key | Description |
|-----|-------------|
| `f/iden?add` | Adds a new rule to the identity. |

### `f/iden?add` : AddRule

Adds a new rule to the identity.

| Arg | Req | Returns | Description |
|-----|-----|---------|-------------|
| `type` | X | integer | Keychain item type parameter. |
| `secr` |  | byte string | Symmetric shared secret, or private key if `cert` is present. |
| `iden` |  | text string | Identity to assume for any client using this item. |
| `cert` |  | byte string | Certificate parameter. |


Returns URI for the created keychain item.
