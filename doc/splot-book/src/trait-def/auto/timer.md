# AutomationTimer Trait (`timr`)


Experimental trait representing a schedule or timer.

| Attribute | Value |
|----:|-------------|
|  Id | `tag:google.com,2018:m2m:traits:timer:v1:v0#r0` |
| Short-Id | `timr` |
| Has-Children | no |
| Requires | `tag:google.com,2018:m2m:traits:actionable:v1:v0#r0`|

 An automation timer allows you to trigger events to occur at certain times. They may be repeating or restartable.

## State Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| Next | `s/timr/next` | X |   |   | The number of seconds until the timer fires next. |
| Running | `s/timr/run` | X | X | X | Flag indicating if the timer is running or not. |

### `s/timr/next` : Next

The number of seconds until the timer fires next.

| Attribute | Value |
|----:|-------------|
| Value Type | real number |
| Flags | `GET`|

This value is not cacheable. A value of zero indicates that this timer has fired and is not running.

### `s/timr/run` : Running

Flag indicating if the timer is running or not.

| Attribute | Value |
|----:|-------------|
| Value Type | boolean |
| Flags | `REQ`, `RW`|

Setting this to false will disarm the timer. Setting this to true will (re)arm the timer.
When arming with [`CONF_SCHEDULE_PROGRAM`] specified, [`STAT_NEXT`] will be
recalculated. If [`CONF_SCHEDULE_PROGRAM`] is empty, then the timer
will resume from its previous remaining time. If the timer is already
running, setting this property to true will do nothing.


## Config Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| ScheduleProgram | `c/timr/schd` | X | X | X | Schedule program. |
| PredicateProgram | `c/timr/pred` | X | X | X | Predicate program. |
| AutoReset | `c/timr/arst` | X | X | X | Auto restart flag. |
| AutoDelete | `c/timr/adel` | X | X |   | Auto delete flag. |

### `c/timr/schd` : ScheduleProgram

Schedule program.

| Attribute | Value |
|----:|-------------|
| Value Type | text string |
| Flags | `REQ`, `RW`|

This RPN expression is used to calculate the number
of seconds until the next fire time.

If the expression returns a value that is not a positive number the
timer is stopped.

Some implementations may strictly limit how this program is
structured.

#### Available variables ##

*   `c`: The value of [`actn#STAT_COUNT`]
*   `rtc.tod`: Pushes Time of day, in hours. 0.000 - 23.999.
*   `rtc.dow`: Pushes Day of week. 0-6. Monday is 0, Sunday is 6.
*   `rtc.dom`: Pushes Day of month. Zero-based Integer, 0-30.
*   `rtc.moy`: Pushes Current month. zero-based Integer, 0 = January,
    11 = December
*   `rtc.awm`: Aligned week of month. Pushes Number of times this
    weekday has happened this month. zero-based Integer.
*   `rtc.wom`: Pushes Week of month. zero-based Integer.
*   `rtc.woy`: Pushes Week of year. zero-based Integer, 0-51. Week
    starts on monday.
*   `rtc.y`: Pushes gregorian year.

#### Additional functions/operators/flags ##

*   `rtc.wss`: Indicates that future instances of `rtc.woy` and
    `rtc.dow` should be calculated with the week starting on Sunday.
    Otherwise, they will assume the week starts on Monday. Pushes
    nothing to the stack.
*   `rtc.utc`: Indicates that all time-based operators should use UTC
    instead of local time. Otherwise, all time-based operators use
    local time. Pushes nothing to the stack.
*   `H>S`: Convert hours to seconds
*   `D>S`: Convert days to seconds

#### Examples ##

*   `12 rtc.tod - 24 % H>S`: Every day at noon
*   `13.5 rtc.tod - 24 % H>S`: Every day at 1:30 PM
*   `12 rtc.tod - 24 % H>S 1 rtc.dow - 7 % D>S +`: Every tuesday at
    noon
*   `20 RND 2 * + rtc.tod - 24 % H>S`: Every day at some random time
    between 8pm and 10pm


### `c/timr/pred` : PredicateProgram

Predicate program.

| Attribute | Value |
|----:|-------------|
| Value Type | text string |
| Flags | `REQ`, `RW`|

This program is evaluated whenever the timer
expires. If the predicate evaluates to true, then the actions are
fired. If it evaluates to false, then the timer is reset, and
[`actn#STAT_COUNT`] is not incremented. The intent of the predicate is
to allow for complex schedules to be implemented, such as "every
second tuesday of the month at noon" or "Every easter at 9am". Some
implementations may strictly limit how this program is structured.

#### Available variables ##

*   `c`: The value of [`actn#STAT_COUNT`]
*   `rtc.tod`: Pushes Time of day, in hours. 0.000 - 23.999.
*   `rtc.dow`: Pushes Day of week. 0-6. Monday is 0, Sunday is 6.
*   `rtc.dom`: Pushes Day of month. Zero-based Integer, 0-30.
*   `rtc.moy`: Pushes Current month. zero-based Integer, 0 = January,
    11 = December
*   `rtc.awm`: Aligned week of month. Pushes Number of times this
    weekday has happened this month. zero-based Integer.
*   `rtc.wom`: Pushes Week of month. zero-based Integer.
*   `rtc.woy`: Pushes Week of year. zero-based Integer, 0-51. Week
    starts on monday.
*   `rtc.y`: Pushes gregorian year.

If the RTC has not been set (or is not present) then none of the 'rtc'
variables are present.

#### Additional functions/operators/flags ##

*   `rtc.wss`: Indicates that future instances of `rtc.woy` and
    `rtc.dow` should be calculated with the week starting on Sunday.
    Otherwise, they will assume the week starts on Monday. Pushes
    nothing to the stack.
*   `rtc.utc`: Indicates that all time-based operators should use UTC
    instead of local time. Otherwise, all time-based operators use
    local time. Pushes nothing to the stack.
*   `H>S`: Convert hours to seconds
*   `D>S`: Convert days to seconds

#### Examples ##

*   `2 rtc.dow == 1 rtc.awm == &&`: Only fire on the second Wednesday
    of the month.
*   `0.2 RNG &gt;`: Only fire 20% of the time.
*   `1 rtc.moy == 28 rtc.dom &&`: Only fire on leap day


### `c/timr/arst` : AutoReset

Auto restart flag.

| Attribute | Value |
|----:|-------------|
| Value Type | boolean |
| Flags | `REQ`, `RW`|

If this flag is true, then the timer will automatically restart after firing. It will continue to run until it is explicitly stopped or until the schedule program fails to return a positive number.

### `c/timr/adel` : AutoDelete

Auto delete flag.

| Attribute | Value |
|----:|-------------|
| Value Type | boolean |
| Flags | `RW`|

If this flag is true, then the timer will automatically delete itself
once [`STAT_RUNNING`] *automatically* transitions from true to false.
Explicitly setting [`STAT_RUNNING`] to false will NOT cause the timer
to be deleted. Thus, it does make sense to have cases where both this
flag and `#CONF_AUTO_RESET` are both true. In such a case, deletion
will be triggered by [`CONF_SCHEDULE_PROGRAM`] returning a
non-positive value.


## Metadata Properties

| Name |  Key | R | W |  Req |  Description |
|-----|---|----|----|----|----|
| TraitURI | `m/timr/turi` | X |   | X | The URI that uniquely identifies the specification used to implement this trait. |

### `m/timr/turi` : TraitURI

The URI that uniquely identifies the specification used to implement this trait.

| Attribute | Value |
|----:|-------------|
| Value Type | URI-reference |
| Flags | `CONST`, `REQ`|



## Methods

| Key | Description |
|-----|-------------|
| `f/timr?reset` | Method for resetting the timer. |

### `f/timr?reset` : Reset

Method for resetting the timer.


Calling this method will always restart the timer, even if the timer is already running.
