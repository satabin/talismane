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
import com.joliciel.talismane.machineLearning.features.DynamicSourceCodeBuilder;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.machineLearning.features.StringFeature;
import com.joliciel.talismane.posTagger.PosTaggedToken;

/**
 * The "best" lemma of a given pos-tagged token as supplied by the lexicon.
 * @author Assaf Urieli
 *
 */
public final class LemmaFeature<T> extends AbstractPosTaggedTokenFeature<T,String> implements StringFeature<T> {
	public LemmaFeature(PosTaggedTokenAddressFunction<T> addressFunction) {
		super(addressFunction);
		this.setAddressFunction(addressFunction);
	}
	
	public LemmaFeature() {
		super(new ItsMeAddressFunction<T>());
	}

	@Override
	public FeatureResult<String> checkInternal(T context, RuntimeEnvironment env) {
		PosTaggedTokenWrapper innerWrapper = this.getToken(context, env);
		if (innerWrapper==null)
			return null;
		PosTaggedToken posTaggedToken = innerWrapper.getPosTaggedToken();
		if (posTaggedToken==null)
			return null;
		
		FeatureResult<String> featureResult = null;
		LexicalEntry lexicalEntry = posTaggedToken.getLexicalEntry();
		if (lexicalEntry!=null)
			featureResult = this.generateResult(lexicalEntry.getLemma());
		
		return featureResult;
	}
	
	@Override
	public boolean addDynamicSourceCode(
			DynamicSourceCodeBuilder<T> builder,
			String variableName) {
		String address = builder.addFeatureVariable(addressFunction, "address");
		builder.append("if (" + address + "!=null && " + address + ".getPosTaggedToken().getLexicalEntry()!=null) {" );
		builder.indent();
		builder.append(	variableName + " = " + address + ".getPosTaggedToken().getLexicalEntry().getLemma();");
		builder.outdent();
		builder.append("}");
		return true;
	}
}
