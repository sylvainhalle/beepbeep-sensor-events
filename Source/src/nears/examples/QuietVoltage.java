package nears.examples;

import static ca.uqac.lif.cep.Connector.BOTTOM;
import static ca.uqac.lif.cep.Connector.INPUT;
import static ca.uqac.lif.cep.Connector.OUTPUT;
import static ca.uqac.lif.cep.Connector.TOP;
import static ca.uqac.lif.cep.Connector.connect;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.json.JPathFunction;
import ca.uqac.lif.cep.json.NumberValue;
import ca.uqac.lif.cep.json.StringValue;
import ca.uqac.lif.cep.tmf.Filter;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tuples.MergeScalars;
import ca.uqac.lif.cep.util.Booleans.And;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystem;
import ca.uqac.lif.fs.FileSystemException;
import ca.uqac.lif.json.JsonString;
import nears.DateToTimestamp;
import nears.LogRepository;
import nears.MultiDaySource;
import nears.QuietenDown;

public class QuietVoltage
{

	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the range of days to process. */
		int first_day = 30, last_day = 30;
		
		/* Define the input and output file. */
		FileSystem fs = new LogRepository().open();
		OutputStream os = fs.writeTo("LoudVoltage.txt");
		fs.chdir("0032");
		
		/* Create the pipeline. */
		MultiDaySource feeder = new MultiDaySource(fs, first_day, last_day);
		Pump p = new Pump();
		connect(feeder, p);
		Fork f0 = new Fork();
		connect(p, f0);
		ApplyFunction is_motion = new ApplyFunction(new FunctionTree(And.instance,
				new FunctionTree(Equals.instance,
						new JPathFunction("location"),
						new Constant(new JsonString("living"))),
				new FunctionTree(Equals.instance,
						new JPathFunction("sensor"),
						new Constant(new JsonString("instant_power")))));
		connect(f0, BOTTOM, is_motion, INPUT);
		Filter f_is_voltage = new Filter();
		connect(is_motion, OUTPUT, f_is_voltage, BOTTOM);
		connect(f0, TOP, f_is_voltage, TOP);
		Fork f1 = new Fork();
		connect(f_is_voltage, f1);
		Filter pass = new Filter();
		connect(f1, TOP, pass, TOP);
		ApplyFunction get_value = new ApplyFunction(new FunctionTree(NumberValue.instance, new JPathFunction("state")));
		connect(f1, BOTTOM, get_value, INPUT);
		QuietenDown qd = new QuietenDown(new FunctionTree(Numbers.isGreaterThan,
				new FunctionTree(Numbers.absoluteValue,
						new FunctionTree(Numbers.subtraction, StreamVariable.X, StreamVariable.Y)),
				new Constant(0f)
				));
		connect(get_value, qd);
		connect(qd, OUTPUT, pass, BOTTOM);
		ApplyFunction tuple = new ApplyFunction(new FunctionTree(
				new MergeScalars("t", "V"),
					new FunctionTree(DateToTimestamp.instance, new FunctionTree(StringValue.instance, new JPathFunction("sentAt/$date"))),
					new FunctionTree(NumberValue.instance, new JPathFunction("state"))
				));
		connect(pass, tuple);
		
		/* Connect the pipeline to an output and run. */
		connect(tuple, new Print(new PrintStream(os)).setSeparator("\n"));
		p.run();
		
		/* Clean up. */
		feeder.stop();
		os.close();
		fs.close();
	}

}
