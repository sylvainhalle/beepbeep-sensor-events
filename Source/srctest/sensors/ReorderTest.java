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
import ca.uqac.lif.cep.tmf.QueueSink;
import ca.uqac.lif.cep.util.NthElement;

/**
 * Unit tests for {@link Reorder}.
 */
public class ReorderTest
{
	@Test
	public void test1()
	{
		Reorder r = new Reorder(new NthElement(0), 10);
		QueueSink s = new QueueSink();
		Queue<Object> q = s.getQueue();
		Connector.connect(r, s);
		Pushable p = r.getPushableInput();
		p.push(new Object[] {0l, "a"});
		assertEquals(0, q.size());
		p.push(new Object[] {1l, "b"});
		assertEquals(0, q.size());
		p.push(new Object[] {12l, "c"});
		assertEquals(2, q.size());
		p.push(new Object[] {22l, "d"});
		assertEquals(3, q.size());
	}
	
	@Test
	public void test2()
	{
		Reorder r = new Reorder(new NthElement(0), 10);
		QueueSink s = new QueueSink();
		Queue<Object> q = s.getQueue();
		Connector.connect(r, s);
		Object[] tuple;
		Pushable p = r.getPushableInput();
		p.push(new Object[] {3l, "a"});
		assertEquals(0, q.size());
		p.push(new Object[] {1l, "b"});
		assertEquals(0, q.size());
		p.push(new Object[] {2l, "c"});
		assertEquals(0, q.size());
		p.push(new Object[] {9l, "d"});
		assertEquals(0, q.size());
		p.push(new Object[] {12l, "e"});
		assertEquals(2, q.size()); // b, c
		tuple = (Object[]) q.remove();
		assertEquals("b", tuple[1]);
		tuple = (Object[]) q.remove();
		assertEquals("c", tuple[1]);
		p.push(new Object[] {13l, "f"});
		assertEquals(1, q.size()); // a
		tuple = (Object[]) q.remove();
		assertEquals("a", tuple[1]);
	}
}
