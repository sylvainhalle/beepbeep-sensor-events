/*
    Processing of sensor events with BeepBeep
    Copyright (C) 2023-2024 Sylvain Hallé, Rania Taleb

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
package sensors.casas.hh.examples;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.util.Sets;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.PrettyPrint;
import sensors.casas.hh.HHFormat;
import sensors.casas.hh.HHLogRepository;

import static ca.uqac.lif.cep.Connector.connect;

public class ListSensors
{
	/* The adapter for the event format. */
	protected static final EventFormat format = new HHFormat();
	protected static final LogRepository fs = new HHLogRepository("hh115");
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input and output file. */
		fs.open();
		InputStream is = fs.readFrom("casas-rawdata.txt");
		OutputStream os = fs.writeTo("ListSensors.txt");
		Processor feeder = format.getFeeder(is);
		
		/* Create the pipeline. */
		Pump p = (Pump) connect(feeder,
				new Slice(format.modelString(), 
						new GroupProcessor(1, 1) {{
							ApplyFunction f = new ApplyFunction(format.sensorString());
							Processor p = connect(f, new Sets.PutInto());
							addProcessors(f, p).associateInput(f).associateOutput(p);
						}}),
				new KeepLast(),
				new Pump());
		ApplyFunction pp = new ApplyFunction(new PrettyPrint());
		connect(p, pp);
		connect(pp, new Print(new PrintStream(os)));
		
		/* Run the pipeline. */
		p.run();
		
		/* Close the resources. */
		os.close();
		fs.close();
		System.out.print("Code runs successfully.");
	}
}
