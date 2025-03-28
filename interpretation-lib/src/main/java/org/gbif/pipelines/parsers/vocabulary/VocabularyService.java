package org.gbif.pipelines.parsers.vocabulary;

import lombok.Builder;
import lombok.Singular;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.dwc.terms.Terms;
import org.gbif.vocabulary.lookup.VocabularyLookup;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

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
