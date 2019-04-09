# Automation Primitive Introduction #

One of the key design goals for Splot and SMCP was to support the use
of in-band configurable automation primitives, enabling complex
device-to-device relationships that have no external dependencies
(like internet access) for correct operation. Automation primitives
can act like virtual wires, making properties on different devices
dependent on each other, or they can act as scheduled timers that
trigger actions at programmable times of the week. Additionally, since
these automation primitives are defined in terms of simple JSON/CBOR
values and URIs, they can be used to automate devices which have a
RESTful interface but don't necessarily support SMCP or use the Splot
object model.

Splot for Java currently defines three types of automation primitives:

*   Pairings
*   Rules
*   Timers

Ultimately, the automation primitives described here are experimental.
There is always room for improvement and changes in behavior should be
expected until the protocol stabilizes.

## Pairings ##

Pairings are automation primitives that connect two resources, which
are referred to as the *source* and *destination*.

When configured for *push* operation, changes to the source are
*pushed* to the destination. When configured for *pull* operation,
changes are *pulled* from the destination to the source. Both
operational modes may be enabled simultaneously, allowing for changes
to either to be applied to the other.

In addition to basic value mirroring, two custom transforms can be
specified: a *forward transform* that is applied to changes from the
source to the destination, and a *reverse transform* that is applied
to changes from the destination to the source.

### Splot Automation Expressions ###

Pairing transforms are specified using Splot Automation Expressions, a
specialized postfix (Reverse polish notation) language that is
expressed as a string. The language was designed to be expressive yet
simple to implement on highly-constrained devices.

For pairing transformations, the value being transformed is initially pushed
onto the stack and the last value on the stack after evaluation is the
output fo the expression.

The syntax is somewhat similar to forth, but unlike forth all numeric
values are considered floating-point.

For example, the following transform would apply *x²* to source
values before they are applied to the destination:

*   `"2 ^"` (or `"DUP *"`)

If the pairing was bidirectional, then the reverse transform for the
above expression would be:

*   `"0.5 ^"`

In this case we are raising *x* to the power of 0.5, which reverses
raising it to the power of two from the forward transform.

This example was fairly simple, but much more complex transforms are
possible. For example, the infix expression *(cos(x/2 - 0.5) + 1)/2*
would become `"2 / 0.5 - COS 1 + 2 /"`. (Note that currently, the
trigonometric operations use *turns* instead of *radians*)

In cases where an expression is used on an input, the previous input
can be pushed on the stack using `v_l`. This allows for things like
edge detection and determining the direction of a change. For
convenience, the current input can also be pushed onto the stack using
`v`.

The ability to handle arrays and dictionaries is also specified. For
example, imagine that we have a color light and a text display. Our
goal is to display the [approximate correlated color temperature
(CCT)][approxCT] of the light on the text display in Kelvin. However,
in this example, the color light only gives us the [CIE xy
chromaticity coordinates][CIExy] in the form of an array with two
floating point values. We can start by creating a pairing between the
CIE xy coordinates and the text display value. To get the right
transformation expression, we first have to have the infix expression:

*   *CCT(x, y) = -449n³ + 3525n² - 6823.3n + 5520.33*
*   where *n = (x − 0.332)/(y - 0.1858)*

By using the `POP` operation to remove the last item from an array on
the stack and push that value onto the stack, we end up with the
following expression:

    POP 0.1858 - SWAP POP 0.3320 - SWAP DROP SWAP / -449 3525 -6823.3 5520.33 POLY3

[approxCT]: https://en.wikipedia.org/wiki/Color_temperature#Approximation
[CIExy]: https://en.wikipedia.org/wiki/CIE_1931_color_space#CIE_xy_chromaticity_diagram_and_the_CIE_xyY_color_space

Arrays can be built by pushing values onto the stack and invoking one
of the array creation operators:

*   `[]` Pushes an empty array onto the stack.
*   `[1]` Pops the last value off of the stack, puts it into an array,
    and pushes that array onto the stack.
*   `[2]` Pops the last two values off of the stack, puts them into an
    array, and pushes that array onto the stack.
*   `[3]` Pops the last three values off of the stack, puts them into
    an array, and pushes that array onto the stack.
*   `[4]` Pops the last four values off of the stack, puts them into
    an array, and pushes that array onto the stack.

Dictionaries (json "objects") can also be read and written. The
following expression calculates the vector length for an input vector
specified as `{"x":12,"y":14}`:

    :x GET DUP * SWAP :y GET DUP * SWAP DROP + 0.5 ^

A dictionary can be created by putting it together value-by-value. For
example, the following expression takes an input of "turns" and
converts that to a vector:

    {} OVER COS :x PUT OVER SIN :y PUT

If you wanted to do something similar with arrays, you have two options:

 * `[] OVER COS PUSH OVER SIN PUSH`
 * `DUP COS SWAP SIN [2]`

## Actionable Primitives ##

Rules and Timers are both actionable automation primitives, meaning
that they when either are "triggered" they invoke actions. The
machinery and interface that performs these actions is identical
between the two. When an actionable automation primitive is triggered,
it performs zero or more "actions". You can think of an action as
similar to a [webhook](https://en.wikipedia.org/wiki/Webhook).

An action is specified by the following important parameters:

*   A URI. Required.
*   A REST method (PUT, POST, DELETE, etc) to perform on that URI.
    Optional: defaults to POST.
*   A body to pass along while performing the method. Optional:
    defaults to empty.
*   An enumeration value that determines if the action should be
    evaluated synchronously or asynchronously, as well as what to do
    in the event of an error. Optional: defaults to asynchronous
    firing (later actions don't wait for this action to finish before
    firing).
*   Security context information. Optional: defaults to no security
    context.

## Rules ##

Rules are actionable automation primitives that consist of one or more
*conditions*. Each condition can evaluate to "true" or "false". A rule
can be configured to trigger when ALL conditions match or when ANY
condition matches.

A condition monitors a specific URI value and defines how that value
should be interpreted in the form of a Splot Automation Expression, as
described above. Similar to pairings, the previous value is pushed
onto the stack before the current value: allowing for edge triggers.

## Timers ##

Timers are actionable automation primitives that allow you to
implement the following types of time-based actions:

*   One-off timers
*   Repeating timers
*   Scheduled timers

The behavior of a timer is ultimately defined by two Splot Automation
Expressions:

*   Schedule
*   Predicate

Evaluating the schedule expression yields the number of seconds to
wait until the predicate expression should be evaluated. If the
predicate expression evaluates to *true* then the action(s) are
triggered. Otherwise, the schedule expression is re-evaluated and the
timer resets.

While this is more than enough for one-off timers and repeating
timers, it does not itself allow you to schedule events. To enable
that, Splot Automation Expressions supports several operations for
probing a real-time-clock:

*   `c`: The number of times this timer has fired since it was last
    reset.
*   `rtc.y`: Pushes Gregorian year.
*   `rtc.dow`: Pushes Day of week. 0-6. Monday is 0, Sunday is 6.
*   `rtc.dom`: Pushes Day of month. Zero-based Integer, 0-30.
*   `rtc.tod`: Pushes Time of day, in fractional hours. 0.000 -
    23\.999.
*   `rtc.moy`: Pushes Current month. zero-based Integer, 0 = January,
    11 = December
*   `rtc.awm`: Aligned week of month. Pushes Number of times this
    weekday has happened this month. zero-based Integer.
*   `rtc.wom`: Pushes Week of month. zero-based Integer.
*   `rtc.woy`: Pushes Week of year. zero-based Integer, 0-51. Week
    starts on monday.
*   `H>S`: Convert hours to seconds.
*   `D>S`: Convert days to seconds.

The combination of a schedule expression and a predicate expression
allow for the evaluation of very complex schedules (like easter) even
on constrained devices. The schedule expression is used to determine
*when* to check the predicate, whereas the predicate is used to
determine if it is indeed the right time.

For example, to schedule an event to happen at 1:30pm every second
Wednesday of the month, you could use the following expressions:

*   schedule: `"13.5 rtc.tod - 24 % H>S"` (Seconds until the next
    1:30pm)
*   predicate: `"2 rtc.dow == 1 rtc.awm == &&"` (Is it the second
    Wednesday of this month?)

This will cause the predicate to be evaluated every day at 1:30pm.
When the predicate finally evaluates to true on the second Wednesday
of the month, the actions are triggered.

## Putting it all together ##

Let's say we've got a SMCP-compatible device that has two buttons, and
we want to use it to control the brightness of an SMCP-compatible
lightbulb. Here is what we want to happen from a user perspective:

*   One button dims the light, the other button makes it brighter.
*   Holding down a button causes the brightness to change continuously
    until the button is released.

There are several different ways to do this. One way we can implement
this uses two timers, two pairings, and a rule:

*   Two timers, one for increasing brightness, one for decreasing
    brightness.
*   Two pairings, connecting the button state to the two timers.
*   One rule, triggering when one of the buttons is released and
    freezing any transition to the current value of the light.

Let's assume the following:

*   The increase button state value is at `/3/s/onof/v`
*   The decrease button state value is at `/4/s/onof/v`
*   The light brightness value can be changed using
    `coap://light/1/s/levl/v?inc&d=0.4`
    *   `inc`: causes the value to be incremented by the amount
        specified in the body.
    *   `d=0.4`: causes the value to be transitioned from the previous
        value over a period of 0.4 seconds.
*   POSTing 0 to `coap://light/1/s/tran/d` will stop any transition in
    progress.

This is how the primitives would be configured:

*   Timer 1: Value Increment Timer
    *   schedule: `c 0 == IF 0.001 ELSE 0.4 ENDIF`
        *   *0.001 seconds for first firing, 0.4 seconds otherwise*
    *   predicate: *empty*
    *   auto-reset: true
    *   action:
        *   `POST coap://light/1/s/levl/v?inc&d=0.4 0.1`
    *   Note: This timer can be enabled/disabled via POSTing a boolean
        to `/1/f/tmgr/1/s/enab/v`

*   Timer 2: Value Decrement Timer
    *   schedule: `c 0 == IF 0.001 ELSE 0.4 ENDIF`
        *   *0.001 seconds for first firing, 0.4 seconds otherwise*
    *   predicate: *empty*
    *   auto-reset: true
    *   action:
        *   `POST coap://light/1/s/levl/v?inc&d=0.4 -0.1`
    *   Note: This timer can be enabled/disabled via POSTing a boolean
        to `/1/f/tmgr/2/s/enab/v`

*   Pairing 1: Increase Button Pairing
    *   source: `/3/s/onof/v`
    *   destination: `/1/f/tmgr/1/s/enab/v`
    *   push: true
    *   pull: false
    *   forward transform: *empty*

*   Pairing 2: Decrease Button Pairing
    *   source: `/4/s/onof/v`
    *   destination: `/1/f/tmgr/2/s/enab/v`
    *   push: true
    *   pull: false
    *   forward transform: *empty*

*   Rule 1: Stop Inc/Dec Timers Rule
    *   condition 1
        *   uri: `/3/s/onof/v`
        *   expression: `! v_l &&`
            *   infix: *(v == false) && (v\_l == true)* (Condition is
                true only on the transition from true to false)
    *   condition 2
        *   uri: `/4/s/onof/v`
        *   expression: `! v_l &&`
            *   infix: *(v == false) && (v\_l == true)* (Condition is
                true only on the transition from true to false)
    *   match: ANY
    *   action
        *   `POST coap://light/1/s/tran/d 0`

When holds the increment button for half a second:

1.  `/3/s/onof/v` transitions from `false` to `true`.
    *   Pairing 1 notices and sets `/1/f/tmgr/1/s/enab/v` to the same
        value, `true`.
    *   Rule 1 notices and evaluates condition 1, which evaluates to
        `false`: No action is triggered.
2.  Timer 1 has been enabled
    *   Clears the counter back to zero
    *   Evaluates the schedule to determine how long to wait: 0.001
        seconds
3.  0\.001 seconds go by.
4.  Timer 1 schedule expires
    *   Evaluates predicate: true
    *   Triggers action `POST coap://light/1/s/levl/v?inc&d=0.4 0.1`
    *   Increments counter
    *   Evaluates the schedule to determine how long to wait: 0.4
        seconds
5.  Light starts incrementing by 10% over the next 0.4 seconds.
6.  0\.4 seconds go by.
7.  Timer 1 schedule expires
    *   Evaluates predicate: true
    *   Triggers action `POST coap://light/1/s/levl/v?inc&d=0.4 0.1`
    *   Increments counter
    *   Evaluates the schedule to determine how long to wait: 0.4
        seconds
8.  Light starts incrementing by another 10% over the next 0.4
    seconds.

When someone lets go of the increment button:

1.  `/3/s/onof/v` transitions from `true` to `false`.
    *   Pairing 1 notices and sets `/1/f/tmgr/1/s/enab/v` to the same
        value, `false`.
    *   Rule 1 notices and evaluates condition 1, which evaluates to
        `true`:
        *   Triggers action `POST coap://light/1/s/tran/d 0`
2.  Timer 1 has been disabled.
3.  Light stops any transition that is in progress.

But this setup has a small flaw: When the user presses both buttons at
the same time, the timers would be firing at the same time: producing
weird behavior until the user releases one of the buttons.

We could fix this by replacing the two pairings with two rules. This
new approach would better handles such edge cases, since each button
press could be configured with actions to gracefully turn off the
opposing timer. It would also allow us to remove the `0.001` second
hack. Let's see what that approach would look like:

*   Timer 1 is the same, except:
    *   schedule: `0.4`
*   Timer 2 is the same, except:
    *   schedule: `0.4`
*   Rule 1 is the same.
*   Rule 2: Inc Button Pressed Rule
    *   condition 1
        *   uri: `/3/s/onof/v`
        *   expression: `v_l ! &&`
            *   infix: *(v == true) && (v\_l == false)* (Condition is
                true only on the transition from false to true)
    *   action
        *   `POST /1/f/tmgr/2/s/enab/v false` (Synchronous)
        *   `POST coap://light/1/s/levl/v?inc&d=0.4 0.1` (Synchronous)
        *   `POST /1/f/tmgr/1/s/enab/v true`
*   Rule 3: Dec Button Pressed Rule
    *   condition 1
        *   uri: `/4/s/onof/v`
        *   expression: `v_l ! &&`
            *   infix: *(v == true) && (v\_l == false)* (Condition is
                true only on the transition from false to true)
    *   action
        *   `POST /1/f/tmgr/1/s/enab/v false` (Synchronous)
        *   `POST coap://light/1/s/levl/v?inc&d=0.4 -0.1`
            (Synchronous)
        *   `POST /1/f/tmgr/2/s/enab/v true`

Now whenever the user presses more than one button, the last button
pressed will be the one in effect. Note that letting go of *either*
button will still stop the transition, and if that's a problem you
would need to split up Rule 1 into two rules instead of having Rule 1
do double duty.

Notice how the actions are explicitly marked as synchronous: we only
want them to fire once the previous action has finished. This allows
us to eliminate race conditions.

## Recipes ##

It is not expected that end users would directly configure automation
primitives by hand---although they certainly could do that if they
wanted to. The expected way is that you would have applications (either
on your phone or in the cloud) that would configure and maintain these
automation pairings for you. Such applications can store some metadata
along with the automation primitives they are in charge of to help their
software make sense of them. There are also lots of places where such
applications are encouraged to include human-readable descriptions
and comments.

Which brings me to Recipes: Recipes are instructions for how automation
primitives can be combined in order to implement a specific, previously
defined behavior. Recipes provide instructions that allow software to
to create, manage, and update complex automation primitive chains.

