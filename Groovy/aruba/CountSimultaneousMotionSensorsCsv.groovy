import static sensors.casas.aruba.shortcuts.*
import ca.uqac.lif.cep.Connector

f = Read(args) | Fork()
t = ApplyFunction(Timestamp())
Connector.connect(f, 0, t, 0)
g =  new Group() {{
	in(Slice(SensorId(), ApplyFunction(Equals("ON", State())))) |
	  out(ApplyFunction(Size(Maps.FilterMap(Equals(Y, true)))))
}}
Connector.connect(f, 1, g, 0)
a = ApplyFunction(new ca.uqac.lif.cep.util.Bags.ToArray(Object.class, Object.class))
Connector.connect(t, 0, a, 0)
Connector.connect(g, 0, a, 1)
w = a | Write()
w.run()