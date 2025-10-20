/**
 * For each contact sensor, counts the number of times it violates its
 * lifecycle (i.e. two successive OPEN or CLOSED events).
 */
import static beepbeep.groovy.*
import static sensors.shortcuts.*
import static sensors.casas.hh.shortcuts.*

Read(args) |
Filter(Or(Equals(State(), OPEN), Equals(State(), CLOSED))) |
Slice(SensorId(), CountIf(Successive(Equals(State(X), State(Y))))) |
KeepLast() |
Write()