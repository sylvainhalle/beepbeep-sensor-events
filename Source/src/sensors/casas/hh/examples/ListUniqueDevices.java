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

import static ca.uqac.lif.cep.Connector.connect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.util.Sets;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.PrettyPrint;
import sensors.casas.hh.HHFormat;
import sensors.casas.hh.HHLogRepository;

public class ListUniqueDevices
{
	/* The adapter for the event format. */
	protected static final EventFormat format = new HHFormat();
	
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new HHLogRepository("hh115");
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input and output file. */
		fs.open();
		InputStream is = fs.readFrom("casas-rawdata.txt");
		OutputStream os = fs.writeTo("ListUniqueDevices.txt");
		Processor feeder = format.getFeeder(is);
		
		/* Create the pipeline. */
		Pump p = new Pump();
		connect(feeder, p);
		ApplyFunction scal = new ApplyFunction(format.sensorPlacement());
		connect(p, scal);
		Sets.PutInto pi = new Sets.PutInto();
		connect(scal, pi);
		KeepLast last = new KeepLast();
		connect(pi, last);
		
		/* Connect the pipeline to an output and run. */
		ApplyFunction pp = new ApplyFunction(new PrettyPrint());
		connect(last, pp);
		connect(pp, new Print(new PrintStream(os)));
		p.run();
		
		/* Clean up. */
		os.close();
		is.close();
		fs.close();
	}

}
