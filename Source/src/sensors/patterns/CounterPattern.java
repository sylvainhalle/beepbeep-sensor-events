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
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.TurnInto;

/**
 * A pattern that simply emits an increasing sequence of numbers, starting at
 * 1, upon every input event. Graphically, this pattern can be represented as
 * follows:
 * <p>
 * <img src="{@docRoot}/doc-files/CounterPattern.png" alt="Processor graph">
 * @author Sylvain Hallé
 */
public class CounterPattern extends GroupProcessor
{
	public CounterPattern()
	{
		super(1, 1);
		TurnInto one = new TurnInto(1);
		Cumulate sum = new Cumulate(Numbers.addition);
		Connector.connect(one, sum);
		addProcessors(one, sum);
		associateInput(one);
		associateOutput(sum);
	}
	
	@Override
	public CounterPattern duplicate(boolean with_state)
	{
		return new CounterPattern();
	}
}