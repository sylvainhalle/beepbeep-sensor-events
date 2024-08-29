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
package sensors.casas.aruba;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.UnaryFunction;
import ca.uqac.lif.cep.io.ReadLines;
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.tuples.FixedTupleBuilder;
import ca.uqac.lif.cep.tuples.Tuple;
import ca.uqac.lif.cep.util.Strings;
import sensors.ReadLinesStatus;
import sensors.casas.CasasTxtFormat;
import sensors.casas.DateToTimestampCasas;
import sensors.casas.IndexTupleFeeder;

/**
 * Format specific to the CASAS datasets of type "Aruba".
 */
public class ArubaFormat extends CasasTxtFormat
{
	/**
	 * Name of the attribute in tuples holding the date.
	 */
	/* @ non_null @ */ public static final String TXT_DATE = "date";
	
	/**
	 * Name of the attribute in tuples holding the time.
	 */
	/* @ non_null @ */ public static final String TXT_TIME = "time";
	
	/**
	 * The builder creating objects identifying a sensor's uniquely defined
	 * location.
	 */
	protected static final FixedTupleBuilder s_placementBuilder = new FixedTupleBuilder("location",	"subject", "model");
	
	/**
	 * The builder creating objects identifying a sensor's unique ID.
	 */
	protected static final FixedTupleBuilder s_idBuilder = new FixedTupleBuilder("sensor");
	
	/**
	 * In the HH dataset, the first two fields field of and event corresponds to
	 * the date and time. They are concatenated and then converted into a Unix
	 * timestamp (i.e. a {@code long}).
	 */
	@Override
	public Function timestamp()
	{
		return new FunctionTree(DateToTimestampCasas.instance,
				new FunctionTree(Strings.concat, new FetchAttribute(TXT_DATE),
						new FunctionTree(Strings.concat, new Constant(" "), new FetchAttribute(TXT_TIME))));
	}
	
	/**
	 * In the Aruba dataset, the placement of a sensor must be inferred from the
	 * sensor's unique name and the diagram provided with the dataset. The
	 * placement itself is not present in the log.
	 */
	@Override
	public Function sensorPlacement()
	{
		// TODO, eventually, deduce placement
		throw new UnsupportedOperationException("Placement not implemented in Aruba");
	}

	/**
	 * In the Aruba dataset, the placement of a sensor must be inferred from the
	 * sensor's unique name and the diagram provided with the dataset. The
	 * placement itself is not present in the log.
	 */
	@Override
	public Tuple createPlacement(String location, String subject, String model)
	{
		// TODO, eventually, deduce placement
		throw new UnsupportedOperationException("Placement not implemented in Aruba");
	}

	/**
	 * In the Aruba dataset, the ID of a sensor is uniquely determined by the
	 * the {@code sensor} field.
	 */
	@Override
	public Function sensorId()
	{
		return new FetchAttribute(TXT_SENSOR);
	}

	/**
	 * In the HH dataset, the placement of a sensor is uniquely determined by the
	 * combination of three attributes in an event: {@code location},
	 * {@code subject} and {@code model}.
	 */
	@Override
	public Tuple createId(String location, String subject, String model, String sensor)
	{
		return s_idBuilder.createTuple(sensor);
	}
	
	@Override
	public GroupProcessor getFeeder(InputStream is)
	{
		GroupProcessor g = new GroupProcessor(0, 1);
		{
			ReadLines r = new ReadLines(is);
			IndexTupleFeeder f = new IndexTupleFeeder(TXT_DATE, TXT_TIME,	TXT_SENSOR, TXT_STATE).setSeparator("\\s");
			Connector.connect(r, f);
			g.associateOutput(0, f, 0);
		}
		return g;
	}
	
	public GroupProcessor getFeeder(String filename, PrintStream os) throws IOException
	{
		InputStream is = new FileInputStream(filename);
		GroupProcessor g = new GroupProcessor(0, 1);
		{
			ReadLines r = os == null ? new ReadLines(is) : new ReadLinesStatus(filename, os);
			IndexTupleFeeder f = new IndexTupleFeeder(TXT_DATE,	TXT_TIME, TXT_SENSOR, TXT_STATE).setSeparator("\\s");
			Connector.connect(r, f);
			g.associateOutput(0, f, 0);
		}
		return g;
	}
	
	protected static class PlacementFunction extends UnaryFunction<Tuple,String>
	{
		/**
		 * A single publicly visible instance of the function.
		 */
		public static PlacementFunction instance = new PlacementFunction();
		
		/**
		 * Creates an instance of the function.
		 */
		protected PlacementFunction()
		{
			super(Tuple.class, String.class);
		}

		@Override
		public String getValue(Tuple t)
		{
			// TODO
			return null;
		}
	}
}
