/**
 * Establishes the "profile" of sensors producing numerical values.
 */
import static beepbeep.groovy.*
import static sensors.shortcuts.*
import static sensors.casas.aruba.shortcuts.*

(
  Read(args) |
  Filter(IsNumeric()) |
  Slice(SensorId(), new Group() {{
    def s = in(ApplyFunction(State()))
    def p = Sets.PutInto()
    def af = s | p | out(ApplyFunction(BoxAndWhiskers()))
    addProcessors(s, p, af)
  }}) |
  KeepLast() |
  ApplyFunction(PrettyPrint()) |
  Write()
).run()