/**
 * Filters a data stream to include only events produced by a sensor with
 * a given ID.
 */
import static beepbeep.groovy.*
import static sensors.shortcuts.*
import static sensors.casas.hh.shortcuts.*

Read(args) |
Filter(Equals(SensorId(), "T106")) |
Write()