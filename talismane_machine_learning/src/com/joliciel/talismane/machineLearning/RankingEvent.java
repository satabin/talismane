///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2013 Assaf Urieli
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

/**
 * A single classification event in a training or test corpus, combining the results of feature
 * tests and the correct classification.
 * @author Assaf Urieli
 *
 */
public interface RankingEvent<T> {
	/**
	 * The correct solution for this event's input.
	 * @return
	 */
	public RankingSolution getSolution();

	/**
	 * The input resulting in this event.
	 * @return
	 */
	public T getInput();

}