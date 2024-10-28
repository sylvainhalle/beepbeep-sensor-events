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
package sensors.casas.hh;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.StreamVariable;
import ca.uqac.lif.cep.io.ReadLines;
import ca.uqac.lif.cep.tuples.FetchAttribute;
import ca.uqac.lif.cep.tuples.FixedTupleBuilder;
import ca.uqac.lif.cep.tuples.MergeScalars;
import ca.uqac.lif.cep.tuples.Tuple;
import ca.uqac.lif.cep.util.Strings;
import sensors.IndexTupleFeeder;
import sensors.ReadLinesStatus;
import sensors.casas.CasasTxtFormat;
import sensors.casas.DateToTimestampCasas;

/**
 * Format specific to the CASAS datasets of type "HH" (e.g.<!-- hh115 -->).
 */
public class HHFormat extends CasasTxtFormat
{
	/**
	 * Name of the attribute in tuples holding both the date and time.
	 */
	/* @ non_null @ */ public static final String TXT_DATETIME = "datetime";

	/**
	 * The builder creating objects identifying a sensor's uniquely defined
	 * location.
	 */
	protected static final FixedTupleBuilder s_placementBuilder = new FixedTupleBuilder("location",	"subject", "model");

	/**
	 * The builder creating objects identifying a sensor's unique ID.
	 */
	protected static final FixedTupleBuilder s_idBuilder = new FixedTupleBuilder("location", "subject", "model", "sensor");

	protected static final FixedTupleBuilder s_globalTupleBuilder = new FixedTupleBuilder(TXT_DATETIME,	TXT_SENSOR, TXT_LOCATION, TXT_SUBJECT, TXT_STATE, TXT_ACTIVITY);

	/**
	 * In the HH dataset, the first field in an event corresponds to the
	 * date in the form "yyyy-MM-dd HH:mm:ss.SSSSSX". The strings is converted
	 * into a Unix timestamp (i.e. a {@code long}).
	 */
	@Override
	public Function timestamp()
	{
		return new FunctionTree(DateToTimestampCasas.instance, new FetchAttribute(TXT_DATETIME));
	}


	/**
	 * In the HH dataset, the placement of a sensor can be uniquely determined
	 * by the combination of three attributes in an event: {@code location},
	 * {@code subject} and {@code model}.
	 */
	@Override
	public Function sensorPlacement()
	{
		return new FunctionTree(new MergeScalars("location", "subject", "model"),
				new FetchAttribute(TXT_LOCATION), new FetchAttribute(TXT_SUBJECT),
				modelString());
	}

	/**
	 * In the HH dataset, the placement of a sensor is uniquely determined by the
	 * combination of three attributes in an event: {@code location},
	 * {@code subject} and {@code model}.
	 */
	@Override
	public Tuple createPlacement(String location, String subject, String model)
	{
		return s_placementBuilder.createTuple(location, subject, model);
	}

	/**
	 * In the HH dataset, the placement of a sensor is uniquely determined by the
	 * combination of three attributes in an event: {@code location},
	 * {@code subject} and {@code model}.
	 */
	@Override
	public Function sensorId()
	{
		/*return new FunctionTree(new MergeScalars("location", "subject", "model", "sensor"),
				new FetchAttribute(TXT_LOCATION), new FetchAttribute(TXT_SUBJECT),
				modelString(), new FetchAttribute(TXT_SENSOR));
				*/
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
		return s_idBuilder.createTuple(location, subject, model, sensor);
	}

	@Override
	public GroupProcessor getFeeder(InputStream is)
	{
		GroupProcessor g = new GroupProcessor(0, 1);
		{
			ReadLines r = new ReadLines(is);
			IndexTupleFeeder f = new IndexTupleFeeder(TXT_DATETIME,	TXT_SENSOR, TXT_LOCATION, TXT_SUBJECT, TXT_STATE, TXT_MODEL, TXT_ACTIVITY).setSeparator("\t");
			Connector.connect(r, f);
			g.associateOutput(0, f, 0);
		}
		return g;
	}

	@Override
	public GroupProcessor getFeeder(PrintStream os, String ... filenames) throws IOException
	{
		GroupProcessor g = new GroupProcessor(0, 1);
		{
			ReadLinesStatus r = new ReadLinesStatus(os, filenames);
			IndexTupleFeeder f = new IndexTupleFeeder(TXT_DATETIME,	TXT_SENSOR, TXT_LOCATION, TXT_SUBJECT, TXT_STATE, TXT_MODEL, TXT_ACTIVITY).setSeparator("\t");
			Connector.connect(r, f);
			g.associateOutput(0, f, 0);
		}
		return g;
	}
	
	/**
	 * In this dataset, a temperature event is spotted by the sensor's ID
	 * starting with "T".
	 */
	@Override
	public Function isTemperature()
	{
		return new FunctionTree(Strings.startsWith, new FunctionTree(stateString(), StreamVariable.X), new Constant("T"));
	}
}
