/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.pipelines.parsers.vocabulary;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.Terms;
import org.gbif.vocabulary.lookup.VocabularyLookup;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import lombok.Builder;
import lombok.Singular;

@SuppressWarnings("FallThrough")
@Builder
public class VocabularyService implements Serializable {

  @Singular private final Map<String, VocabularyLookup> vocabularyLookups;

  public Optional<VocabularyLookup> get(Term term) {
    if (!Terms.getVocabularyBackedTerms().contains(term)) {
      throw new IllegalArgumentException("Vocabulary-backed term not supported: " + term);
    }

    if (term instanceof DwcTerm
        && ((DwcTerm) term).getGroup().equals(DwcTerm.GROUP_GEOLOGICALCONTEXT)) {
      return Optional.ofNullable(vocabularyLookups.get(DwcTerm.GROUP_GEOLOGICALCONTEXT));
    }

    return Optional.ofNullable(vocabularyLookups.get(term.qualifiedName()));
  }

  public void close() {
    vocabularyLookups.values().forEach(VocabularyLookup::close);
  }
}
