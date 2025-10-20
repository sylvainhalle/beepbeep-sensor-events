package sensors;

import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;

/* An object carrying a {@link Processor} to be connected, and which expects
* a call to its {@link #call()} method after the connection is established.
* Currently the only use of this object is discussed in detail in
* {@link GroupProcessor#out(Processor)}.
* @see GroupProcessor#out(Processor)
* @author Sylvain Hallé
* @since 0.11.4
*/
public interface CallAfterConnect
{
	/**
	 * Gets the processor carried by this object.
	 * @return The processor
	 */
	public Processor getProcessor();
	
	/**
	 * Performs whatever action is needed after the connection is established.
	 */
	public void call();
}
