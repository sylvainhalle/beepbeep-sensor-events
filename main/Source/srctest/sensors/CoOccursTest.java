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

import static org.junit.Assert.assertEquals;

import java.util.Queue;

import org.junit.Test;

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.Pushable;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.UnaryFunction;
import ca.uqac.lif.cep.tmf.QueueSink;
import sensors.CoOccurs.CheckCoOccurrence;

public class CoOccursTest
{
	@Test
	public void testCheckCoOccurrence1()
	{
		CheckCoOccurrence cco = new CheckCoOccurrence(100, 3);
		QueueSink qs = new QueueSink();
		Queue<Object> q = qs.getQueue();
		Connector.connect(cco, qs);
		pushCco(cco, 1, true, 1, -1, -1);
		assertEquals(0, q.size());
		pushCco(cco, 9, false, 1, 9, -1);
		assertEquals(0, q.size());
		pushCco(cco, 13, false, 1, 9, 13);
		assertEquals(3, q.size());
		assertEquals(true, q.remove());
		assertEquals(true, q.remove());
		assertEquals(true, q.remove());
	}
	
	@Test
	public void testCheckCoOccurrence2()
	{
		CheckCoOccurrence cco = new CheckCoOccurrence(100, 3);
		QueueSink qs = new QueueSink();
		Queue<Object> q = qs.getQueue();
		Connector.connect(cco, qs);
		pushCco(cco, 1, true, 1, -1, -1);
		assertEquals(0, q.size());
		pushCco(cco, 9, false, 1, 9, -1);
		assertEquals(0, q.size());
		pushCco(cco, 113, false, 1, 9, 113);
		assertEquals(3, q.size());
		assertEquals(false, q.remove());
		assertEquals(true, q.remove());
		assertEquals(true, q.remove());
	}
	
	@Test
	public void testCheckCoOccurrence3()
	{
		CheckCoOccurrence cco = new CheckCoOccurrence(100, 3);
		QueueSink qs = new QueueSink();
		Queue<Object> q = qs.getQueue();
		Connector.connect(cco, qs);
		pushCco(cco, 0, false, -1, -1, -1);
		assertEquals(1, q.size());
		assertEquals(true, q.remove());
		pushCco(cco, 1, true, 1, -1, -1);
		assertEquals(0, q.size());
		pushCco(cco, 9, false, 1, 9, -1);
		assertEquals(0, q.size());
		pushCco(cco, 13, false, 1, 9, 13);
		assertEquals(3, q.size());
		assertEquals(true, q.remove());
		assertEquals(true, q.remove());
		assertEquals(true, q.remove());
	}
	
	@Test
	public void testCoOccurs1()
	{
		CoOccurs co = new CoOccurs(100, new GetTimestamp(), 
				new ApplyFunction(new HasLabel("a")),
				new ApplyFunction(new HasLabel("a")),
				new ApplyFunction(new HasLabel("b")),
				new ApplyFunction(new HasLabel("c")));
		QueueSink qs = new QueueSink();
		Queue<Object> q = qs.getQueue();
		Connector.connect(co, qs);
		Pushable p = co.getPushableInput();
		p.push(new TimestampedEvent(1, "a"));
		assertEquals(0, q.size());
		p.push(new TimestampedEvent(10, "b"));
		assertEquals(0, q.size());
		p.push(new TimestampedEvent(23, "c"));
		assertEquals(3, q.size());
		assertEquals(true, q.remove());
		assertEquals(true, q.remove());
		assertEquals(true, q.remove());
	}
	
	@Test
	public void testCoOccurs2()
	{
		CoOccurs co = new CoOccurs(100, new GetTimestamp(), 
				new ApplyFunction(new HasLabel("a")),
				new ApplyFunction(new HasLabel("a")),
				new ApplyFunction(new HasLabel("b")),
				new ApplyFunction(new HasLabel("c")));
		QueueSink qs = new QueueSink();
		Queue<Object> q = qs.getQueue();
		Connector.connect(co, qs);
		Pushable p = co.getPushableInput();
		p.push(new TimestampedEvent(1, "a"));
		assertEquals(0, q.size());
		p.push(new TimestampedEvent(10, "b"));
		assertEquals(0, q.size());
		p.push(new TimestampedEvent(123, "c"));
		assertEquals(3, q.size());
		assertEquals(false, q.remove());
		assertEquals(true, q.remove());
		assertEquals(true, q.remove());
	}
	
	@Test
	public void testCoOccurs3()
	{
		CoOccurs co = new CoOccurs(100, new GetTimestamp(), 
				new ApplyFunction(new HasLabel("a")),
				new ApplyFunction(new HasLabel("a")),
				new ApplyFunction(new HasLabel("b")),
				new ApplyFunction(new HasLabel("c")));
		QueueSink qs = new QueueSink();
		Queue<Object> q = qs.getQueue();
		Connector.connect(co, qs);
		Pushable p = co.getPushableInput();
		p.push(new TimestampedEvent(1, "a"));
		assertEquals(0, q.size());
		p.push(new TimestampedEvent(50, "a"));
		assertEquals(0, q.size());
		p.push(new TimestampedEvent(123, "c"));
		assertEquals(1, q.size());
		assertEquals(false, q.remove());
		p.push(new TimestampedEvent(130, "b"));
		assertEquals(3, q.size());
		assertEquals(true, q.remove());
		assertEquals(true, q.remove());
		assertEquals(true, q.remove());
	}
	
	protected static void pushCco(CheckCoOccurrence cco, Object ... front)
	{
		for (int i = 0; i < front.length; i++)
		{
			cco.getPushableInput(i).push(front[i]);
		}
	}
	
	protected static class TimestampedEvent
	{
		public long m_ts;
		
		public String m_label;
		
		public TimestampedEvent(long ts, String l)
		{
			super();
			m_ts = ts;
			m_label = l;
		}
	}
	
	protected static class GetTimestamp extends UnaryFunction<TimestampedEvent,Long>
	{
		public GetTimestamp()
		{
			super(TimestampedEvent.class, Long.class);
		}

		@Override
		public Long getValue(TimestampedEvent x)
		{
			return x.m_ts;
		}
	}
	
	protected static class HasLabel extends UnaryFunction<TimestampedEvent,Boolean>
	{
		private final String m_compareTo;
		
		public HasLabel(String l)
		{
			super(TimestampedEvent.class, Boolean.class);
			m_compareTo = l;
		}

		@Override
		public Boolean getValue(TimestampedEvent x)
		{
			return x.m_label.compareTo(m_compareTo) == 0;
		}
	}
}
