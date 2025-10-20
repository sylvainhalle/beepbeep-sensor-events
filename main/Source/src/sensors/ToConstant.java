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

import ca.uqac.lif.cep.functions.Constant;
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.functions.RaiseArity;
import ca.uqac.lif.cep.functions.UnaryFunction;

/**
 * Function that transforms an object <i>a</i> into the unary
 * function <i>f</i>(<i>x</i>) = <i>a</i>.
 * 
 * @author Sylvain Hallé
 */
public class ToConstant extends UnaryFunction<Object,Function>
{
	/**
	 * A single publicly visible instance of the function.
	 */
	public static final ToConstant instance = new ToConstant();
	
	/**
	 * Creates a new instance of the function.
	 */
	protected ToConstant()
	{
		super(Object.class, Function.class);
	}

	@Override
	public Function getValue(Object x)
	{
		return new RaiseArity(1, new Constant(x));
	}
}
