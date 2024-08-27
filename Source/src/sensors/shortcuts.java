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

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.FunctionTree;
import ca.uqac.lif.cep.functions.IfThenElse;
import ca.uqac.lif.cep.functions.TurnInto;
import ca.uqac.lif.cep.tmf.Filter;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.Trim;

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
		GroupProcessor g = new GroupProcessor(1, 1);
		Fork fork = new Fork();
		Filter fil = new Filter();
		Connector.connect(fork, 0, fil, 0);
		Connector.connect(fork, 1, p, 0);
		Connector.connect(p, 0, fil, 1);
		g.addProcessors(fork, fil, p);
		g.associateInput(fork);
		g.associateOutput(fil);
		return g;
	}
	
	public static Processor Filter(Function f)
	{
		return Filter(new ApplyFunction(f));
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
		GroupProcessor g = new GroupProcessor(1, 1);
		Fork fork = new Fork();
		TurnInto one = new TurnInto(1);
		Connector.connect(fork, 0, one, 0);
		Cumulate sum = new Cumulate(Numbers.addition);
		Connector.connect(one, sum);
		Connector.connect(fork, 1, p, 0);
		Filter fil = new Filter();
		Connector.connect(sum, 0, fil, 0);
		Connector.connect(p, 0, fil, 1);
		g.addProcessors(fork, one, sum, p, fil);
		g.associateInput(fork);
		g.associateOutput(fil);
		return g;
	}
	
	public static Processor Count(Processor p)
	{
		GroupProcessor g = new GroupProcessor(1, 1);
		Fork fork = new Fork(3);
		Connector.connect(p, fork);
		ApplyFunction ite = new ApplyFunction(IfThenElse.instance);
		Connector.connect(fork, 0, ite, 0);
		TurnInto one = new TurnInto(1);
		Connector.connect(fork, 1, one, 0);
		Connector.connect(one, 0, ite, 1);
		TurnInto zero = new TurnInto(0);
		Connector.connect(fork, 2, zero, 0);
		Connector.connect(zero, 0, ite, 2);
		Cumulate sum = new Cumulate(Numbers.addition);
		Connector.connect(ite, sum);
		g.addProcessors(p, fork, ite, one, zero, sum);
		g.associateInput(p);
		g.associateOutput(sum);
		return g;
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
		GroupProcessor g = new GroupProcessor(1, 1);
		Fork fork = new Fork();
		Trim t = new Trim(1);
		Connector.connect(fork, 1, t, 0);
		ApplyFunction af = new ApplyFunction(f);
		Connector.connect(fork, 0, af, 0);
		Connector.connect(t, 0, af, 1);
		g.addProcessors(fork, t, af);
		g.associateInput(fork);
		g.associateOutput(af);
		return g;
	}
	
	public static Function LessThan(Object x, Object y)
	{
		return new FunctionTree(Numbers.isLessThan, liftFunction(x), liftFunction(y));
	}
	
	public static Function GreaterThan(Object x, Object y)
	{
		return new FunctionTree(Numbers.isGreaterThan, liftFunction(x), liftFunction(y));
	}
	
	public static Function Minus(Object x, Object y)
	{
		return new FunctionTree(Numbers.subtraction, liftFunction(x), liftFunction(y));
	}
	
	public static Function Minutes(Object x)
	{
		return new FunctionTree(Numbers.multiplication, liftFunction(x), liftFunction(60000l));
	}
	
	public static Function Hours(long x)
	{
		return new FunctionTree(Numbers.multiplication, liftFunction(x), liftFunction(60l * 60000l));
	}
	
	public static Function Flatten()
	{
		return Flatten.instance;
	}
	
	public static SpliceSource readJsonStreamFrom(String ... filenames)
	{
		return new SpliceSource.SpliceJsonStreamSource(false, filenames);
	}
	
	public static SpliceSource readJsonFrom(String ... filenames)
	{
		return new SpliceSource.SpliceJsonSource(false, filenames);
	}
	
	public static class GroupProcessor extends ca.uqac.lif.cep.GroupProcessor
	{
		public GroupProcessor(int in, int out)
		{
			super(in, out);
		}
	}
	
	public static class Slice extends ca.uqac.lif.cep.tmf.Slice
	{
		public Slice(ca.uqac.lif.cep.functions.Function f, ca.uqac.lif.cep.Processor p)
		{
			super(f, p);
		}
	}
	
	public static class JPathFunction extends ca.uqac.lif.cep.json.JPathFunction
	{
		public JPathFunction(String path)
		{
			super(path);
		}
	}
	
	public static class ApplyFunction extends ca.uqac.lif.cep.functions.ApplyFunction
	{
		public ApplyFunction(ca.uqac.lif.cep.functions.Function f)
		{
			super(f);
		}
	}
	
	public static class Sets extends ca.uqac.lif.cep.util.Sets
	{
		private Sets()
		{
			super();
		}
		
		public static class PutInto extends ca.uqac.lif.cep.util.Sets.PutInto
		{
			
		}
	}
	
	protected static Function liftFunction(Object o)
	{
		if (o instanceof Function)
		{
			return (Function) o;
		}
		return new Constant(o);
	}
	
	public static PullPrintln Write()
	{
		return new PullPrintln();
	}
	
	protected static class PullPrintln extends GroupProcessor
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
}
