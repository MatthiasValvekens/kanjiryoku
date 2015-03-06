package be.mapariensis.kanjiryoku.cr.pool;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import be.mapariensis.kanjiryoku.cr.KanjiGuesser;
import be.mapariensis.kanjiryoku.cr.KanjiGuesserFactory;
import be.mapariensis.kanjiryoku.util.IProperties;

public class KanjiGuesserFactoryWrapper implements
		PooledObjectFactory<KanjiGuesser> {
	private final KanjiGuesserFactory backend;
	private final IProperties backendConfig;

	public KanjiGuesserFactoryWrapper(KanjiGuesserFactory backend,
			IProperties backendConfig) {
		this.backend = backend;
		this.backendConfig = backendConfig;
	}

	@Override
	public void activateObject(PooledObject<KanjiGuesser> arg0)
			throws Exception {

	}

	@Override
	public void destroyObject(PooledObject<KanjiGuesser> arg0) throws Exception {
		arg0.getObject().close();
	}

	@Override
	public PooledObject<KanjiGuesser> makeObject() throws Exception {
		return new DefaultPooledObject<KanjiGuesser>(
				backend.getGuesser(backendConfig));
	}

	@Override
	public void passivateObject(PooledObject<KanjiGuesser> arg0)
			throws Exception {

	}

	@Override
	public boolean validateObject(PooledObject<KanjiGuesser> arg0) {
		return arg0.getObject().isOpen();
	}

}
