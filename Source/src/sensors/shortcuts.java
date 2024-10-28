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
package sensors;

import java.io.IOException;
import java.util.List;

import ca.uqac.lif.cep.CallAfterConnect;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.mtnp.plot.gnuplot.GnuPlot;
import sensors.patterns.CountIfPattern;
import sensors.patterns.CounterPattern;
import sensors.patterns.EpisodePattern;
import sensors.patterns.FilterPattern;
import sensors.patterns.IndexRangePattern;
import sensors.patterns.LocatePattern;
import sensors.patterns.PlotPattern;
import sensors.patterns.PullPrintln;
import sensors.patterns.PullWrite;
import sensors.patterns.SliceByPattern;
import sensors.patterns.SuccessivePattern;

/**
 * A set of static functions extending BeepBeep processors and functions from
 * various packages without providing any additional functionality. This class
 * has no use other than allowing one to write shorter Groovy scripts by
 * referring to these classes in a single <tt>import</tt> statement. Thus,
 * instead of writing:
 * <pre>
 * import ca.uqac.lif.cep.GroupProcessor
 * import ca.uqac.lif.functions.ApplyFunction
 * import ca.uqac.lif.tmf.Slice
 * </pre>
 * one can instead write
 * <pre>
 * import static sensors.shortcuts.*
 * </pre>
 * and get access to all these classes on a "first-name basis".
 * <p>
 * The use of these class names is totally optional and normal BeepBeep class
 * names may be used anywhere instead.
 * 
 * @author Sylvain Hallé
 */
public class shortcuts extends beepbeep.groovy
{
	/**
	 * The symbolic variable designating the first input stream or argument of
	 * a function.
	 */
	public static ca.uqac.lif.cep.functions.StreamVariable X = ca.uqac.lif.cep.functions.StreamVariable.X;

	/**
	 * The symbolic variable designating the second input stream or argument of
	 * a function.
	 */
	public static ca.uqac.lif.cep.functions.StreamVariable Y = ca.uqac.lif.cep.functions.StreamVariable.Y;

	/**
	 * The symbolic variable designating the third input stream or argument of
	 * a function.
	 */
	public static ca.uqac.lif.cep.functions.StreamVariable Z = ca.uqac.lif.cep.functions.StreamVariable.Z;

	/**
	 * Unreachable constructor.
	 */
	protected shortcuts()
	{
		super();
	}

	/* --- I/O --- */
	
	/**
	 * Creates a processor that read events from a file in a given format. The
	 * resulting processor will either read from the standard input if the
	 * filename is "-", or from a file with the given name.
	 * @param format The format of the events
	 * @param args The command-line arguments
	 * @return The processor
	 * @throws IOException If an error occurs while reading the file or the
	 * standard input
	 */
	protected static Processor createSource(EventFormat format, String[] args) throws IOException
	{
		boolean show_progress = false;
		String filename = "-";
		for (int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			if (arg.compareTo("--show-progress") == 0 || arg.compareTo("-p") == 0)
			{
				show_progress = true;
			}
			else
			{
				filename = args[i];
			}
		}
		if (filename.compareTo("-") == 0)
		{
			return format.getFeeder(System.in);
		}
		return format.getFeeder(show_progress ? System.err : null, filename);
	}

	public static SpliceSource readJsonStreamFrom(String ... filenames)
	{
		return new SpliceSource.SpliceJsonStreamSource(false, filenames);
	}

	public static SpliceSource readJsonFrom(String ... filenames)
	{
		return new SpliceSource.SpliceJsonSource(false, filenames);
	}

	public static class JPathFunction extends ca.uqac.lif.cep.json.JPathFunction
	{
		public JPathFunction(String path)
		{
			super(path);
		}
	}

	public static StartAfterConnect Write()
	{
		return new StartAfterConnect(new PullPrintln());
	}

	public static StartAfterConnect WriteBinary()
	{
		return new StartAfterConnect(new PullWrite());
	}
	
	public static PrettyPrint PrettyPrint()
	{
		return new PrettyPrint();
	}
	
	/**
	 * A {@link CallAfterConnect} object that starts a processor after it has
	 * been connected to its input. In a Groovy script, this object spares the
	 * user from manually calling the {@link Processor#start()} method after
	 * connecting the last processor in the chain, in order for it to start
	 * processing events.
	 */
	protected static class StartAfterConnect implements CallAfterConnect
	{
		/**
		 * The processor to connect.
		 */
		private final Processor m_processor;
		
		/**
		 * Creates a new instance of the call.
		 * @param p The processor to connect
		 */
		public StartAfterConnect(Processor p)
		{
			super();
			m_processor = p;
		}
		
		@Override
		public Processor getProcessor()
		{
			return m_processor;
		}

		@Override
		public void call()
		{
			m_processor.start();
		}
	}

	/* --- Processors --- */

	/**
	 * Creates a 1:1 processor chain that retains events in a stream at indices
	 * where another processor chain <i>P</i> produces the value <em>true</em>
	 * (&top;). The chain is represented graphically as:
	 * <p>
	 * <img src="{@docRoot}/doc-files/FilterPattern.png" alt="Pattern" />
	 * @param p The 1:1 processor chain <i>P</i> producing the Boolean stream
	 * used as the filtering criterion
	 * @return The processor chain
	 * @see FilterPattern
	 */
	public static Processor Filter(Processor p)
	{
		return new FilterPattern(p);
	}

	/**
	 * Creates a 1:1 processor chain that retains events in a stream at indices
	 * where another processor chain <i>P</i> produces the value <em>true</em>
	 * (&top;). The chain is represented graphically as:
	 * <p>
	 * <img src="{@docRoot}/doc-files/FilterPattern.png" alt="Pattern" />
	 * @param p The 1:1 function to be evaluated by <i>P</i> in order to produce
	 * the Boolean stream used as the filtering criterion
	 * @return The processor chain
	 * @see FilterPattern
	 */
	public static Processor Filter(Function f)
	{
		return Filter(new ca.uqac.lif.cep.functions.ApplyFunction(f));
	}

	/**
	 * Creates a processor chain that evaluates a function on events that mark the
	 * start and the end of an "episode". The chain is represented graphically as:
	 * <p>
	 * <img src="{@docRoot}/doc-files/EpisodePattern.png" alt="Pattern" />
	 * @param f_s The function to evaluate on the start of the episode
	 * @param f_e The function to evaluate on the end of the episode
	 * @param f_delta The function to evaluate on the two events identified as
	 * the start and the end of the episode
	 * @return The processor chain
	 * @see EpisodePattern
	 */
	public static Processor Episode(Function f_s, Function f_e, Function f_delta)
	{
		return new EpisodePattern(f_s, f_e, f_delta);
	}

	/**
	 * Creates a processor chain that outputs the indices in a stream where
	 * another processor chain <i>P</i> processing that stream produces the value
	 * <em>true</em> (&top;). The chain is represented graphically as:
	 * <p>
	 * <img src="{@docRoot}/doc-files/LocatePattern.png" alt="Pattern" />
	 * @param p The 1:1 processor chain <i>P</i> producing the Boolean stream
	 * @return The processor chain
	 * @see LocatePattern
	 */
	public static Processor Locate(Processor p)
	{
		return new LocatePattern(p);
	}

	/**
	 * Creates a processor chain that counts the number of times that a processor <i>P</i> produces the value
	 * <em>true</em> (&top;) when ingesting a given input stream.
	 * @param p The 1:1 processor chain <i>P</i> producing the Boolean stream
	 * @return The processor chain
	 * @see CountIfPattern
	 */
	public static Processor CountIf(Processor p)
	{
		return new CountIfPattern(p);
	}

	/**
	 * Creates a pattern that generates a plot from a set of streams processed by a set of processors.
	 * The first processor generates the x-axis of the plot, while the other
	 * processors generate the values of the y-axis. The plot is then output as
	 * a GnuPlot document. Graphically, this pattern can be represented as follows:
	 * <p>
	 * <img src="{@docRoot}/doc-files/PlotPattern.png" alt="Processor graph">
	 * @see PlotPattern
	 */
	public static Processor Plot(List<String> names, GnuPlot pi, Processor x, Processor... ys)
	{
		String[] a_names = new String[names.size()];
		names.toArray(a_names);
		return new PlotPattern(a_names, pi, x, ys);
	}
	
	public static Processor IndexRange(int start, int end)
	{
		return new IndexRangePattern(start, end);
	}

	/**
	 * Creates a processor chain that evaluates a function on every pair of
	 * successive events. The chain is represented graphically as:
	 * <p>
	 * <img src="{@docRoot}/doc-files/SuccessivePattern.png" alt="Pattern" />
	 * @param f The 2:1 function to evaluate 
	 * @return The processor chain
	 * @see SuccessivePattern
	 */
	public static Processor Successive(Function f)
	{
		return new SuccessivePattern(f);
	}

	/**
	 * Creates a processor that simply emits an increasing sequence of numbers, starting at
	 * 1, upon every input event. Graphically, this pattern can be represented as
	 * follows:
	 * <p>
	 * <img src="{@docRoot}/doc-files/CounterPattern.png" alt="Processor graph">
	 * @return The processor chain
	 * @see CounterPattern
	 */
	public static Processor Counter()
	{
		return new CounterPattern();
	}
	
	public static Processor SliceBy(Object f, Object p)
	{
		return new SliceByPattern(liftFunction(f), liftProcessor(p));
	}
	
	public static Processor RemoveStutterTail()
	{
		return new RemoveStutterTail();
	}
	
	public static Processor RemoveStutterHead()
	{
		return new RemoveStutterHead();
	}

	/* --- Functions --- */

	/**
	 * Creates a new instance of the {@link BoxAndWhiskers} function.
	 * @return The function
	 * @see sensors.BoxAndWhiskers
	 */
	public static Function BoxAndWhiskers()
	{
		return new sensors.BoxAndWhiskers();
	}

	/**
	 * Creates a function that flattens a collection of collections into a single
	 * collection.
	 * @see Flatten
	 * @return The function instance
	 */
	public static Function Flatten()
	{
		return Flatten.instance;
	}

	/**
	 * Creates a function that flattens a specific collection of collections into
	 * a single collection. 
	 * @param o The collection
	 * @return The function instance
	 * @see Flatten
	 */
	public static Function Flatten(Object o)
	{
		return new FunctionTree(Flatten.instance, liftFunction(o));
	}

	/**
	 * Creates a function that takes a map and outputs the collection of its
	 * values.
	 * @return The function instance
	 * @see Maps.Values
	 */
	public static Function Values()
	{
		return Maps.values;
	}

	/**
	 * Creates a function that takes a constant map and outputs the collection
	 * of its values.
	 * @param o The map
	 * @return The function instance
	 * @see Maps.Values
	 */
	public static Function Values(Object o)
	{
		return new FunctionTree(Maps.values, liftFunction(o));
	}
	
	/**
	 * Creates a function that transforms a number of minutes into a number of
	 * milliseconds.
	 * @param o The argument to the function
	 * @return The function instance
	 * @see Timestamps.Minutes
	 */
	public static Function Minutes(Object o)
	{
		return new Timestamps.Minutes(liftFunction(o));
	}
	
	/**
	 * Creates a function that transforms a number of hours into a number of
	 * milliseconds.
	 * @param o The argument to the function
	 * @return The function instance
	 * @see Timestamps.Hours
	 */
	public static Function Hours(Object o)
	{
		return new Timestamps.Hours(liftFunction(o));
	}
	
	/**
	 * Creates a function that extracts the year-week pair from a timestamp.
	 * @return The function instance
	 */
	public static Function GetYearWeek()
	{
		return new Timestamps.GetYearWeek();
	}
	
	public static Function GetYearWeek(Object o)
	{
		return new FunctionTree(new Timestamps.GetYearWeek(), liftFunction(o));
	}
	
	public static Function StartsWith(Object x, Object y)
	{
		return new FunctionTree(Strings.startsWith, liftFunction(x), liftFunction(y));
	}
}
