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
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.tuples.MergeScalars;
import ca.uqac.lif.fs.FileSystemException;
import sensors.LogRepository;
import sensors.opportunity.OpportunityFormat;
import sensors.opportunity.OpportunityLogRepository;

/**
 * Extracts the accelerometric data of a specific sensor.
 */
public class DrawerLifecycle
{
	/* The adapter for the event format. */
	protected static OpportunityFormat format = new OpportunityFormat();
	
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new OpportunityLogRepository();
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		String dataset_nb = "2"; // 1, 2, 3, or 4
		String sensor_name = "LOWERDRAWER";
		
		fs.open();
		InputStream is = fs.readFrom("S" + dataset_nb +"-ADL1.dat");
		OutputStream os = fs.writeTo("proj_" + dataset_nb + "_" + sensor_name + ".csv");
		Processor feeder = format.getRawFeeder(is);

		Fork f1 = new Fork(4);
		connect(feeder, f1);
		ApplyFunction x = new ApplyFunction(new FetchAttribute("REED SWITCH LOWERDRAWER S3"));
		connect(f1, 0, x, 0);
		ApplyFunction y = new ApplyFunction(new FetchAttribute("REED SWITCH LOWERDRAWER S2"));
		connect(f1, 1, y, 0);
		ApplyFunction z = new ApplyFunction(new FetchAttribute("REED SWITCH LOWERDRAWER S1"));
		connect(f1, 2, z, 0);
		ApplyFunction t = new ApplyFunction(format.timestamp());
		connect(f1, 3, t, 0);
		
		ApplyFunction merge = new ApplyFunction(new MergeScalars("x", "y", "z", "t"));
		connect(x, 0, merge, 0);
		connect(y, 0, merge, 1);
		connect(z, 0, merge, 2);
		connect(t, 0, merge, 3);
		Pump p = new Pump();
		connect(merge, p);
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
