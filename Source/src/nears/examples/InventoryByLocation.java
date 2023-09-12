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
package nears.examples;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.json.JPathFunction;
import ca.uqac.lif.cep.tmf.KeepLast;
import ca.uqac.lif.cep.tmf.Pump;
import ca.uqac.lif.cep.tmf.Slice;
import ca.uqac.lif.cep.util.Sets;
import ca.uqac.lif.fs.FileSystem;
import ca.uqac.lif.fs.FileSystemException;
import ca.uqac.lif.fs.HardDisk;
import nears.JsonLineFeeder;
import nears.PrettyPrint;

import static ca.uqac.lif.cep.Connector.connect;

/**
 * Retrieves the list of all sensors, grouped by location and by subject.
 * <p>
 * The pipeline corresponding to this calculation is illustrated below:
 * <p>
 * <img src="{@docRoot}/doc-files/InventoryByLocation.png" alt="Pipeline" />
 * <p>
 * The result is a map from a location to another map from a subject to a
 * set of sensors. A possible (stylized) output of this pipeline is:
 * <p>
 * <img src="{@docRoot}/doc-files/InventoryByLocation_output.png" alt="Pipeline" />
 * 
 * @author Sylvain Hallé
 */
public class InventoryByLocation
{
	public static void main(String[] args) throws FileSystemException, IOException
	{
		/* Define the input and output file. */
		FileSystem fs = new HardDisk("/home/sylvain/domus-capteurs").open();
		InputStream is = fs.readFrom("nears-hub-0032-sorted.json");
		OutputStream os = fs.writeTo("ListSensorsByLocation.txt");
		
		/* Create the pipeline. */
		Pump p = (Pump) connect(new JsonLineFeeder(is),
				new Slice(new JPathFunction("location"),
						new Slice(new JPathFunction("subject"),
								new GroupProcessor(1, 1) {{
									ApplyFunction f = new ApplyFunction(new JPathFunction("model"));
									Processor p = connect(f, new Sets.PutInto());
									addProcessors(f, p).associateInput(f).associateOutput(p);
								}})),
				new KeepLast(),
				new Pump());
		connect(p, new PrettyPrint(new PrintStream(os)));
		
		/* Run the pipeline. */
		p.run();
		
		/* Close the resources. */
		os.close();
		is.close();
		fs.close();
	}
}
