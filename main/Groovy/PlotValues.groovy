/**
 * Produces a scatterplot out of the (numerical) values produced by a
 * single sensor.
 */
import static beepbeep.groovy.*
import static sensors.shortcuts.*
import static sensors.casas.hh.shortcuts.*
import ca.uqac.lif.mtnp.plot.gnuplot.Scatterplot

// Sensor to look for
SENSOR = "LS003"

Read(args) |
Filter(Equals(SensorId(), SENSOR)) |
IndexRange(0, 1000) |
Plot(["t", "v"], new Scatterplot(), ApplyFunction(Timestamp()), ApplyFunction(State())) |
KeepLast() |
Write()