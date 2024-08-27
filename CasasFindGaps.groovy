import static sensors.casas.shortcuts.*

g = new ca.uqac.lif.cep.GroupProcessor(1, 1) {{
  s = Successive(ToList(2))
  f = s | Filter(GreaterThan(Minus(Timestamp(Element(1)), Timestamp(Element(0))), 24 * 3600 * 1000))
  addProcessors(s, f)
  associateInput(s)
  associateOutput(f)
}}

(
 Read(args) |
 Slice(SensorId(), g) |
 KeepLast() |
 ApplyFunction(ApplyToAll(Index(), Flatten(Values()))) |
 Write()
).run()