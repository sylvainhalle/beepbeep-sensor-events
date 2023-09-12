package nears;

import static org.junit.Assert.*;

import org.junit.Test;

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.Pullable;
import ca.uqac.lif.cep.tmf.QueueSink;
import ca.uqac.lif.cep.tuples.Tuple;
import ca.uqac.lif.fs.FileSystemException;
import ca.uqac.lif.fs.FileUtils;
import ca.uqac.lif.fs.HardDisk;
import ca.uqac.lif.fs.RamDisk;
import ca.uqac.lif.json.JsonElement;

public class SpliceInputStreamTest
{
	@Test
	public void test1() throws FileSystemException
	{
		RamDisk rd = new RamDisk();
		FileUtils.writeStringTo(rd, "foo\nbar\nbaz", "file1");
		FileUtils.writeStringTo(rd, "1\n2\n3\n4", "file2");
		SpliceInputStream sis = new SpliceInputStream(rd, false, "file1", "file2");
		Pullable p = sis.getPullableOutput();
		assertEquals("foo", p.pull());
		assertEquals("bar", p.pull());
		assertEquals("baz", p.pull());
		assertEquals("1", p.pull());
		assertEquals("2", p.pull());
		assertEquals("3", p.pull());
		assertEquals("4", p.pull());
		assertFalse(p.hasNext());
		sis.stop();
		rd.close();
	}
	
	@Test
	public void test2() throws FileSystemException
	{
		RamDisk rd = new RamDisk();
		FileUtils.writeStringTo(rd, "foo\nbar\nbaz", "file1");
		FileUtils.writeStringTo(rd, "1\n2\n3\n4", "file2");
		SpliceInputStream sis = new SpliceInputStream(rd, false, "file1", "file2");
		QueueSink sink = new QueueSink();
		Connector.connect(sis, sink);
		sis.start();
		assertEquals(7, sink.getQueue().size());
		sis.stop();
		rd.close();
	}
	
	@Test
	public void test3() throws FileSystemException
	{
		RamDisk rd = new RamDisk();
		FileUtils.writeStringTo(rd, "{\"foo\" : 123}\n{\"bar\" : 234}", "file1.json");
		FileUtils.writeStringTo(rd, "{\"foo\" : 123}\n{\"bar\" : 234}", "file2.json");
		SpliceInputStream sis = new SpliceInputStream(rd, false, "file1.json", "file2.json");
		Pullable p = sis.getPullableOutput();
		assertTrue(p.pull() instanceof JsonElement);
		sis.stop();
		rd.close();
	}
	
	@Test
	public void test4() throws FileSystemException
	{
		RamDisk rd = new RamDisk().open();
		FileUtils.writeStringTo(rd, "foo,bar\n1,2\n3,4", "file1.csv");
		FileUtils.writeStringTo(rd, "foo,bar\n5,6\n7,8", "file2.csv");
		SpliceInputStream sis = new SpliceInputStream(rd, false, "file1.csv", "file2.csv");
		Pullable p = sis.getPullableOutput();
		Tuple t;
		t = (Tuple) p.pull();
		assertEquals("1", t.get("foo"));
		sis.stop();
		rd.close();
	}
	
	@Test
	public void test5() throws FileSystemException
	{
		HardDisk rd = new HardDisk().open();
		SpliceInputStream sis = new SpliceInputStream(rd, false, "file1.csv", "file2.csv");
		Pullable p = sis.getPullableOutput();
		Tuple t;
		t = (Tuple) p.pull();
		assertEquals("1", t.get("foo"));
		sis.stop();
		rd.close();
	}
}
