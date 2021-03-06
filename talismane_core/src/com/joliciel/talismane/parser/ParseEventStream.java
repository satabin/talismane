///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.ClassificationEvent;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.parser.features.ParseConfigurationFeature;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * A classification event stream for parse configurations.
 * @author Assaf Urieli
 *
 */
class ParseEventStream implements ClassificationEventStream {
    private static final Log LOG = LogFactory.getLog(ParseEventStream.class);
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(ParseEventStream.class);

    ParserAnnotatedCorpusReader corpusReader;
    Set<ParseConfigurationFeature<?>> parseFeatures;
    
	ParseConfiguration targetConfiguration;
	ParseConfiguration currentConfiguration;
	
	ParserServiceInternal parserServiceInternal;
	MachineLearningService machineLearningService;
	FeatureService featureService;

	int currentIndex;
	
	ParseEventStream(ParserAnnotatedCorpusReader corpusReader, Set<ParseConfigurationFeature<?>> parseFeatures) {
		this.corpusReader = corpusReader;
		this.parseFeatures = parseFeatures;
	}

	@Override
	public boolean hasNext() {
		MONITOR.startTask("hasNext");
		try {
			while (targetConfiguration==null) {
				if (this.corpusReader.hasNextConfiguration()) {
					
					targetConfiguration = this.corpusReader.nextConfiguration();
					currentConfiguration = parserServiceInternal.getInitialConfiguration(targetConfiguration.getPosTagSequence());
					currentIndex = 0;
					if (currentIndex == targetConfiguration.getTransitions().size()) {
						targetConfiguration = null;
					}
				} else {
					break;
				}
			}
			
			if (targetConfiguration==null) {
				LOG.debug("Event stream reading complete");
			}
			return targetConfiguration!=null;
		} finally {
			MONITOR.endTask("hasNext");
		}
	}

	@Override
	public ClassificationEvent next() {
		MONITOR.startTask("next");
		try {
			ClassificationEvent event = null;
			if (this.hasNext()) {
				LOG.debug("next event, configuration: " + currentConfiguration.toString());
		
				List<FeatureResult<?>> parseFeatureResults = new ArrayList<FeatureResult<?>>();
				for (ParseConfigurationFeature<?> parseFeature : parseFeatures) {
					MONITOR.startTask(parseFeature.getName());
					try {
						RuntimeEnvironment env = this.featureService.getRuntimeEnvironment();
						FeatureResult<?> featureResult = parseFeature.check(currentConfiguration, env);
						if (featureResult!=null) {
							parseFeatureResults.add(featureResult);
							if (LOG.isTraceEnabled()) {
								LOG.trace(featureResult.toString());
							}
						}	
					} finally {
						MONITOR.endTask(parseFeature.getName());
					}
				}
				
				Transition transition = targetConfiguration.getTransitions().get(currentIndex);
				String classification = transition.getCode();
				event = this.machineLearningService.getClassificationEvent(parseFeatureResults, classification);
				
				// apply the transition and up the index
				currentConfiguration = parserServiceInternal.getConfiguration(currentConfiguration);
				transition.apply(currentConfiguration);
				currentIndex++;
	
				if (currentIndex==targetConfiguration.getTransitions().size()) {
					targetConfiguration = null;
				}
			}
			return event;
		} finally {
			MONITOR.endTask("next");
		}
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String,String> attributes = new LinkedHashMap<String, String>();
		attributes.put("eventStream", this.getClass().getSimpleName());		
		attributes.put("corpusReader", corpusReader.getClass().getSimpleName());		
		
		attributes.putAll(corpusReader.getCharacteristics());
		return attributes;
	}

	public ParserServiceInternal getParserServiceInternal() {
		return parserServiceInternal;
	}

	public void setParserServiceInternal(ParserServiceInternal parserServiceInternal) {
		this.parserServiceInternal = parserServiceInternal;
	}

	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

	public FeatureService getFeatureService() {
		return featureService;
	}

	public void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
	}

	
}
