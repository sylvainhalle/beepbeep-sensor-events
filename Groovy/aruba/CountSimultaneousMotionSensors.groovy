import static sensors.casas.aruba.shortcuts.*

(
  Read(args) |
  ToTable(["t", "N"], [
    Timestamp(),
    new Group() {{
    	in(Slice(SensorId(), ApplyFunction(Equals("ON", State())))) |
    	  out(ApplyFunction(Size(Maps.FilterMap(Equals(Y, true)))))
    }}
  ]) |
  KeepLast() | new ca.uqac.lif.cep.mtnp.PrintGnuPlot(GnuplotScatterplot()) | Write() //DrawPngPlot(GnuplotScatterplot()) | WriteBinary()
).run()