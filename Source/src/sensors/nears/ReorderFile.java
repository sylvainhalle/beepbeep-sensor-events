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
package sensors.nears;

import static ca.uqac.lif.cep.Connector.connect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.io.Print;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.fs.FileSystemException;
import sensors.EventFormat;
import sensors.LogRepository;
import sensors.OrderTimestamps;

/**
 * From a JSON file, create a new JSON file where events are physically
 * occurring in the order defined by their timestamp. A side effect of
 * calling this program is that the resulting file also reformats each event so
 * that it is written on a single text line.
 * 
 * @author Sylvain Hallé
 */
public class ReorderFile
{
	/* The input event format. */
	protected static EventFormat format = new NearsJsonFormat();
	
	/* The folder where the data files reside. */
	protected static final LogRepository fs = new NearsLogRepository("0034");
	
	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input and output file. */
		fs.open();
		InputStream is = fs.readFrom("nears-hub-0034.json");
		OutputStream os = fs.writeTo("nears-hub-0034-sorted.json");
		
		/* Create the pipeline. */
		JsonFeeder feeder = new JsonFeeder(is);
		Pump p = new Pump();
		connect(feeder, p);
		OrderTimestamps o = new OrderTimestamps(format.timestamp());
		connect(p, o);

		/* Connect the pipeline to an output and run. */
		connect(o, new Print(new PrintStream(os)).setSeparator("\n"));
		p.run();
		
		/* Clean up. */
		os.close();
		is.close();
		fs.close();
	}

}
