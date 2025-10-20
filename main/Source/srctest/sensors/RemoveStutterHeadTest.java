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

import static org.junit.Assert.*;

import java.util.Queue;

import org.junit.Test;

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.Pushable;
import ca.uqac.lif.cep.functions.BinaryFunction;
import ca.uqac.lif.cep.tmf.QueueSink;

/**
 * Unit tests for {@link RemoveStutterTail}.
 */
public class RemoveStutterHeadTest
{
	@Test
	public void test1()
	{
		RemoveStutterHead rst = new RemoveStutterHead(new CustomComparison());
		QueueSink sink = new QueueSink();
		Connector.connect(rst, sink);
		Queue<Object> q = sink.getQueue();
		Pushable p = rst.getPushableInput();
		p.push(0);
		assertEquals(0, q.size());
		p.push(0.1);
		assertEquals(0, q.size());
		p.push(1.1);
		assertEquals(1, q.size());
		assertEquals(0.1, q.remove());
		p.notifyEndOfTrace();
		assertEquals(1, q.size());
		assertEquals(1.1, q.remove());
	}
	
	@Test
	public void test2()
	{
		RemoveStutterHead rst = new RemoveStutterHead(new CustomComparison());
		QueueSink sink = new QueueSink();
		Connector.connect(rst, sink);
		Queue<Object> q = sink.getQueue();
		Pushable p = rst.getPushableInput();
		p.push(0);
		assertEquals(0, q.size());
		p.push(0.1);
		assertEquals(0, q.size());
		p.push(0.2);
		p.notifyEndOfTrace();
		assertEquals(1, q.size());
		assertEquals(0.2, q.remove());
	}
	
	protected static class CustomComparison extends BinaryFunction<Number,Number,Boolean>
	{
		public CustomComparison()
		{
			super(Number.class, Number.class, Boolean.class);
		}

		@Override
		public Boolean getValue(Number x, Number y)
		{
			return Math.round(x.doubleValue()) == Math.round(y.doubleValue());
		}
	}
}
