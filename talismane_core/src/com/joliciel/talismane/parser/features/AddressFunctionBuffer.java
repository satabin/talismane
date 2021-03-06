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
package com.joliciel.talismane.parser.features;

import java.util.Iterator;

import com.joliciel.talismane.machineLearning.features.DynamicSourceCodeBuilder;
import com.joliciel.talismane.machineLearning.features.FeatureResult;
import com.joliciel.talismane.machineLearning.features.IntegerFeature;
import com.joliciel.talismane.machineLearning.features.RuntimeEnvironment;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.features.PosTaggedTokenWrapper;

/**
 * Retrieves the nth item from the buffer.
 * @author Assaf Urieli
 *
 */
public final class AddressFunctionBuffer extends AbstractAddressFunction {
	private IntegerFeature<ParseConfigurationWrapper> indexFeature;
	
	public AddressFunctionBuffer(IntegerFeature<ParseConfigurationWrapper> indexFeature) {
		super();
		this.indexFeature = indexFeature;
		this.setName("Buffer[" + indexFeature.getName() + "]");
	}

	@Override
	public FeatureResult<PosTaggedTokenWrapper> checkInternal(ParseConfigurationWrapper wrapper, RuntimeEnvironment env) {
		ParseConfiguration configuration = wrapper.getParseConfiguration();
		PosTaggedToken resultToken = null;
		FeatureResult<Integer> indexResult = indexFeature.check(configuration, env);
		if (indexResult!=null) {
			int index = indexResult.getOutcome();
			
			Iterator<PosTaggedToken> bufferIterator = configuration.getBuffer().iterator();
			for (int i=0; i<=index; i++) {
				if (!bufferIterator.hasNext()) {
					resultToken = null;
					break;
				}
				resultToken = bufferIterator.next();
			}
		}
		FeatureResult<PosTaggedTokenWrapper> featureResult = null;
		if (resultToken!=null)
			featureResult = this.generateResult(resultToken);
		return featureResult;
	}


	@Override
	public boolean addDynamicSourceCode(
			DynamicSourceCodeBuilder<ParseConfigurationWrapper> builder,
			String variableName) {
		
		String indexName = builder.addFeatureVariable(indexFeature, "index");
		
		builder.append("if (" + indexName + "!=null) {");
		builder.indent();
		String bufferIterator = builder.getVarName("bufferIterator");
		builder.addImport(Iterator.class);
		builder.addImport(PosTaggedToken.class);
		
		builder.append(	"Iterator<PosTaggedToken> " + bufferIterator + " = context.getParseConfiguration().getBuffer().iterator();");
		
		builder.append(	"for (int i=0; i<=" + indexName + "; i++) {");
		builder.indent();
		builder.append(		"if (!" + bufferIterator + ".hasNext()) {");
		builder.indent();
		builder.append(			variableName + " = null;");
		builder.append(			"break;");
	    builder.outdent();
		builder.append(		"}");
		builder.append(	variableName + " = " + bufferIterator + ".next();");
		builder.outdent();
		builder.append("}");
		
		builder.outdent();
		builder.append("}");
		return true;
	}
}
