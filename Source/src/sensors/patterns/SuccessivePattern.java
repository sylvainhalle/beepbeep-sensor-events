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
import ca.uqac.lif.cep.functions.Function;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.Trim;

/**
 * Creates a processor chain that evaluates a function on every pair of
 * successive events. The chain is represented graphically as:
 * <p>
 * <img src="{@docRoot}/doc-files/SuccessivePattern.png" alt="Pattern" />
 */
public class SuccessivePattern extends GroupProcessor
{
	public SuccessivePattern(Function f)
	{
		super(1, 1);
		Fork fork = new Fork();
		Trim t = new Trim(1);
		Connector.connect(fork, 1, t, 0);
		ca.uqac.lif.cep.functions.ApplyFunction af = new ca.uqac.lif.cep.functions.ApplyFunction(f);
		Connector.connect(fork, 0, af, 0);
		Connector.connect(t, 0, af, 1);
		addProcessors(fork, t, af);
		associateInput(fork);
		associateOutput(af);
	}
}