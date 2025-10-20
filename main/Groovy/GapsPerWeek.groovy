/**
 * Counts the number of times two successive events in the log are separated
 * by more than one hour, and groups the results by week.
 */
import static beepbeep.groovy.*
import static sensors.shortcuts.*
import static sensors.casas.hh.shortcuts.*

// The minimum duration of a gap; set as a constant that can be modified
DURATION = Hours(1)

Read(args) |
SliceBy(GetYearWeek(Timestamp(X)), 
CountIf(new Group() {{
  in(Successive(Minus(Timestamp(Y), Timestamp(X)))) |
  out(ApplyFunction(GreaterThan(X, DURATION)))
  }})
) |
Write()