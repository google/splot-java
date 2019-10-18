## Timers

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
