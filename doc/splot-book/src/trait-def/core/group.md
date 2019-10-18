# Group Trait (`grup`)


Trait for groups of things.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:group:v1:v0#r0` |
| Short-Id | `grup` |
| Has-Children | no |



## Config Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Members | `c/grup/memb` | X | ? |   | URLs of group members |
| GroupAddress | `c/grup/addr` | X | ? |   | Multicast Group Address |
| Version | `c/grup/vers` | X | ? |   | Incremented whenever group configuration has changed. |

### `c/grup/memb` : Members

URLs of group members.

| Attribute | Value |
|----:|-------------|
| Value Type | array containing URI-references |
| Flags | `GET`, `OPT_SET`, `OBS`|



### `c/grup/addr` : GroupAddress

Multicast Group Address.

| Attribute | Value |
|----:|-------------|
| Value Type | text string |
| Flags | `GET`, `OPT_SET`|



### `c/grup/vers` : Version

Incremented whenever group configuration has changed.

| Attribute | Value |
|----:|-------------|
| Value Type | integer |
| Flags | `GET`, `OPT_SET`|



## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/grup/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |

### `m/grup/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|



## Associated Constants

| Name | Value | Description |
|-----|------|-------|
| `PARAM_GROUP_ID` | "gid" | Group ID |
