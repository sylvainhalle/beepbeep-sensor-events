/**
 * Converts a JSON file exported by the NEARS platform to another JSON file
 * in the JSONStream format (i.e. a JSON file with each event on a single line).
 * In addition the script reorders the events by increasing timestamp value.
 * Usage: groovy ReorderFile.gvy filename.json > new_filename.json
 * where filename.json is the path to a NEARS JSON file and new_filename.json
 * is the new reformatted file.
 *
 * NOTE: the BeepBeep sensor toolbox must be in the classpath (see the Readme).
 */
import static nears.Shortcuts.*
import nears.*

cmd = readJsonFrom(args)
cmd | new OrderTimestamps() | new Print().setSeparator("\n")
cmd.start()