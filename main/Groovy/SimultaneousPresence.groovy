/**
 * At every point in time where an event is emitted, counts the number of
 * motion sensors currently in the "ON" state.
 */
import static beepbeep.groovy.*
import static sensors.shortcuts.*
import static sensors.casas.aruba.shortcuts.*

Read(args) |
Slice(SensorId(), ApplyFunction(Equals("ON", State()))) |
ApplyFunction(Size(Maps.FilterMap(Equals(Y, true)))) |
Write()