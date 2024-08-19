package sensors.examples.casas;

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


import static ca.uqac.lif.cep.Connector.connect;

import java.io.IOException;
import java.io.InputStream;
import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.io.ReadLines;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Trim;
import ca.uqac.lif.cep.tuples.TupleFeeder;
import ca.uqac.lif.cep.util.Booleans;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.casas.CasasLogRepository;
import sensors.casas.CasasTxtFormat;


/**
* Determines if a log is such that all its events are ordered by timestamp.
* The pipeline outputs a single Boolean value depending on whether this
* condition is satisfied or not for a given log.
*/
public class MonotonicTimestamps
{
/* The adapter for the event format. */
protected static EventFormat format = new CasasTxtFormat();

/* The folder where the data files reside. */
protected static final LogRepository fs = new CasasLogRepository();

public static void main(String[] args) throws FileSystemException, IOException
{
	fs.open();
	InputStream is = fs.readFrom("casas-rawdata.txt");
	ReadLines reader = new ReadLines(is);
	
	TupleFeeder feeder = new TupleFeeder();
	Connector.connect(reader, feeder);
	ApplyFunction get_ts = new ApplyFunction(format.timestamp());
	connect(feeder, get_ts);
	Fork f1 = new Fork(2);
	connect(get_ts, f1);
	ApplyFunction gt = new ApplyFunction(Numbers.isGreaterOrEqual);
	connect(f1, 0, gt, 0);
	Trim t = new Trim(1);
	connect(f1, 1, t, 0);
	connect(t, 0, gt, 1);
	Cumulate all = new Cumulate(Booleans.and);
	connect(gt, all);
	Pump p = new Pump();
	connect(all, p);
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
	System.out.print("Code runs successfully.");

}

}