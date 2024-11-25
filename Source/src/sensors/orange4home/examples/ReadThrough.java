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
package sensors.orange4home.examples;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.FilterOn;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.orange4home.Orange4HomeFormat;
import sensors.orange4home.Orange4HomeLogRepository;

import static ca.uqac.lif.cep.Connector.connect;

/**
 * Keeps the count of how many motion sensors are simultaneously ON at any
 * moment, and displays the result in a scatterplot.
 */
public class ReadThrough
{
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new Orange4HomeLogRepository();
	
	/* The adapter for the event format. */
	protected static final EventFormat format = new Orange4HomeFormat();
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		fs.open();
		InputStream is = fs.readFrom("o4h_all_events.csv");
		OutputStream os = fs.writeTo("read.txt");
		Processor feeder = format.getFeeder(is);
		FilterOn fil = new FilterOn(new FunctionTree(
				Equals.instance, 
				format.sensorId(), 
				new Constant(format.createId("office", "tv", "", "status"))));
		connect(feeder, fil);
		Pump p = new Pump();
		connect(fil, p);
		Print print = new Print(new PrintStream(os)).setSeparator("\n");
		connect(p, print);
		
		/* Run the pipeline. */
		p.run();
		
		/* Close the resources. */
		is.close();
		os.close();
		fs.close();
	}
	
	
}
