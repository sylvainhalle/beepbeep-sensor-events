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
package sensors;

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
 * import static nears.Shortcuts.*
 * </pre>
 * and get access to all these classes on a "first-name basis".
 * <p>
 * The use of these class names is totally optional and normal BeepBeep class
 * may be used anywhere instead.
 * 
 * @author Sylvain Hallé
 */
public class Shortcuts
{
	private Shortcuts()
	{
		super();
	}
	
	public static SpliceSource readLinesFrom(String ... filenames)
	{
		return new SpliceSource.SpliceLineSource(false, filenames);
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
	
	public static class KeepLast extends ca.uqac.lif.cep.tmf.KeepLast
	{
		
	}
	
	public static class Print extends ca.uqac.lif.cep.io.Print
	{
		
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
}
