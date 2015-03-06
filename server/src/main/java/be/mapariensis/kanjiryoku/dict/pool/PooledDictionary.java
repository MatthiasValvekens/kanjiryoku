package be.mapariensis.kanjiryoku.dict.pool;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import be.mapariensis.kanjiryoku.config.ConfigFields;
import be.mapariensis.kanjiryoku.dict.DictionaryAccessException;
import be.mapariensis.kanjiryoku.dict.KanjidicInterface;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.IProperties;

public class PooledDictionary implements KanjidicInterface {
	public static final int MAX_IDLE_DEFAULT = 8;
	public static final int MAX_TOTAL_DEFAULT = 10;
	public static final int MIN_IDLE_DEFAULT = 0;
	public static final long MAX_WAIT_DEFAULT = 1000L;

	public static final class Factory implements KanjidicInterface.Factory {

		@SuppressWarnings("unchecked")
		@Override
		public KanjidicInterface setUp(IProperties config)
				throws DictionaryAccessException, BadConfigurationException {
			String bfcn = config.getRequired(ConfigFields.BACKEND_FACTORY,
					String.class);
			Class<? extends KanjidicInterface.Factory> factoryClass;
			try {
				factoryClass = (Class<? extends KanjidicInterface.Factory>) getClass()
						.getClassLoader().loadClass(bfcn);
			} catch (ClassNotFoundException | ClassCastException e) {
				throw new BadConfigurationException(e);
			}
			KanjidicInterface.Factory kdif;
			try {
				kdif = factoryClass.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new BadConfigurationException(e);
			}
			IProperties backendConfig = config.getRequired(
					ConfigFields.BACKEND_CONFIG, IProperties.class);
			return new PooledDictionary(kdif, backendConfig, config);
		}

	}

	private final PooledObjectFactory<KanjidicInterface> pof;
	private final GenericObjectPool<KanjidicInterface> kdiPool;

	public PooledDictionary(KanjidicInterface.Factory backendFactory,
			IProperties factoryConfiguration, IProperties poolConfiguration)
			throws BadConfigurationException {
		this.pof = new KanjidicInterfaceFactoryWrapper(backendFactory,
				factoryConfiguration);
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		int maxidle = poolConfiguration.getTyped(ConfigFields.MAX_IDLE,
				Integer.class, MAX_IDLE_DEFAULT);
		int minidle = poolConfiguration.getTyped(ConfigFields.MIN_IDLE,
				Integer.class, MIN_IDLE_DEFAULT);
		int maxtotal = poolConfiguration.getTyped(ConfigFields.MAX_TOTAL,
				Integer.class, MAX_TOTAL_DEFAULT);
		long maxWaitMillis = poolConfiguration.getTyped(ConfigFields.MAX_WAIT,
				Long.class, MAX_WAIT_DEFAULT);
		config.setBlockWhenExhausted(true);
		config.setMaxWaitMillis(maxWaitMillis);
		config.setMaxIdle(maxidle);
		config.setMinIdle(minidle);
		config.setMaxTotal(maxtotal);
		this.kdiPool = new GenericObjectPool<KanjidicInterface>(pof, config);
	}

	@Override
	public Set<String> getOn(char kanji) throws DictionaryAccessException {
		KanjidicInterface ki;
		try {
			ki = kdiPool.borrowObject();
		} catch (Exception e) {
			throw new DictionaryAccessException(e);
		}
		Set<String> res;
		try {
			res = ki.getOn(kanji);
		} finally {
			kdiPool.returnObject(ki);
		}
		return res;
	}

	@Override
	public Set<String> getKun(char kanji) throws DictionaryAccessException {
		KanjidicInterface ki;
		try {
			ki = kdiPool.borrowObject();
		} catch (Exception e) {
			throw new DictionaryAccessException(e);
		}
		Set<String> res;
		try {
			res = ki.getKun(kanji);
		} finally {
			kdiPool.returnObject(ki);
		}
		return res;
	}

	@Override
	public Set<Character> getKanjiByOn(String on)
			throws DictionaryAccessException {
		KanjidicInterface ki;
		try {
			ki = kdiPool.borrowObject();
		} catch (Exception e) {
			throw new DictionaryAccessException(e);
		}
		Set<Character> res;
		try {
			res = ki.getKanjiByOn(on);
		} finally {
			kdiPool.returnObject(ki);
		}
		return res;
	}

	@Override
	public Set<Character> getKanjiByKun(String kun)
			throws DictionaryAccessException {
		KanjidicInterface ki;
		try {
			ki = kdiPool.borrowObject();
		} catch (Exception e) {
			throw new DictionaryAccessException(e);
		}
		Set<Character> res;
		try {
			res = ki.getKanjiByKun(kun);
		} finally {
			kdiPool.returnObject(ki);
		}
		return res;
	}

	@Override
	public Set<Character> getSimilar(char kanji)
			throws DictionaryAccessException {
		KanjidicInterface ki;
		try {
			ki = kdiPool.borrowObject();
		} catch (Exception e) {
			throw new DictionaryAccessException(e);
		}
		Set<Character> res;
		try {
			res = ki.getSimilar(kanji);
		} finally {
			kdiPool.returnObject(ki);
		}
		return res;
	}

	@Override
	public Set<Character> randomKanji() throws DictionaryAccessException {
		KanjidicInterface ki;
		try {
			ki = kdiPool.borrowObject();
		} catch (Exception e) {
			throw new DictionaryAccessException(e);
		}
		Set<Character> res;
		try {
			res = ki.randomKanji();
		} finally {
			kdiPool.returnObject(ki);
		}
		return res;
	}

	@Override
	public void close() throws IOException {
		kdiPool.close();
	}

	@Override
	public boolean isOpen() {
		return !kdiPool.isClosed();
	}
}
