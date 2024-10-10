/**
 * Establishes the "profile" of sensors producing numerical values.
 */
import static beepbeep.groovy.*
import static sensors.shortcuts.*
import static sensors.casas.aruba.shortcuts.*

Read(args) |
Filter(IsNumeric()) |
Slice(SensorId(), new Group() {{
  in(ApplyFunction(State())) |
  Sets.PutInto() |
  out(ApplyFunction(BoxAndWhiskers()))
}}) |
KeepLast() |
ApplyFunction(PrettyPrint()) |
Write()