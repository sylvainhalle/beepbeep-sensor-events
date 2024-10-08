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
import ca.uqac.lif.cep.functions.TurnInto;
import ca.uqac.lif.cep.tmf.Filter;
import ca.uqac.lif.cep.tmf.Fork;

/**
 * Creates a processor chain that outputs the indices in a stream where
 * another processor chain <i>P</i> processing that stream produces the value
 * <em>true</em> (&top;). The chain is represented graphically as:
 * <p>
 * <img src="{@docRoot}/doc-files/LocatePattern.png" alt="Pattern" />
 */
public class LocatePattern extends GroupProcessor
{
	/**
	 * Creates a new instance of the pattern.
	 * @param p The 1:1 processor chain <i>P</i> producing the Boolean stream
	 */
	public LocatePattern(Processor p)
	{
		super(1, 1);
		Fork fork = new Fork();
		TurnInto one = new TurnInto(1);
		Connector.connect(fork, 0, one, 0);
		Cumulate sum = new Cumulate(Numbers.addition);
		Connector.connect(one, sum);
		Connector.connect(fork, 1, p, 0);
		Filter fil = new Filter();
		Connector.connect(sum, 0, fil, 0);
		Connector.connect(p, 0, fil, 1);
		addProcessors(fork, one, sum, p, fil);
		associateInput(fork);
		associateOutput(fil);
	}
}
