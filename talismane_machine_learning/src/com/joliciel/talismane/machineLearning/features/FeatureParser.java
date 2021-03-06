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
package com.joliciel.talismane.machineLearning.features;

import java.util.List;

/**
 * Parses a function descriptor into an actual feature.
 * @author Assaf Urieli
 *
 */
public interface FeatureParser<T> {
	/**
	 * Parse a function descriptor and return one or more features
	 * corresponding to it.
	 * @param descriptor
	 * @return
	 */
	public List<Feature<T, ?>> parse(FunctionDescriptor descriptor);
	
	/**
	 * A feature dynamiser: if provided, features will by dynamised before being returned.
	 * @return
	 */
	public Dynamiser<T> getDynamiser();
	public void setDynamiser(Dynamiser<T> dynamiser);
}
