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
package com.joliciel.talismane.machineLearning;

import java.util.List;

import com.joliciel.talismane.machineLearning.features.FeatureResult;

class ClassificationEventImpl implements ClassificationEvent {
	private String classification;
	private List<FeatureResult<?>> featureResults;

	ClassificationEventImpl(List<FeatureResult<?>> featureResults,
			String classification) {
		super();
		this.classification = classification;
		this.featureResults = featureResults;
	}
	
	/**
	 * The result of testing the various features on this event.
	 * @return
	 */
	@Override
	public List<FeatureResult<?>> getFeatureResults() {
		return featureResults;
	}
	
	/**
	 * The correct classification of this event.
	 * @return
	 */
	@Override
	public String getClassification() {
		return classification;
	}
}
