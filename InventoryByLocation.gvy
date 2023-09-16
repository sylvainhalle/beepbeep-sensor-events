/* Retrieves the list of all sensors, grouped by location and by subject. */

import static nears.Shortcuts.*
import nears.*

cmd = new SpliceInputStream(args)
cmd | new Slice(new JPathFunction("location"),
  new Slice(new JPathFunction("subject"),
    new GroupProcessor(1, 1) {{
	  f = new ApplyFunction(new JPathFunction("model"))
	  p = f | new Sets.PutInto()
      addProcessors(f, p).associateInput(f).associateOutput(p) 
    }})) | new KeepLast() | new Print()
cmd.start()