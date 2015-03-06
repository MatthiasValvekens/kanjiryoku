package be.mapariensis.kanjiryoku.cr.pool;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.config.ConfigFields;
import be.mapariensis.kanjiryoku.cr.Dot;
import be.mapariensis.kanjiryoku.cr.KanjiGuesser;
import be.mapariensis.kanjiryoku.cr.KanjiGuesserFactory;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.IProperties;

public class PooledGuesser implements KanjiGuesser {
	private static final Logger log = LoggerFactory
			.getLogger(PooledGuesser.class);

	public static final int MAX_IDLE_DEFAULT = 8;
	public static final int MAX_TOTAL_DEFAULT = 10;
	public static final int MIN_IDLE_DEFAULT = 0;
	public static final long MAX_WAIT_DEFAULT = 1000L;

	public static final String BACKEND_FACTORY = "backendFactory";
	public static final String BACKEND_CONFIG = "backendConfig";

	public static final class Factory implements KanjiGuesserFactory {

		@SuppressWarnings("unchecked")
		@Override
		public KanjiGuesser getGuesser(IProperties config)
				throws BadConfigurationException, IOException {
			String bfcn = config.getRequired(BACKEND_FACTORY, String.class);
			Class<? extends KanjiGuesserFactory> kgfc;
			try {
				kgfc = (Class<? extends KanjiGuesserFactory>) getClass()
						.getClassLoader().loadClass(bfcn);
			} catch (ClassNotFoundException e) {
				throw new BadConfigurationException(e);
			}
			KanjiGuesserFactory kgf;
			try {
				kgf = kgfc.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new BadConfigurationException(e);
			}
			IProperties backendConfig = config.getRequired(BACKEND_CONFIG,
					IProperties.class);
			return new PooledGuesser(kgf, backendConfig, config);
		}

	}

	private final PooledObjectFactory<KanjiGuesser> pof;
	private final GenericObjectPool<KanjiGuesser> guesserPool;

	public PooledGuesser(KanjiGuesserFactory backendFactory,
			IProperties factoryConfiguration, IProperties poolConfiguration)
			throws BadConfigurationException {
		this.pof = new KanjiGuesserFactoryWrapper(backendFactory,
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
		this.guesserPool = new GenericObjectPool<KanjiGuesser>(pof, config);
	}

	@Override
	public List<Character> guess(int width, int height, List<List<Dot>> strokes) {
		KanjiGuesser kg;
		try {
			kg = guesserPool.borrowObject();
		} catch (Exception e) {
			log.error("Failed to borrow guesser.", e);
			return Collections.emptyList();
		}
		List<Character> res;
		try {
			res = kg.guess(width, height, strokes);
		} finally {
			guesserPool.returnObject(kg);
		}
		return res;
	}

	@Override
	public void close() {
		guesserPool.close();
	}

	@Override
	public boolean isOpen() {
		return !guesserPool.isClosed();
	}

}
