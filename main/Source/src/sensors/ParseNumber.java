package sensors;

import ca.uqac.lif.cep.functions.UnaryFunction;

public class ParseNumber extends UnaryFunction<String,Number>
{
	public static final transient ParseNumber instance = new ParseNumber();
	
	private ParseNumber()
	{
		super(String.class, Number.class);
	}

	@Override
	public Number getValue(String x)
	{
		return Double.parseDouble(x);
	}
}
