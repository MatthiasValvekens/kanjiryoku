package be.mapariensis.kanjiryoku.dict.pool;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import be.mapariensis.kanjiryoku.dict.KanjidicInterface;
import be.mapariensis.kanjiryoku.util.IProperties;

public class KanjidicInterfaceFactoryWrapper implements
		PooledObjectFactory<KanjidicInterface> {
	private final KanjidicInterface.Factory backend;
	private final IProperties backendConfig;

	public KanjidicInterfaceFactoryWrapper(KanjidicInterface.Factory backend,
			IProperties backendConfig) {
		this.backend = backend;
		this.backendConfig = backendConfig;
	}

	@Override
	public void activateObject(PooledObject<KanjidicInterface> arg0)
			throws Exception {

	}

	@Override
	public void destroyObject(PooledObject<KanjidicInterface> arg0)
			throws Exception {
		arg0.getObject().close();
	}

	@Override
	public PooledObject<KanjidicInterface> makeObject() throws Exception {
		return new DefaultPooledObject<KanjidicInterface>(
				backend.setUp(backendConfig));
	}

	@Override
	public void passivateObject(PooledObject<KanjidicInterface> arg0)
			throws Exception {

	}

	@Override
	public boolean validateObject(PooledObject<KanjidicInterface> arg0) {
		return arg0.getObject().isOpen();
	}

}
