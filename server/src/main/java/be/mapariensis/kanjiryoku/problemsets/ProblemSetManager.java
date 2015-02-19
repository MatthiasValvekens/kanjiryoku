package be.mapariensis.kanjiryoku.problemsets;

import java.util.List;

import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.util.IProperties;

public interface ProblemSetManager {

	public void loadNewConfig(String name, IProperties conf)
			throws BadConfigurationException;

	public ProblemOrganizer getProblemSets(int seed, List<String> names)
			throws BadConfigurationException;

}