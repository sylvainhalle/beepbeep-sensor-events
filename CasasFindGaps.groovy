import static sensors.casas.hh.shortcuts.*

(
 Read(args) |
 Slice(SensorId(), new Group() {{
   input(Successive(ToList(2))) |
   output(Filter(GreaterThan(Minus(Timestamp(Element(1)), Timestamp(Element(0))), Day(1))))
 }}) |
 KeepLast() |
 ApplyFunction(ApplyToAll(Index(), Flatten(Values()))) |
 Write()
).run()