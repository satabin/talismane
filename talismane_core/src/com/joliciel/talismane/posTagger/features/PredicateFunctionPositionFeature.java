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
package com.joliciel.talismane.posTagger.features;

import com.joliciel.talismane.lexicon.LexicalEntry;
import com.joliciel.talismane.lexicon.PredicateArgument;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * For this pos-tagged token's main lexical entry, assuming the function name provided is in the list,
 * what is its index?
 * If the function name provided is not in the list, returns null.
 * @author Assaf Urieli
 *
 */
public final class PredicateFunctionPositionFeature<T> extends AbstractPosTaggedTokenFeature<T,Integer> implements IntegerFeature<T> {
	public StringFeature<T> functionNameFeature;
	
	public PredicateFunctionPositionFeature(PosTaggedTokenAddressFunction<T> addressFunction, StringFeature<T> functionNameFeature) {
		super(addressFunction);
		this.functionNameFeature = functionNameFeature;
		this.setName(super.getName() + "(" + functionNameFeature.getName() + ")");
		this.setAddressFunction(addressFunction);
	}

	@Override
	public FeatureResult<Integer> checkInternal(T context, RuntimeEnvironment env) {
		PosTaggedTokenWrapper innerWrapper = this.getToken(context, env);
		if (innerWrapper==null)
			return null;
		PosTaggedToken posTaggedToken = innerWrapper.getPosTaggedToken();
		if (posTaggedToken==null)
			return null;
		
		
		FeatureResult<Integer> featureResult = null;
		
		FeatureResult<String> functionNameResult = this.functionNameFeature.check(context, env);
		if (functionNameResult!=null) {
			String functionName = functionNameResult.getOutcome();
			LexicalEntry lexicalEntry = null;
			if (posTaggedToken.getLexicalEntries().size()>0)
				lexicalEntry = posTaggedToken.getLexicalEntries().iterator().next();
			if (lexicalEntry!=null) {
				PredicateArgument argument = lexicalEntry.getPredicateArgument(functionName);
				if (argument!=null)
					featureResult = this.generateResult(argument.getIndex());
			}
		}
		return featureResult;
	}
	
	
}
