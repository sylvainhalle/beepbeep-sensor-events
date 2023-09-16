/**
 * Retrieves the list of all sensors, grouped by location and by subject.
 * Usage: groovy InventoryByLocation.gvy filename.json
 * where filename.json is the path to a JSONStream file with sensor events
 * (see ReorderFile.gvy).
 * 
 * NOTE: the BeepBeep sensor toolbox must be in the classpath (see the Readme).
 */
import static nears.Shortcuts.*
import nears.*

cmd = readJsonStreamFrom(args)
cmd | new Slice(new JPathFunction("location"),
  new Slice(new JPathFunction("subject"),
    new GroupProcessor(1, 1) {{
	  f = new ApplyFunction(new JPathFunction("model"))
	  p = f | new Sets.PutInto()
      addProcessors(f, p).associateInput(f).associateOutput(p) 
    }})) | new KeepLast() | new Print()
cmd.start()