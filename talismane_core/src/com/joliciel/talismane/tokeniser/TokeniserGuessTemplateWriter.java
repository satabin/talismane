///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
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
//////////////////////////////////////////////////////////////////////////////package com.joliciel.talismane.parser;
package com.joliciel.talismane.tokeniser;

import java.io.Reader;
import java.io.Writer;
import java.util.List;

import com.joliciel.talismane.output.FreemarkerTemplateWriter;

/**
 * Simply a wrapper for the FreemarkerTemplateWriter, writing the best guess
 * using a freemarker template.
 * @author Assaf Urieli
 *
 */
public class TokeniserGuessTemplateWriter implements TokenEvaluationObserver {
	FreemarkerTemplateWriter freemarkerTemplateWriter;
	Writer writer;
	
	public TokeniserGuessTemplateWriter(Writer writer, Reader templateReader) {
		freemarkerTemplateWriter = new FreemarkerTemplateWriter(templateReader);
		this.writer = writer;
	}

	@Override
	public void onEvaluationComplete() {
		freemarkerTemplateWriter.onCompleteParse();
	}

	@Override
	public void onNextTokenSequence(TokenSequence realSequence,
			List<TokenisedAtomicTokenSequence> guessedAtomicSequences) {
		freemarkerTemplateWriter.onNextTokenSequence(guessedAtomicSequences.get(0).inferTokenSequence(), writer);
	}


}
