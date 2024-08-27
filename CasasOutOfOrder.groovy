import static sensors.casas.shortcuts.*

(
 Read(args) |
 Count(Successive(LessThan(Timestamp(Y), Timestamp(X)))) | KeepLast() |
 Write()
).run()