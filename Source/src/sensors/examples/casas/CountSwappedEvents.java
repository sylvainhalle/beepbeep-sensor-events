/*
    Processing of sensor events with BeepBeep
    Copyright (C) 2023 Sylvain Hallé

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
package sensors.examples.casas;

import static ca.uqac.lif.cep.Connector.connect;

import java.io.IOException;
import java.io.InputStream;

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.IfThenElse;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.io.ReadLines;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Trim;
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.tuples.TupleFeeder;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.cep.util.Strings;
import ca.uqac.lif.fs.FileSystemException;
import sensors.LogRepository;
import sensors.casas.CasasLogRepository;
import sensors.casas.DateToTimestampCasas;


public class CountSwappedEvents
{
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new CasasLogRepository();

	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		fs.open();
		InputStream is = fs.readFrom("casas-rawdata.txt");
		ReadLines reader = new ReadLines(is);
		TupleFeeder feeder = new TupleFeeder();
		Connector.connect(reader, feeder);
		
		ApplyFunction get_ts = new ApplyFunction(new FunctionTree(DateToTimestampCasas.instance, new FunctionTree(Strings.concat, 
                new FunctionTree(Strings.concat, new FetchAttribute("date"), new Constant("T")),
                new FetchAttribute("time"))));
		connect(feeder, get_ts);
		Fork f1 = new Fork(2);
		connect(get_ts, f1);
		ApplyFunction gt = new ApplyFunction(Numbers.isLessOrEqual);
		connect(f1, 0, gt, 0);
		Trim t = new Trim(1);
		connect(f1, 1, t, 0);
		connect(t, 0, gt, 1);
		ApplyFunction ite = new ApplyFunction(new FunctionTree(IfThenElse.instance, StreamVariable.X, new Constant(0), new Constant(1)));
		connect(gt, ite);
		Cumulate sum = new Cumulate(Numbers.addition);
		connect(ite, sum);
		Pump p = new Pump();
		connect(sum, p);
		KeepLast kl = new KeepLast();
		connect(p, kl);
		Print print = new Print();
		connect(kl, print);
		
		/* Run the pipeline. */
		p.run();
		
		/* Close the resources. */
		is.close();
		fs.close();
		System.out.println();
		System.out.println("Code runs successfully.");
	}

}
