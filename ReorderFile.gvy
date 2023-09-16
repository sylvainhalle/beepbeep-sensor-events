/* Retrieves the list of all sensors, grouped by location and by subject. */

import static nears.Shortcuts.*
import nears.*

cmd = new SpliceInputStream(args)
cmd | new OrderTimestamps() | new Print().setSeparator("\n")
cmd.start()