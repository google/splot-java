# AutomationRule Trait (`rule`)


Experimental trait representing an automation rule.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:rule:v1:v0#r0` |
| Short-Id | `rule` |
| Has-Children | no |
| Requires | `tag:google.com,2018:m2m:traits:actionable:v1:v0#r0`|

 An automation rule allows you to create if-this-then-that style relationships across things that are associated with the same technology.

## Config Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Conditions | `c/rule/cond` | X | X | X | Criteria table for determining when the action should fire. |
| Match | `c/rule/mtch` | X | X | X | Match-all-criteria vs. Match-any-criteria |

### `c/rule/cond` : Conditions

Criteria table for determining when the action should fire.

| Attribute | Value |
|----:|-------------|
| Value Type | array containing maps of nullable splot values |
| Flags | `REQ`, `GET`, `SET`|

All of the given criteria must be satisfied for the action to fire. Each criteria
is defined as a map keyed by strings. The string keys are the
following:

*   `p` [`PARAM_COND_PATH`]: URL or absolute path to the resource
    being evalu
*   `c` [`PARAM_COND_EXPR`]: RPN condition to evaluate.
*   `s` [`PARAM_COND_SKIP`]: True if this condition should be skipped.
*   `d` [`PARAM_COND_DESC`]: Human-readable description of the
    criteria

If a `path` is present, then this value is observed. When the observed
path changes, it's previous value is pushed onto the stack, followed
by the just observed new value. `cond` is then evaluated. After
evaluation, if the top-most item on the stack is greater than or equal
to 0.5, then the condition is considered satisfied. Once all
conditions are considered satisfied, the action fires. If the path is
empty, the value "1.0" is passed onto the stack. Some technologies may
have strict requirements on how the condition string is formatted,
since not all technologies directly support evaluating arbitrary RPN
expressions. In the RPN evaluation context, the following additional
operators are available:

*   `rtc.tod`: Pushes Time of day, in hours. 0.000 - 23.999.
*   `rtc.dow`: Pushes Day of week. One-based Integer, 1-7. Monday is
    1, Sunday is 7.
*   `rtc.dom`: Pushes Day of month. One-based Integer, 1-30.
*   `rtc.moy`: Pushes Current month. One-based Integer, 1 = January,
    12 = December
*   `rtc.dim`: Pushes Number of times this weekday has happened this
    month. One-based Integer.
*   `rtc.wom`: Pushes Week of month. One-based Integer.
*   `rtc.woy`: Pushes Week of year. One-based Integer, 1-52. Week
    starts on monday.
*   `rtc.wss`: Indicates that future instances of `rtc.woy` and
    `rtc.dow` should be calculated with the week starting on Sunday.
    Otherwise, they will assume the week starts on Monday. Pushes
    nothing to the stack.
*   `rtc.utc`: Indicates that all time-based operators should use UTC
    instead of local time. Otherwise, all time-based operators use
    local time. Pushes nothing to the stack.
*   `dt_dx`: Pushes seconds since the path was last changed, max value
    if never.
*   `dt_cs`: Pushes seconds since this condition was last satisfied,
    max value if never.
*   `dt_rt`: Pushes seconds since the enclosing rule was last
    triggered, max value if never.


### `c/rule/mtch` : Match

Match-all-criteria vs. Match-any-criteria.

| Attribute | Value |
|----:|-------------|
| Value Type | text string |
| Flags | `REQ`, `GET`, `SET`|

This is either "any" or "all". Default is "all".

## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/rule/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |

### `m/rule/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|



## Associated Constants

| Name | Value | Description |
|-----|------|-------|
| `MATCH_ALL` | "all" | Match all criteria. |
| `MATCH_ANY` | "any" | Match any criteria. |
| `PARAM_COND_PATH` | "p" | Path for condition. Optional. |
| `PARAM_COND_EXPR` | "c" | Expression for evaluating if the condition is satisfied. |
| `PARAM_COND_SKIP` | "s" | Flag indicating if this condition should be skipped. |
| `PARAM_COND_DESC` | "desc" | Human readable description of the rule. Optional. |
