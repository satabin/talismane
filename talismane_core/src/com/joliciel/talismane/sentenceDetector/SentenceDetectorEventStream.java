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
package com.joliciel.talismane.sentenceDetector;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.machineLearning.ClassificationEvent;
import com.joliciel.talismane.machineLearning.ClassificationEventStream;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.FeatureService;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeature;
import com.joliciel.talismane.sentenceDetector.features.SentenceDetectorFeatureService;

class SentenceDetectorEventStream implements ClassificationEventStream {
    private static final Log LOG = LogFactory.getLog(SentenceDetectorEventStream.class);
    
	private SentenceDetectorAnnotatedCorpusReader corpusReader;
	private Set<SentenceDetectorFeature<?>> features;
	private String currentSentence;
	private String previousSentence = ". ";
	private int currentIndex = 0;
	private List<Integer> possibleBoundaries = new ArrayList<Integer>();
	private int realBoundary = -1;
	private LinkedList<String> sentences = new LinkedList<String>();
	int minCharactersAfterBoundary = 50;
	
	private SentenceDetectorService sentenceDetectorService;
	private SentenceDetectorFeatureService sentenceDetectorFeatureService;
	private MachineLearningService machineLearningService;
	private FeatureService featureService;
	
	public SentenceDetectorEventStream(
			SentenceDetectorAnnotatedCorpusReader corpusReader,
			Set<SentenceDetectorFeature<?>> features) {
		super();
		this.corpusReader = corpusReader;
		this.features = features;
	}

	@Override
	public ClassificationEvent next() {
		ClassificationEvent event = null;
		if (this.hasNext()) {
			int possibleBoundary = possibleBoundaries.get(currentIndex++);
			
			String moreText = "";
			int sentenceIndex = 0;
			while (moreText.length() < minCharactersAfterBoundary) {
				String nextSentence = "";
				if (sentenceIndex<sentences.size()) {
					nextSentence = sentences.get(sentenceIndex);
				} else if (corpusReader.hasNextSentence()) {
					nextSentence = corpusReader.nextSentence();
					sentences.add(nextSentence);
				} else {
					break;
				}
				if (nextSentence.startsWith(" ")||nextSentence.startsWith("\n"))
					moreText += sentences.get(sentenceIndex);
				else
					moreText += " " + sentences.get(sentenceIndex);
					
				sentenceIndex++;
			}
			String text = previousSentence + currentSentence + moreText;
			
			PossibleSentenceBoundary boundary = sentenceDetectorService.getPossibleSentenceBoundary(text, possibleBoundary);
			LOG.debug("next event, boundary: " + boundary);

			List<FeatureResult<?>> featureResults = new ArrayList<FeatureResult<?>>();
			for (SentenceDetectorFeature<?> feature : features) {
				RuntimeEnvironment env = this.featureService.getRuntimeEnvironment();
				FeatureResult<?> featureResult = feature.check(boundary, env);
				if (featureResult!=null)
					featureResults.add(featureResult);
			}
			if (LOG.isTraceEnabled()) {
				for (FeatureResult<?> result : featureResults) {
					LOG.trace(result.toString());
				}
			}			
			String classification = SentenceDetectorOutcome.IS_NOT_BOUNDARY.name();
			if (possibleBoundary==realBoundary)
				classification = SentenceDetectorOutcome.IS_BOUNDARY.name();
			
			event = this.machineLearningService.getClassificationEvent(featureResults, classification);
			
			if (currentIndex==possibleBoundaries.size()) {
				if (currentSentence.endsWith(" "))
					previousSentence = currentSentence;
				else
					previousSentence = currentSentence + " ";
				currentSentence = null;
			}
		}
		return event;
	}

	@Override
	public boolean hasNext() {
		while (currentSentence==null) {
			currentIndex = 0;
			possibleBoundaries = new ArrayList<Integer>();
			if (!sentences.isEmpty()) {
				currentSentence = sentences.poll();
			} else if (corpusReader.hasNextSentence()) {
				currentSentence = corpusReader.nextSentence();
			} else {
				break;
			}
			if (currentSentence!=null) {
				Matcher matcher = SentenceDetector.POSSIBLE_BOUNDARIES.matcher(currentSentence);
				
				while (matcher.find()) {
					possibleBoundaries.add(previousSentence.length() + matcher.start());
				}
				realBoundary = previousSentence.length() + currentSentence.length() - 1;
			}
			if (possibleBoundaries.size()==0) {
				if (currentSentence.endsWith(" "))
					previousSentence = currentSentence;
				else
					previousSentence = currentSentence + " ";
				currentSentence=null;
			}
		}
		
		return (currentSentence!=null);
	}

	public SentenceDetectorService getSentenceDetectorService() {
		return sentenceDetectorService;
	}

	public void setSentenceDetectorService(
			SentenceDetectorService sentenceDetectorService) {
		this.sentenceDetectorService = sentenceDetectorService;
	}

	public SentenceDetectorFeatureService getSentenceDetectorFeatureService() {
		return sentenceDetectorFeatureService;
	}

	public void setSentenceDetectorFeatureService(
			SentenceDetectorFeatureService sentenceDetectorFeatureService) {
		this.sentenceDetectorFeatureService = sentenceDetectorFeatureService;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String,String> attributes = new LinkedHashMap<String, String>();
		attributes.put("eventStream", this.getClass().getSimpleName());		
		attributes.put("corpusReader", corpusReader.getClass().getSimpleName());		
		
		return attributes;
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
