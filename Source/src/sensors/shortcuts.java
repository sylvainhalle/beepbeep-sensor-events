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

import java.util.ArrayList;

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.TurnInto;
import ca.uqac.lif.cep.tmf.Fork;

/**
 * A set of "dummy" classes extending BeepBeep processors and functions from
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

	/**
	 * Creates a 1:1 processor chain that retains events in a stream at indices
	 * where another processor chain <i>P</i> produces the value <em>true</em>
	 * (&top;). The chain is represented graphically as:
	 * <p>
	 * <img src="{@docRoot}/doc-files/FilterPattern.png" alt="Pattern" />
	 * @param p The 1:1 processor chain <i>P</i> producing the Boolean stream
	 * used as the filtering criterion
	 * @return The processor chain
	 */
	public static Processor Filter(Processor p)
	{
		return new FilterPattern(p);
	}

	public static Processor Filter(Function f)
	{
		return Filter(new ca.uqac.lif.cep.functions.ApplyFunction(f));
	}

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
	 */
	public static Processor Locate(Processor p)
	{
		return new LocatePattern(p);
	}

	public static Processor Count(Processor p)
	{
		return new CountPattern(p);
	}

	/**
	 * Creates a processor chain that evaluates a function on every pair of
	 * successive events. The chain is represented graphically as:
	 * <p>
	 * <img src="{@docRoot}/doc-files/SuccessivePattern.png" alt="Pattern" />
	 * @param f The 2:1 function to evaluate 
	 * @return The processor chain
	 */
	public static Processor Successive(Function f)
	{
		return new SuccessivePattern(f);
	}

	/**
	 * Creates a function that transforms a number of minutes into a number of
	 * milliseconds.
	 * @param x The number of minutes
	 * @return The number of milliseconds
	 */
	public static Function Minutes(Object x)
	{
		return new FunctionTree(Numbers.multiplication, liftFunction(x), liftFunction(60000l));
	}

	/**
	 * Creates a function that transforms a number of hours into a number of
	 * milliseconds.
	 * @param x The number of hours
	 * @return The number of milliseconds
	 */
	public static Function Hours(Object x)
	{
		return new FunctionTree(Numbers.multiplication, liftFunction(x), liftFunction(60l * 60000l));
	}

	public static Function Flatten()
	{
		return Flatten.instance;
	}

	public static Function Flatten(Object o)
	{
		return new FunctionTree(Flatten.instance, liftFunction(o));
	}

	public static Processor ToTable(String[] names, Object[] procs)
	{
		GroupProcessor g = new GroupProcessor(1, 1);
		Fork f = new Fork(procs.length);
		ca.uqac.lif.cep.mtnp.UpdateTableStream uts = new ca.uqac.lif.cep.mtnp.UpdateTableStream(names);
		for (int i = 0; i < procs.length; i++)
		{
			Processor p = liftProcessor(procs[i]);
			Connector.connect(f, i, p, 0);
			Connector.connect(p, 0, uts, i);
			g.addProcessor(p);
		}
		g.addProcessors(f, uts);
		g.associateInput(f);
		g.associateOutput(uts);
		return g;
	}

	//@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Processor ToTable(ArrayList<?> names, ArrayList<?> procs)
	{
		String[] a_names = new String[names.size()];
		for (int i = 0; i < a_names.length; i++)
		{
			a_names[i] = names.get(i).toString();
		}
		Object[] a_procs = new Object[procs.size()];
		procs.toArray(a_procs);
		return ToTable(a_names, a_procs);
	}

	public static Processor Counter()
	{
		GroupProcessor g = new GroupProcessor(1, 1);
		TurnInto one = new TurnInto(1);
		Cumulate sum = new Cumulate(Numbers.addition);
		Connector.connect(one, sum);
		g.addProcessors(one, sum);
		g.associateInput(one);
		g.associateOutput(sum);
		return g;
	}

	public static Function Values()
	{
		return Maps.values;
	}

	public static Function Values(Object o)
	{
		return new FunctionTree(Maps.values, liftFunction(o));
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

	public static PullPrintln Write()
	{
		return new PullPrintln();
	}

	public static PullWrite WriteBinary()
	{
		return new PullWrite();
	}

	protected static class PullWrite extends ca.uqac.lif.cep.GroupProcessor 
	{
		private final ca.uqac.lif.cep.tmf.Pump m_pump;

		public PullWrite()
		{
			super(1, 0);
			m_pump = new ca.uqac.lif.cep.tmf.Pump();
			ca.uqac.lif.cep.io.WriteOutputStream pr = new ca.uqac.lif.cep.io.WriteOutputStream(System.out);
			Connector.connect(m_pump, pr);
			associateInput(m_pump);
		}

		public void run()
		{
			m_pump.run();
		}
	}

	protected static class PullPrintln extends ca.uqac.lif.cep.GroupProcessor
	{
		private final ca.uqac.lif.cep.tmf.Pump m_pump;

		public PullPrintln()
		{
			super(1, 0);
			m_pump = new ca.uqac.lif.cep.tmf.Pump();
			ca.uqac.lif.cep.io.Print.Println pr = new ca.uqac.lif.cep.io.Print.Println();
			Connector.connect(m_pump, pr);
			associateInput(m_pump);
		}

		public void run()
		{
			m_pump.run();
		}
	}

	public static Function Eq(Object x, Object y)
	{
		return new FunctionTree(new EqualsVerbose(), liftFunction(x), liftFunction(y));
	}

	protected static class EqualsVerbose extends ca.uqac.lif.cep.util.Equals
	{
		public EqualsVerbose()
		{
			super();
		}

		@Override
		public Boolean getValue(Object x, Object y)
		{
			System.out.println("Comparing " + x + " to " + y);
			return isEqualTo(x, y);
		}
	}
}
