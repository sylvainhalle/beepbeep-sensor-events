import static sensors.casas.hh.shortcuts.*

(
 Read(args) |
 Count(Successive(LessThan(Timestamp(Y), Timestamp(X)))) | KeepLast() |
 Write()
).run()