/*
    Processing of sensor events with BeepBeep
    Copyright (C) 2023-2024 Sylvain Hallé

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
*/
package sensors.casas.hh.examples;

import static ca.uqac.lif.cep.Connector.connect;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import static ca.uqac.lif.cep.Connector.BOTTOM;
import static ca.uqac.lif.cep.Connector.INPUT;
import static ca.uqac.lif.cep.Connector.OUTPUT;
import static ca.uqac.lif.cep.Connector.TOP;

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.IfThenElse;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.functions.TurnInto;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.io.ReadLines;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.Insert;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Window;
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.tuples.Tuple;
import ca.uqac.lif.cep.tuples.TupleFeeder;
import ca.uqac.lif.cep.util.Booleans;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.casas.hh.HHFormat;
import sensors.casas.hh.HHLogRepository;


/**
* Detects when a sensor produces values exceeding a specific threshold.
* Instead of detecting individual outliers, the pipeline looks for a
* <em>pattern</em> where the threshold is exceeded on <i>m</i> out of
* <i>n</i> successive events from that sensor. When this occurs, no alarm is
* triggered again until the sensor returns to normal values for at least
* <i>n</i> successive events. This avoids reporting a warning multiple times
* for the same outlier "episode".
* <p>
* The pipeline corresponding to this calculation is illustrated below:
* <p>
* <img src="{@docRoot}/doc-files/Threshold.png" alt="Pipeline" />
* 
* @author Sylvain Hallé
*
*/
public class ThresholdTxt
{
/* The adapter for the event format. */
	protected static final EventFormat format = new HHFormat();

public static void main(String[] args) throws FileSystemException, IOException
{
	/* The threshold value that must not be exceeded. */
	int k = 24;
	
	/* The width of the window (n) and the number of times an outlier must be
	 * observed to report a warning (m). */
	int m = 3, n = 5;
	
	
	/* The ID of the sensor we wish to examine. */
	Tuple sensor_id = format.createId("Ignore", "KitchenTemp", "T106", "Control4-Temperature");
	System.out.println(sensor_id);	
	
	
	/* Prepare to read from an offline log. */
	LogRepository fs = new HHLogRepository("hh115").open();
	InputStream is = fs.readFrom("casas-rawdata.txt");
	ReadLines reader = new ReadLines(is);
	PrintStream os = new PrintStream(fs.writeTo("Threshold.txt"));
	
	
	/* Filter the events of a single sensor. */
	TupleFeeder f = new TupleFeeder();
	Connector.connect(reader, f);
	Pump p = new Pump(); // a pump processor is usually created between input source and output
	connect(f, p);
	FilterOn filter = new FilterOn(new FunctionTree(Equals.instance, format.sensorId(), new Constant(sensor_id)));
	connect(p, filter);
	
	
	/* Detect excessive values according to the pattern. */
	ApplyFunction exceeds = new ApplyFunction(new FunctionTree(Numbers.isGreaterThan, new FunctionTree(Numbers.numberCast, new FetchAttribute("state")), new Constant(k)));
	
	connect(filter, exceeds);
	Window win = new Window(new GroupProcessor(1, 1) {{ /**A Processor that applies another processor on a “sliding window” of events. It takes as arguments another processor P and a window width n. */
		Fork f = new Fork(3); 
		ApplyFunction ite = new ApplyFunction(IfThenElse.instance);
		connect(f, 0, ite, 0);     //connect the output of f to the first input of ite
		TurnInto one = new TurnInto(1);     //A Processor that turns any input event into a predefined object
		connect(f, 1, one, INPUT);    //connect the second output of fork to the input of TurnInto 1
		TurnInto zero = new TurnInto(0);
		connect(f, 2, zero, INPUT);     //connect the third output of fork to the input of TurnInto 0
		connect(one, OUTPUT, ite, 1);    //connect the output of TurnInto1 to the second input of ite
		connect(zero, OUTPUT, ite, 2);    //connect the output of TurnInto0 to the third input of ite
		Cumulate sum = new Cumulate(Numbers.addition);    //an ApplyFunction processor that has a cumulative function. 
		connect(ite, sum);     //connect output of ite to input of sum
		ApplyFunction gt = new ApplyFunction(new FunctionTree(Numbers.isGreaterThan, StreamVariable.X, new Constant(m)));
		connect(sum, gt);
		addProcessors(f, ite, one, zero, sum, gt);
		associateInput(f).associateOutput(gt);
	}}, n);
	connect(exceeds, win);
	Fork f1 = new Fork();
	connect(win, f1);
	Insert ins = new Insert(1, false);
	connect(f1, TOP, ins, INPUT);     //connect the top output of f1 to the input of Insert processor
	ApplyFunction new_warning = new ApplyFunction(new FunctionTree(Booleans.and, new FunctionTree(Booleans.not, StreamVariable.X), StreamVariable.Y));
	connect(ins, OUTPUT, new_warning, TOP);
	connect(f1, BOTTOM, new_warning, BOTTOM);

	
	/* Run the pipeline and print its results in the output stream. */
	connect(new_warning, new Print.Println(os));
	p.run();

	
	/* Close the resources. */
	os.close();
	is.close();
	fs.close();
}

}
