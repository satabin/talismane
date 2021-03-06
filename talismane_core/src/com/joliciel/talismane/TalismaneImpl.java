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
package com.joliciel.talismane;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.talismane.filters.FilterService;
import com.joliciel.talismane.filters.RollingSentenceProcessor;
import com.joliciel.talismane.filters.Sentence;
import com.joliciel.talismane.filters.SentenceHolder;
import com.joliciel.talismane.filters.TextMarker;
import com.joliciel.talismane.filters.TextMarkerFilter;
import com.joliciel.talismane.machineLearning.ClassificationModel;
import com.joliciel.talismane.machineLearning.ClassificationModelTrainer;
import com.joliciel.talismane.machineLearning.DecisionFactory;
import com.joliciel.talismane.machineLearning.ExternalResourceFinder;
import com.joliciel.talismane.machineLearning.MachineLearningAlgorithm;
import com.joliciel.talismane.machineLearning.MachineLearningModel;
import com.joliciel.talismane.machineLearning.MachineLearningService;
import com.joliciel.talismane.machineLearning.Ranker;
import com.joliciel.talismane.machineLearning.RankingEventStream;
import com.joliciel.talismane.machineLearning.RankingModelTrainer;
import com.joliciel.talismane.machineLearning.MachineLearningModel.MachineLearningModelType;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronClassificationModelTrainer;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronModelTrainerObserver;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronService;
import com.joliciel.talismane.machineLearning.perceptron.PerceptronServiceLocator;
import com.joliciel.talismane.parser.NonDeterministicParser;
import com.joliciel.talismane.parser.ParseComparator;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.parser.ParserEvaluator;
import com.joliciel.talismane.parser.ParserService;
import com.joliciel.talismane.parser.ParsingConstrainer;
import com.joliciel.talismane.parser.Transition;
import com.joliciel.talismane.posTagger.NonDeterministicPosTagger;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagComparator;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSequenceProcessor;
import com.joliciel.talismane.posTagger.PosTaggerEvaluator;
import com.joliciel.talismane.posTagger.PosTaggerService;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorOutcome;
import com.joliciel.talismane.sentenceDetector.SentenceDetectorService;
import com.joliciel.talismane.sentenceDetector.SentenceProcessor;
import com.joliciel.talismane.tokeniser.TokenComparator;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.joliciel.talismane.tokeniser.TokenSequenceProcessor;
import com.joliciel.talismane.tokeniser.TokeniserEvaluator;
import com.joliciel.talismane.tokeniser.TokeniserOutcome;
import com.joliciel.talismane.tokeniser.TokeniserService;
import com.joliciel.talismane.tokeniser.patterns.TokeniserPatternService.PatternTokeniserType;
import com.joliciel.talismane.utils.CSVFormatter;
import com.joliciel.talismane.utils.LogUtils;
import com.joliciel.talismane.utils.PerformanceMonitor;

/**
 * The default implementation for the Talismane interface.
 * @author Assaf Urieli
 */
class TalismaneImpl implements Talismane {
	private static final Log LOG = LogFactory.getLog(TalismaneImpl.class);
	private static final CSVFormatter CSV = new CSVFormatter();
	private static final PerformanceMonitor MONITOR = PerformanceMonitor.getMonitor(TalismaneImpl.class);

	private Reader reader;
	private Writer writer;
	private SentenceProcessor sentenceProcessor;
	private TokenSequenceProcessor tokenSequenceProcessor;
	private PosTagSequenceProcessor posTagSequenceProcessor;
	private ParseConfigurationProcessor parseConfigurationProcessor;
	
	private SentenceDetectorService sentenceDetectorService;
	private TokeniserService tokeniserService;
	private PosTaggerService posTaggerService;
	private ParserService parserService;
	private FilterService filterService;
	private MachineLearningService machineLearningService;
	
	private boolean stopOnError = true;

	private TalismaneConfig config;
	
	protected TalismaneImpl(TalismaneConfig config) {
		this.config = config;
	}

	@Override
	public void process() {
		long startTime = new Date().getTime();

		PerformanceMonitor.start(config.getPerformanceConfigFile());
		try {
			if (!config.getCommand().equals(Command.process)) {
				// force a lexicon read at the start, so that it doesn't skew performance monitoring downstream
				TalismaneSession.getLexicon();
			}
			if (this.getSentenceProcessor()==null)
				this.setSentenceProcessor(config.getSentenceProcessor());
			if (this.getTokenSequenceProcessor()==null)
				this.setTokenSequenceProcessor(config.getTokenSequenceProcessor());
			if (this.getPosTagSequenceProcessor()==null)
				this.setPosTagSequenceProcessor(config.getPosTagSequenceProcessor());
			if (this.getParseConfigurationProcessor()==null)
				this.setParseConfigurationProcessor(config.getParseConfigurationProcessor());
			
			switch (config.getCommand()) {
			case analyse:
				MONITOR.startTask("analyse");
				try {
					this.analyse(config);
				} finally {
					MONITOR.endTask("analyse");
				}
				break;
			case process:
				MONITOR.startTask("process");
				try {
					switch (config.getModule()) {
					case Tokeniser:
						if (this.getTokenSequenceProcessor()==null)
							throw new TalismaneException("Cannot process tokeniser output without a token sequence processor!");
						
						while (config.getTokenCorpusReader().hasNextTokenSequence()) {
							TokenSequence tokenSequence = config.getTokenCorpusReader().nextTokenSequence();
							MONITOR.startTask("processTokenSequence");
							try {
								this.getTokenSequenceProcessor().onNextTokenSequence(tokenSequence, this.getWriter());
							} finally {
								MONITOR.endTask("processTokenSequence");
							}
						}
						break;
					case PosTagger:
						if (this.getPosTagSequenceProcessor()==null)
							throw new TalismaneException("Cannot process pos-tagger output without a pos-tag sequence processor!");
						
						try {
							while (config.getPosTagCorpusReader().hasNextPosTagSequence()) {
								PosTagSequence posTagSequence = config.getPosTagCorpusReader().nextPosTagSequence();
								MONITOR.startTask("processPosTagSequence");
								try {
									this.getPosTagSequenceProcessor().onNextPosTagSequence(posTagSequence, this.getWriter());
								} finally {
									MONITOR.endTask("processPosTagSequence");
								}
							}
						} finally {
							this.getPosTagSequenceProcessor().onCompleteAnalysis();
						}
						break;
					case Parser:
						if (this.getParseConfigurationProcessor()==null)
							throw new TalismaneException("Cannot process parser output without a parse configuration processor!");
						try {
							while (config.getParserCorpusReader().hasNextConfiguration()) {
								ParseConfiguration parseConfiguration = config.getParserCorpusReader().nextConfiguration();
								MONITOR.startTask("processParseConfiguration");
								try {
									this.getParseConfigurationProcessor().onNextParseConfiguration(parseConfiguration, this.getWriter());
								} finally {
									MONITOR.endTask("processParseConfiguration");
								}
							}
						} finally {
							this.getParseConfigurationProcessor().onCompleteParse();
						}
						break;
					default:
						throw new TalismaneException("Command 'process' does not yet support module: " + config.getModule());
					}
				} finally {
					MONITOR.endTask("process");
				}
				break;
			case evaluate:
				MONITOR.startTask("evaluate");
				try {
					switch (config.getModule()) {
					case Tokeniser:
						TokeniserEvaluator tokeniserEvaluator = config.getTokeniserEvaluator();
						MONITOR.startTask("tokeniserEvaluate");
						try {
							tokeniserEvaluator.evaluate(config.getTokenCorpusReader());
						} finally {
							MONITOR.endTask("tokeniserEvaluate");
						}
						break;
					case PosTagger:
						PosTaggerEvaluator posTaggerEvaluator = config.getPosTaggerEvaluator();
						MONITOR.startTask("posTaggerEvaluate");
						try {
							posTaggerEvaluator.evaluate(config.getPosTagCorpusReader());
						} finally {
							MONITOR.endTask("posTaggerEvaluate");
						}
						break;
					case Parser:
						ParserEvaluator parserEvaluator = config.getParserEvaluator();
						MONITOR.startTask("parserEvaluate");
						try {
							parserEvaluator.evaluate(config.getParserCorpusReader());
						} finally {
							MONITOR.endTask("parserEvaluate");
						}
						break;
					default:
						throw new TalismaneException("Command 'evaluate' does not yet support module: " + config.getModule());
					}
				} finally {
					MONITOR.endTask("evaluate");
				}
				break;
			case compare:
				MONITOR.startTask("compare");
				try {
					switch (config.getModule()) {
					case Tokeniser:
						TokenComparator tokenComparator = config.getTokenComparator();
						tokenComparator.compare();
						break;
					case PosTagger:
						PosTagComparator posTagComparator = config.getPosTagComparator();
						posTagComparator.evaluate(config.getPosTagCorpusReader(), config.getPosTagEvaluationCorpusReader());
						break;
					case Parser:
						ParseComparator parseComparator = config.getParseComparator();
						parseComparator.evaluate(config.getParserCorpusReader(), config.getParserEvaluationCorpusReader());
						break;
					default:
						throw new TalismaneException("Command 'compare' does not yet support module: " + config.getModule());
					}
				} finally {
					MONITOR.endTask("compare");
				}
				break;
			case train:
				MONITOR.startTask("train");
				try {
					switch (config.getModule()) {
					case SentenceDetector: {
						File modelFile = new File(config.getSentenceModelFilePath());
						File modelDir = modelFile.getParentFile();
						modelDir.mkdirs();
						
						ClassificationModelTrainer<SentenceDetectorOutcome> trainer = machineLearningService.getClassificationModelTrainer(config.getAlgorithm(), config.getTrainParameters());
	
						DecisionFactory<SentenceDetectorOutcome> decisionFactory = this.getSentenceDetectorService().getDecisionFactory();
						ClassificationModel<SentenceDetectorOutcome> sentenceModel = trainer.trainModel(config.getClassificationEventStream(), decisionFactory, config.getDescriptors());
						if (config.getExternalResourceFinder()!=null)
							sentenceModel.setExternalResources(config.getExternalResourceFinder().getExternalResources());
						sentenceModel.persist(modelFile);
						break;
					}
					case Tokeniser: {
						File modelFile = new File(config.getTokeniserModelFilePath());
						File modelDir = modelFile.getParentFile();
						modelDir.mkdirs();
						ClassificationModelTrainer<TokeniserOutcome> trainer = this.getMachineLearningService().getClassificationModelTrainer(config.getAlgorithm(), config.getTrainParameters());
						
						DecisionFactory<TokeniserOutcome> decisionFactory = this.getTokeniserService().getDecisionFactory();
						ClassificationModel<TokeniserOutcome> tokeniserModel = trainer.trainModel(config.getClassificationEventStream(), decisionFactory, config.getDescriptors());
						if (config.getExternalResourceFinder()!=null)
							tokeniserModel.setExternalResources(config.getExternalResourceFinder().getExternalResources());
						tokeniserModel.getModelAttributes().put(PatternTokeniserType.class.getSimpleName(), config.getPatternTokeniserType().toString());
						tokeniserModel.persist(modelFile);
						break;
					}
					case PosTagger: {
						File modelFile = new File(config.getPosTaggerModelFilePath());
						File modelDir = modelFile.getParentFile();
						modelDir.mkdirs();
						if (config.getPerceptronObservationPoints()==null) {
							ClassificationModelTrainer<PosTag> trainer = machineLearningService.getClassificationModelTrainer(config.getAlgorithm(), config.getTrainParameters());
	
							ClassificationModel<PosTag> posTaggerModel = trainer.trainModel(config.getClassificationEventStream(), TalismaneSession.getPosTagSet(), config.getDescriptors());			
							if (config.getExternalResourceFinder()!=null)
								posTaggerModel.setExternalResources(config.getExternalResourceFinder().getExternalResources());
							posTaggerModel.persist(modelFile);
						} else {
							if (config.getAlgorithm()!=MachineLearningAlgorithm.Perceptron)
								throw new RuntimeException("Incompatible argument perceptronTrainingInterval with algorithm " + config.getAlgorithm());
							PerceptronServiceLocator perceptronServiceLocator = PerceptronServiceLocator.getInstance();
							PerceptronService perceptronService = perceptronServiceLocator.getPerceptronService();
							PerceptronClassificationModelTrainer<PosTag> trainer = perceptronService.getPerceptronModelTrainer();
							trainer.setParameters(config.getTrainParameters());
							
							
							String modelName = modelFile.getName().substring(0, modelFile.getName().lastIndexOf('.'));
							PerceptronModelTrainerObserver<PosTag> observer = new PosTaggerPerceptronModelPersister(modelDir, modelName, config.getExternalResourceFinder());
							trainer.trainModelsWithObserver(config.getClassificationEventStream(), TalismaneSession.getPosTagSet(), config.getDescriptors(), observer, config.getPerceptronObservationPoints());
						}
						break;
					}
					case Parser: {
						MachineLearningModel parserModel = null;
						File modelFile = new File(config.getParserModelFilePath());
						File modelDir = modelFile.getParentFile();
						modelDir.mkdirs();
	
						boolean needToPersist = true;
						if (config.getAlgorithm().getModelType()==MachineLearningModelType.Classification) {
							if (config.getPerceptronObservationPoints()==null) {
								ClassificationModelTrainer<Transition> trainer = this.getMachineLearningService().getClassificationModelTrainer(config.getAlgorithm(), config.getTrainParameters());
			
								parserModel = trainer.trainModel(config.getClassificationEventStream(), TalismaneSession.getTransitionSystem(), config.getDescriptors());
							} else {
								if (config.getAlgorithm()!=MachineLearningAlgorithm.Perceptron)
									throw new RuntimeException("Incompatible argument perceptronTrainingInterval with algorithm " + config.getAlgorithm());
								PerceptronServiceLocator perceptronServiceLocator = PerceptronServiceLocator.getInstance();
								PerceptronService perceptronService = perceptronServiceLocator.getPerceptronService();
								PerceptronClassificationModelTrainer<Transition> trainer = perceptronService.getPerceptronModelTrainer();
								trainer.setParameters(config.getTrainParameters());
								
								String modelName = modelFile.getName().substring(0, modelFile.getName().lastIndexOf('.'));
	
								PerceptronModelTrainerObserver<Transition> observer = new ParserPerceptronModelPersister(modelDir, modelName, config.getExternalResourceFinder());
								trainer.trainModelsWithObserver(config.getClassificationEventStream(), TalismaneSession.getTransitionSystem(), config.getDescriptors(), observer, config.getPerceptronObservationPoints());
								needToPersist = false;
							}
						} else if (config.getAlgorithm().getModelType()==MachineLearningModelType.Ranking) {
							RankingEventStream<PosTagSequence> parseEventStream = parserService.getGlobalParseEventStream(config.getParserCorpusReader(), config.getParserFeatures());
							RankingModelTrainer<PosTagSequence> trainer = machineLearningService.getRankingModelTrainer(config.getAlgorithm(), config.getTrainParameters());
							Ranker<PosTagSequence> ranker = parserService.getRanker(config.getParsingConstrainer(), config.getParserFeatures(), config.getBeamWidth());
							parserModel = trainer.trainModel(parseEventStream, ranker, config.getDescriptors());
							parserModel.addDependency(ParsingConstrainer.class.getSimpleName(), config.getParsingConstrainer());
						} else {
							throw new TalismaneException("Unknown model type: " + config.getAlgorithm().getModelType());
						}
						
						if (needToPersist) {
							if (config.getExternalResourceFinder()!=null)
								parserModel.setExternalResources(config.getExternalResourceFinder().getExternalResources());
							parserModel.persist(modelFile);
						}
						break;
					}
					default:
						throw new TalismaneException("Command 'train' does not yet support module: " + config.getModule());
					}
				} finally {
					MONITOR.endTask("train");
				}
				break;
			default:
				throw new TalismaneException("Unsupported command: " + config.getCommand());
			} // which command?
		} catch (Exception e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		} finally {
			PerformanceMonitor.end();
			
			if (PerformanceMonitor.isActivated()) {
				try {
					Writer csvFileWriter = null;
					File csvFile  =new File(config.getOutDir(), config.getBaseName() + ".performance.csv");
					
					csvFile.delete();
					csvFile.createNewFile();
					csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
					PerformanceMonitor.writePerformanceCSV(csvFileWriter);
					csvFileWriter.flush();
					csvFileWriter.close();
				} catch (Exception e) {
					LogUtils.logError(LOG, e);
				}
			}
			
			long endTime = new Date().getTime();
			long totalTime = endTime - startTime;
			LOG.info("Total time: " + totalTime);
			
			if (config.isLogStats()) {
				try {
					Writer csvFileWriter = null;
					File csvFile = new File(config.getOutDir(), config.getBaseName() + ".stats.csv");
					
					csvFile.delete();
					csvFile.createNewFile();
					csvFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile, false),"UTF8"));
					csvFileWriter.write(CSV.format("total time")
							+ CSV.format(totalTime) + "\n");
					csvFileWriter.flush();
					csvFileWriter.close();
				} catch (Exception e) {
					LogUtils.logError(LOG, e);
				}
			}
		}
	}

	public void analyse(TalismaneConfig config) {
		try {
			if (config.needsSentenceDetector()) {
				if (config.getSentenceDetector()==null) {
					throw new TalismaneException("Sentence detector not provided.");
				}
			}
			if (config.needsTokeniser()) {
				if (config.getTokeniser()==null) {
					throw new TalismaneException("Tokeniser not provided.");
				}
			}
			if (config.needsPosTagger()) {
				if (config.getPosTagger()==null) {
					throw new TalismaneException("Pos-tagger not provided.");
				}
			}
			if (config.needsParser()) {
				if (config.getParser()==null) {
					throw new TalismaneException("Parser not provided.");
				}
			}
			
			if (config.getEndModule().equals(Module.SentenceDetector)) {
				if (this.getSentenceProcessor()==null) {
					throw new TalismaneException("No sentence processor provided with sentence detector end module, cannot generate output.");
				}
			}
			if (config.getEndModule().equals(Module.Tokeniser)) {
				if (this.getTokenSequenceProcessor()==null) {
					throw new TalismaneException("No token sequence processor provided with tokeniser end module, cannot generate output.");
				}
			}
			if (config.getEndModule().equals(Module.PosTagger)) {
				if (this.getPosTagSequenceProcessor()==null) {
					throw new TalismaneException("No postag sequence processor provided with pos-tagger end module, cannot generate output.");
				}
			}
			if (config.getEndModule().equals(Module.Parser)) {
				if (this.getParseConfigurationProcessor()==null) {
					throw new TalismaneException("No parse configuration processor provided with parser end module, cannot generate output.");
				}
			}
			
			LinkedList<String> textSegments = new LinkedList<String>();
			LinkedList<Sentence> sentences = new LinkedList<Sentence>();
			TokenSequence tokenSequence = null;
			PosTagSequence posTagSequence = null;
			
			RollingSentenceProcessor rollingSentenceProcessor = this.getFilterService().getRollingSentenceProcessor(config.getFileName(), config.isProcessByDefault());
			Sentence leftover = null;
			if (config.getStartModule().equals(Module.SentenceDetector)||config.getStartModule().equals(Module.Tokeniser)) {
				// prime the sentence detector with two text segments, to ensure everything gets processed
				textSegments.addLast("");
				textSegments.addLast("");
			}
			
		    StringBuilder stringBuilder = new StringBuilder();
		    boolean finished = false;
		    int sentenceCount = 0;
		    
			String prevProcessedText = "";
			String processedText = "";
			String nextProcessedText = "";
			SentenceHolder prevSentenceHolder = null;
			
			int endBlockCharacterCount = 0;
			
		    while (!finished) {
		    	if (config.getStartModule().equals(Module.SentenceDetector)||config.getStartModule().equals(Module.Tokeniser)) {
		    		// Note SentenceDetector and Tokeniser start modules treated identically,
		    		// except that for SentenceDetector we apply a probabilistic sentence detector
		    		// whereas for Tokeniser we assume all sentence breaks are marked by filters
		    		
				    // read characters from the reader, one at a time
			    	char c;
			    	int r = -1;
			    	try {
			    		r = this.getReader().read();
			    	} catch (IOException e) {
			    		LogUtils.logError(LOG, e);
			    	}
			    	
			    	if (r==-1) {
			    		finished = true;
			    		c = '\n';
			    	} else {
		    			c = (char) r;
			    	}

			    	// Jump out if we have 3 consecutive end-block characters.
			    	if (c==config.getEndBlockCharacter()) {
			    		endBlockCharacterCount++;
			    		if (endBlockCharacterCount==3) {
			    			LOG.info("Three consecutive end-block characters. Exiting.");
			    			finished = true;
			    		}
			    	} else {
			    		endBlockCharacterCount = 0;
			    	}
	    			
	    			// have sentence detector
		    		if (finished || (Character.isWhitespace(c) && stringBuilder.length()>config.getBlockSize()) || c==config.getEndBlockCharacter()) {
	    				if (c==config.getEndBlockCharacter())
	    					stringBuilder.append(c);
		    			if (stringBuilder.length()>0) {
			    			String textSegment = stringBuilder.toString();
			    			stringBuilder = new StringBuilder();
			    			
		    				textSegments.add(textSegment);
		    			} // is the current block > 0 characters?
	    				if (c==config.getEndBlockCharacter()) {
	    					textSegments.addLast("");
	    				}
		    		} // is there a next block available?
		    		
		    		if (finished) {
		    			if (stringBuilder.length()>0) {
		    				textSegments.addLast(stringBuilder.toString());
		    				stringBuilder = new StringBuilder();
		    			}
						textSegments.addLast("");
						textSegments.addLast("");
						textSegments.addLast("");
		    		}
		    		
		    		if (c!=config.getEndBlockCharacter())
		    			stringBuilder.append(c);
		    		
					while (textSegments.size()>=3) {
						String prevText = textSegments.removeFirst();
						String text = textSegments.removeFirst();
						String nextText = textSegments.removeFirst();
						if (LOG.isTraceEnabled()) {
							LOG.trace("prevText: " + prevText);
							LOG.trace("text: " + text);
							LOG.trace("nextText: " + nextText);							
						}
						
						Set<TextMarker> textMarkers = new TreeSet<TextMarker>();
						for (TextMarkerFilter textMarkerFilter : config.getTextMarkerFilters()) {
							Set<TextMarker> result = textMarkerFilter.apply(prevText, text, nextText);
							textMarkers.addAll(result);
						}
						
						// push the text segments back onto the beginning of Deque
						textSegments.addFirst(nextText);
						textSegments.addFirst(text);
						
						SentenceHolder sentenceHolder = rollingSentenceProcessor.addNextSegment(text, textMarkers);
						prevProcessedText = processedText;
						processedText = nextProcessedText;
						nextProcessedText = sentenceHolder.getText();
						
						if (LOG.isTraceEnabled()) {
							LOG.trace("prevProcessedText: " + prevProcessedText);
							LOG.trace("processedText: " + processedText);
							LOG.trace("nextProcessedText: " + nextProcessedText);							
						}

					    boolean reallyFinished = finished && textSegments.size()==3;

						if (prevSentenceHolder!=null) {
							if (config.getStartModule().equals(Module.SentenceDetector)) {
								List<Integer> sentenceBreaks = config.getSentenceDetector().detectSentences(prevProcessedText, processedText, nextProcessedText);
								for (int sentenceBreak : sentenceBreaks) {
									prevSentenceHolder.addSentenceBoundary(sentenceBreak);
								}
							}
							
							List<Sentence> theSentences = prevSentenceHolder.getDetectedSentences(leftover);
							leftover = null;
							for (Sentence sentence : theSentences) {
								if (sentence.isComplete()||reallyFinished) {
									sentences.add(sentence);
									sentenceCount++;
								} else {
									LOG.debug("Setting leftover to: " + sentence.getText());
									leftover = sentence;
								}
							}
							if (config.getMaxSentenceCount()>0 && sentenceCount>=config.getMaxSentenceCount()) {
								finished = true;
							}
						}
						prevSentenceHolder = sentenceHolder;
					} // we have at least 3 text segments (should always be the case once we get started)
		    	} else if (config.getStartModule().equals(Module.PosTagger)) {
	    			if (config.getTokenCorpusReader().hasNextTokenSequence()) {
	    				tokenSequence = config.getTokenCorpusReader().nextTokenSequence();
	    			} else {
	    				tokenSequence = null;
	    				finished = true;
	    			}
	    		} else if (config.getStartModule().equals(Module.Parser)) {
	    			if (config.getPosTagCorpusReader().hasNextPosTagSequence()) {
	    				posTagSequence = config.getPosTagCorpusReader().nextPosTagSequence();
	    			} else {
	    				posTagSequence = null;
	    				finished = true;
	    			}
	    		} // which start module?
	    		
	    		boolean needToProcess = false;
	    		if (config.getStartModule().equals(Module.SentenceDetector)||config.getStartModule().equals(Module.Tokeniser))
	    			needToProcess = !sentences.isEmpty();
	    		else if (config.getStartModule().equals(Module.PosTagger))
	    			needToProcess = tokenSequence!=null;
	    		else if (config.getStartModule().equals(Module.Parser))
	    			needToProcess = posTagSequence!=null;
	    		
	    		while (needToProcess) {
	    			Sentence sentence = null;
	    			if (config.getStartModule().compareTo(Module.Tokeniser)<=0 && config.getEndModule().compareTo(Module.SentenceDetector)>=0) {
		    			sentence = sentences.poll();
		    			LOG.debug("Sentence: " + sentence);
		    			if (this.getSentenceProcessor()!=null)
		    				this.getSentenceProcessor().onNextSentence(sentence.getText(), this.getWriter());
	    			} // need to read next sentence
	    			
	    			List<TokenSequence> tokenSequences = null;
	    			if (config.needsTokeniser()) {
	    				tokenSequences = config.getTokeniser().tokenise(sentence);
    					tokenSequence = tokenSequences.get(0);
	    				
    					if (this.getTokenSequenceProcessor()!=null) {
    						this.getTokenSequenceProcessor().onNextTokenSequence(tokenSequence, this.getWriter());
    					}
	    			} // need to tokenise ?
	    			
	    			List<PosTagSequence> posTagSequences = null;
 	    			if (config.needsPosTagger()) {
    					posTagSequence = null;
    					if (tokenSequences==null||!config.isPropagateTokeniserBeam()) {
    						tokenSequences = new ArrayList<TokenSequence>();
    						tokenSequences.add(tokenSequence);
    					}

	    				if (config.getPosTagger() instanceof NonDeterministicPosTagger) {
	    					NonDeterministicPosTagger nonDeterministicPosTagger = (NonDeterministicPosTagger) config.getPosTagger();
	    					posTagSequences = nonDeterministicPosTagger.tagSentence(tokenSequences);
	    					posTagSequence = posTagSequences.get(0);
	    				} else {
	    					posTagSequence = config.getPosTagger().tagSentence(tokenSequence);
	    				}
	    				
	    				if (posTagSequenceProcessor!=null) {
	    					posTagSequenceProcessor.onNextPosTagSequence(posTagSequence, this.getWriter());
	    				}

	    				tokenSequence = null;
 	    			} // need to postag
 	    			
	    			if (config.needsParser()) {
    					if (posTagSequences==null||!config.isPropagatePosTaggerBeam()) {
    						posTagSequences = new ArrayList<PosTagSequence>();
    						posTagSequences.add(posTagSequence);
    					}
    					
    					ParseConfiguration parseConfiguration = null;
    					List<ParseConfiguration> parseConfigurations = null;
    					try {
	    					if (config.getParser() instanceof NonDeterministicParser) {
	    						NonDeterministicParser nonDeterministicParser = (NonDeterministicParser) config.getParser();
		    					parseConfigurations = nonDeterministicParser.parseSentence(posTagSequences);
		    					parseConfiguration = parseConfigurations.get(0);
	    					} else {
	    						parseConfiguration = config.getParser().parseSentence(posTagSequence);
	    					}
	    					
	    					if (this.getParseConfigurationProcessor()!=null) {
	    						this.getParseConfigurationProcessor().onNextParseConfiguration(parseConfiguration, this.getWriter());
	    					}
    					} catch (Exception e) {
    						LOG.error(e);
    						if (stopOnError)
    							throw new RuntimeException(e);
    					}
    					posTagSequence = null;
    				} // need to parse
	    			
		    		if (config.getStartModule().equals(Module.SentenceDetector)||config.getStartModule().equals(Module.Tokeniser))
		    			needToProcess = !sentences.isEmpty();
		    		else if (config.getStartModule().equals(Module.PosTagger))
		    			needToProcess = tokenSequence!=null;
		    		else if (config.getStartModule().equals(Module.Parser))
		    			needToProcess = posTagSequence!=null;
	    		} // next sentence
			} // next character
		} finally {
			if (this.getParseConfigurationProcessor()!=null) {
				this.getParseConfigurationProcessor().onCompleteParse();
			}
			
			try {
				this.getReader().close();
				this.getWriter().flush();
				this.getWriter().close();
			} catch (IOException ioe2) {
				LOG.error(ioe2);
				throw new RuntimeException(ioe2);
			}

		}
	}
	
	public void train(TalismaneConfig config) {
		switch (config.getModule()) {
		case SentenceDetector:
//			ClassificationModelTrainer<SentenceDetectorOutcome> trainer = machineLearningService.getClassificationModelTrainer(algorithm, trainParameters);
//			
//			DecisionFactory<SentenceDetectorOutcome> decisionFactory = sentenceDetectorService.getDecisionFactory();
//			ClassificationModel<SentenceDetectorOutcome> sentenceModel = trainer.trainModel(tokeniserEventStream, decisionFactory, featureDescriptors);
//			if (externalResourceFinder!=null)
//				sentenceModel.setExternalResources(externalResourceFinder.getExternalResources());
//			sentenceModel.persist(sentenceModelFile);

			break;
		case Tokeniser:
			break;
		case PosTagger:
			break;
		case Parser:
			break;
		}
	}

	@Override
	public SentenceProcessor getSentenceProcessor() {
		return sentenceProcessor;
	}

	@Override
	public void setSentenceProcessor(SentenceProcessor sentenceProcessor) {
		this.sentenceProcessor = sentenceProcessor;
	}

	@Override
	public TokenSequenceProcessor getTokenSequenceProcessor() {
		return tokenSequenceProcessor;
	}

	@Override
	public void setTokenSequenceProcessor(
			TokenSequenceProcessor tokenSequenceProcessor) {
		this.tokenSequenceProcessor = tokenSequenceProcessor;
	}

	@Override
	public PosTagSequenceProcessor getPosTagSequenceProcessor() {
		return posTagSequenceProcessor;
	}

	@Override
	public void setPosTagSequenceProcessor(
			PosTagSequenceProcessor posTagSequenceProcessor) {
		this.posTagSequenceProcessor = posTagSequenceProcessor;
	}

	@Override
	public ParseConfigurationProcessor getParseConfigurationProcessor() {
		return parseConfigurationProcessor;
	}

	@Override
	public void setParseConfigurationProcessor(
			ParseConfigurationProcessor parseConfigurationProcessor) {
		this.parseConfigurationProcessor = parseConfigurationProcessor;
	}

	public TokeniserService getTokeniserService() {
		return tokeniserService;
	}

	public void setTokeniserService(TokeniserService tokeniserService) {
		this.tokeniserService = tokeniserService;
	}

	public PosTaggerService getPosTaggerService() {
		return posTaggerService;
	}

	public void setPosTaggerService(PosTaggerService posTaggerService) {
		this.posTaggerService = posTaggerService;
	}

	public ParserService getParserService() {
		return parserService;
	}

	public void setParserService(ParserService parserService) {
		this.parserService = parserService;
	}

	public FilterService getFilterService() {
		return filterService;
	}

	public void setFilterService(FilterService filterService) {
		this.filterService = filterService;
	}


	public MachineLearningService getMachineLearningService() {
		return machineLearningService;
	}

	public void setMachineLearningService(
			MachineLearningService machineLearningService) {
		this.machineLearningService = machineLearningService;
	}

	public SentenceDetectorService getSentenceDetectorService() {
		return sentenceDetectorService;
	}

	public void setSentenceDetectorService(
			SentenceDetectorService sentenceDetectorService) {
		this.sentenceDetectorService = sentenceDetectorService;
	}

	public boolean isStopOnError() {
		return stopOnError;
	}

	public void setStopOnError(boolean stopOnError) {
		this.stopOnError = stopOnError;
	}

	public Reader getReader() {
		if (this.reader==null)
			this.reader = config.getReader();
		return reader;
	}

	public void setReader(Reader reader) {
		this.reader = reader;
	}

	public Writer getWriter() {
		if (this.writer==null)
			this.writer = config.getWriter();
		return writer;
	}

	public void setWriter(Writer writer) {
		this.writer = writer;
	}
	
	  private static final class PosTaggerPerceptronModelPersister implements PerceptronModelTrainerObserver<PosTag> {
	    	File outDir;
	    	ExternalResourceFinder externalResourceFinder;
	    	String baseName;
	    	
			public PosTaggerPerceptronModelPersister(File outDir, String baseName,
					ExternalResourceFinder externalResourceFinder) {
				super();
				this.outDir = outDir;
				this.baseName = baseName;
				this.externalResourceFinder = externalResourceFinder;
			}

			@Override
			public void onNextModel(ClassificationModel<PosTag> model,
					int iterations) {
				this.outDir.mkdirs();
				File posTaggerModelFile = new File(outDir, baseName + "_i" + iterations + ".zip");
				if (externalResourceFinder!=null)
					model.setExternalResources(externalResourceFinder.getExternalResources());
				LOG.info("Writing model " + posTaggerModelFile.getName());
				model.persist(posTaggerModelFile);
			}
	    }
	  
    private static final class ParserPerceptronModelPersister implements PerceptronModelTrainerObserver<Transition> {
    	File outDir;
    	ExternalResourceFinder externalResourceFinder;
    	String baseName;
    	
		public ParserPerceptronModelPersister(File outDir, String baseName,
				ExternalResourceFinder externalResourceFinder) {
			super();
			this.outDir = outDir;
			this.baseName = baseName;
			this.externalResourceFinder = externalResourceFinder;
		}

		@Override
		public void onNextModel(ClassificationModel<Transition> model,
				int iterations) {
			this.outDir.mkdirs();
			File parserModelFile = new File(outDir, baseName + "_i" + iterations + ".zip");
			if (externalResourceFinder!=null)
				model.setExternalResources(externalResourceFinder.getExternalResources());
			LOG.info("Writing model " + parserModelFile.getName());
			model.persist(parserModelFile);
		}
    }
}
