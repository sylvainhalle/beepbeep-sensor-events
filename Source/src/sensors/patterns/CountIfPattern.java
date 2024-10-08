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
package sensors.patterns;

import beepbeep.groovy.Numbers;
import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.IfThenElse;
import ca.uqac.lif.cep.functions.TurnInto;
import ca.uqac.lif.cep.tmf.Fork;

/**
 * Counts the number of times that a processor <i>P</i> produces the value
 * <em>true</em> (&top;) when ingesting a given input stream.
 * @author Sylvain Hallé
 */
public class CountIfPattern extends GroupProcessor
{
	/**
	 * Creates a new instance of the pattern.
	 * @param p The 1:1 processor chain <i>P</i> producing the Boolean stream
	 */
	public CountIfPattern(Processor p)
	{
		super(1, 1);
		Fork fork = new Fork(3);
		Connector.connect(p, fork);
		ca.uqac.lif.cep.functions.ApplyFunction ite = new ca.uqac.lif.cep.functions.ApplyFunction(IfThenElse.instance);
		Connector.connect(fork, 0, ite, 0);
		TurnInto one = new TurnInto(1);
		Connector.connect(fork, 1, one, 0);
		Connector.connect(one, 0, ite, 1);
		TurnInto zero = new TurnInto(0);
		Connector.connect(fork, 2, zero, 0);
		Connector.connect(zero, 0, ite, 2);
		Cumulate sum = new Cumulate(Numbers.addition);
		Connector.connect(ite, sum);
		addProcessors(p, fork, ite, one, zero, sum);
		associateInput(p);
		associateOutput(sum);
	}
}
