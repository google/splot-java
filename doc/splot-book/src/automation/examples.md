# Pulling it All Together

Let's say we've got a SMCP-compatible device that has two buttons, and
we want to use it to control the brightness of an SMCP-compatible
lightbulb. Here is what we want to happen from a user perspective:

*   One button dims the light, the other button makes it brighter.
*   Holding down a button causes the brightness to change continuously
    until the button is released.

## First Attempt

There are several different ways to do this. One inefficient way
that we can implement this uses two timers, two pairings, and a rule:

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

### Edge Cases

This does indeed work, but the fact that we have a timing schedule
of `c 0 == IF 0.001 ELSE 0.4 ENDIF` is rather ugly. More importantly,
it also hints at a small flaw: When the user presses both buttons at
the same time, the timers would be firing at the same time: producing
weird behavior until the user releases one of the buttons.

## Second Attempt

We can fix this by replacing the two pairings with two rules. This
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
