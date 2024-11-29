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
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package sensors.opportunity.examples;

import static ca.uqac.lif.cep.Connector.connect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.Filter;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.Insert;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Trim;
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.tuples.MergeScalars;
import ca.uqac.lif.cep.tuples.TupleMap;
import ca.uqac.lif.cep.util.Booleans;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.NthElement;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystemException;
import sensors.LogRepository;
import sensors.opportunity.OpportunityFormat;
import sensors.opportunity.OpportunityLogRepository;

public class LabelEpisodes
{
	/* The adapter for the event format. */
	protected static OpportunityFormat format = new OpportunityFormat();
	
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new OpportunityLogRepository();
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		final String att = "ML_Both_Arms";
		final String dataset = "S4-ADL1";
		
		fs.open();
		InputStream is = fs.readFrom(dataset + ".dat");
		OutputStream os = fs.writeTo("label_episodes_" /*+ dataset*/ + "_" + att + ".txt");
		Processor feeder = format.getRawFeeder(is);
		
		Fork f = new Fork();
		connect(feeder, f);
		
		/* Check if two successive events have the same label */
		Fork f_a = new Fork();
		connect(f, 0, f_a, 0);
		Trim trim_a = new Trim(1);
		connect(f_a, 1, trim_a, 0);
		ApplyFunction same = new ApplyFunction(new FunctionTree(Booleans.not, 
				new FunctionTree(Equals.instance, 
					new FunctionTree(new FetchAttribute(att), StreamVariable.X),
					new FunctionTree(new FetchAttribute(att), StreamVariable.Y)
				)));
		connect(f_a, 0, same, 0);
		connect(trim_a, 0, same, 1);
		Trim trim_ins = new Trim(1);
		connect(same, trim_ins);
		Insert ins = new Insert(1, true);
		connect(trim_ins, ins);
		
		/* Only keep events that have a different label from the next. This
		 * marks the "boundaries" of the intervals with the same label. */
		Filter fil = new Filter();
		connect(f, 1, fil, 0);
		connect(ins, 0, fil, 1);
		
		/* Calculate the timestamp difference between successive events; the
		 * duration of each "episode" of a given label. */
		Fork f_b = new Fork(3);
		connect(fil, f_b);
		Trim trim_b = new Trim(1);
		connect(f_b, 1, trim_b, 0);
		ApplyFunction time_diff = new ApplyFunction(new FunctionTree(Numbers.subtraction,
				new FunctionTree(format.timestamp(), StreamVariable.Y),
				new FunctionTree(format.timestamp(), StreamVariable.X)
		));
		connect(f_b, 0, time_diff, 0);
		connect(trim_b, 0, time_diff, 1);
		Trim trim_act = new Trim(1);
		connect(f_b, 2, trim_act, 0);
		ApplyFunction get_label = new ApplyFunction(new FetchAttribute(att));
		connect(trim_act, 0, get_label, 0);
		ApplyFunction mts = new ApplyFunction(new MergeScalars("d", "act"));
		connect(time_diff, 0, mts, 0);
		connect(get_label, 0, mts, 1);
		FilterOn only_short = new FilterOn(new FunctionTree(Booleans.and,
				new FunctionTree(Booleans.not, 
						new FunctionTree(Equals.instance, new FetchAttribute("act"), new Constant(0))),
				new FunctionTree(Numbers.isLessOrEqual,
						new FetchAttribute("d"),
						new Constant(300))));
		connect(mts, only_short);
		
		Pump p = new Pump();
		connect(only_short, p);
		Print print = new Print.Println(new PrintStream(os));
		connect(p, print);
		
		/* Run the pipeline. */
		p.run();
		
		/* Close the resources. */
		is.close();
		os.close();
		fs.close();
	}
}
