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

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.tmf.Filter;
import ca.uqac.lif.cep.tmf.Fork;

/**
 * A 1:1 processor chain that retains events in a stream at indices
 * where another processor chain <i>P</i> produces the value <em>true</em>
 * (&top;). The chain is represented graphically as:
 * <p>
 * <img src="{@docRoot}/doc-files/FilterPattern.png" alt="Pattern" />
 */
public class FilterPattern extends GroupProcessor
{
	public FilterPattern(Processor p)
	{
		super(1, 1);
		Fork fork = new Fork();
		Filter fil = new Filter();
		Connector.connect(fork, 0, fil, 0);
		Connector.connect(fork, 1, p, 0);
		Connector.connect(p, 0, fil, 1);
		addProcessors(fork, fil, p);
		associateInput(fork);
		associateOutput(fil);
	}
}
