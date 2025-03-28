package org.gbif.pipelines.transform;

import lombok.Builder;
import lombok.SneakyThrows;
import org.gbif.api.vocabulary.OccurrenceStatus;
import org.gbif.pipelines.interpretation.Interpretation;
import org.gbif.pipelines.interpretation.core.*;
import org.gbif.pipelines.models.BasicRecord;
import org.gbif.pipelines.models.ExtendedRecord;
import org.gbif.pipelines.parsers.vocabulary.VocabularyService;

import java.time.Instant;
import java.util.Optional;

import static org.gbif.api.model.pipelines.InterpretationType.RecordType.BASIC;

/**
 *
 */
public class BasicTransform {

  private final SerializableSupplier<KeyValueStore<String, OccurrenceStatus>>
      occStatusKvStoreSupplier;
  private final SerializableSupplier<VocabularyService> vocabularyServiceSupplier;

  @Builder.Default private boolean useDynamicPropertiesInterpretation = false;

  private KeyValueStore<String, OccurrenceStatus> occStatusKvStore;
  private VocabularyService vocabularyService;

  @Builder(buildMethodName = "create")
  private BasicTransform(
      boolean useDynamicPropertiesInterpretation,
      SerializableSupplier<VocabularyService> vocabularyServiceSupplier,
      SerializableSupplier<KeyValueStore<String, OccurrenceStatus>> occStatusKvStoreSupplier) {
    super(BasicRecord.class, BASIC, BasicTransform.class.getName(), BASIC_RECORDS_COUNT);
    this.useDynamicPropertiesInterpretation = useDynamicPropertiesInterpretation;
    this.occStatusKvStoreSupplier = occStatusKvStoreSupplier;
    this.vocabularyServiceSupplier = vocabularyServiceSupplier;
  }

  /** Maps {@link BasicRecord} to key value, where key is {@link BasicRecord#getId} */
  public MapElements<BasicRecord, KV<String, BasicRecord>> toKv() {
    return MapElements.into(new TypeDescriptor<KV<String, BasicRecord>>() {})
        .via((BasicRecord br) -> KV.of(br.getId(), br));
  }

  public BasicTransform counterFn(SerializableConsumer<String> counterFn) {
    setCounterFn(counterFn);
    return this;
  }

  /** Beam @Setup initializes resources */
  @Setup
  public void setup() {
    if (occStatusKvStore == null && occStatusKvStoreSupplier != null) {
      occStatusKvStore = occStatusKvStoreSupplier.get();
    }
    if (vocabularyService == null && vocabularyServiceSupplier != null) {
      vocabularyService = vocabularyServiceSupplier.get();
    }
  }


  public Optional<BasicRecord> convert(ExtendedRecord source) {

    Interpretation<ExtendedRecord>.Handler<BasicRecord> handler =
        Interpretation.from(source)
            .to(
                BasicRecord.newBuilder()
                    .setId(source.getId())
                    .setCreated(Instant.now().toEpochMilli())
                    .build())
            .when(er -> !er.getCoreTerms().isEmpty())
            .via(BasicInterpreter::interpretBasisOfRecord)
            .via(BasicInterpreter::interpretTypifiedName)
            .via(VocabularyInterpreter.interpretSex(vocabularyService))
            .via(VocabularyInterpreter.interpretTypeStatus(vocabularyService))
            .via(BasicInterpreter::interpretIndividualCount)
            .via((e, r) -> CoreInterpreter.interpretReferences(e, r, r::setReferences))
            .via(BasicInterpreter::interpretOrganismQuantity)
            .via(BasicInterpreter::interpretOrganismQuantityType)
            .via((e, r) -> CoreInterpreter.interpretSampleSizeUnit(e, r::setSampleSizeUnit))
            .via((e, r) -> CoreInterpreter.interpretSampleSizeValue(e, r::setSampleSizeValue))
            .via(BasicInterpreter::interpretRelativeOrganismQuantity)
            .via((e, r) -> CoreInterpreter.interpretLicense(e, r::setLicense))
            .via(BasicInterpreter::interpretIdentifiedByIds)
            .via(BasicInterpreter::interpretRecordedByIds)
            .via(BasicInterpreter.interpretOccurrenceStatus(occStatusKvStore))
            .via(VocabularyInterpreter.interpretEstablishmentMeans(vocabularyService))
            .via(VocabularyInterpreter.interpretLifeStage(vocabularyService))
            .via(VocabularyInterpreter.interpretPathway(vocabularyService))
            .via(VocabularyInterpreter.interpretDegreeOfEstablishment(vocabularyService))
            .via((e, r) -> CoreInterpreter.interpretDatasetID(e, r::setDatasetID))
            .via((e, r) -> CoreInterpreter.interpretDatasetName(e, r::setDatasetName))
            .via(BasicInterpreter::interpretOtherCatalogNumbers)
            .via(BasicInterpreter::interpretRecordedBy)
            .via(BasicInterpreter::interpretIdentifiedBy)
            .via(BasicInterpreter::interpretPreparations)
            .via((e, r) -> CoreInterpreter.interpretSamplingProtocol(e, r::setSamplingProtocol))
            .via(BasicInterpreter::interpretProjectId)
            .via(BasicInterpreter::interpretIsSequenced)
            .via(BasicInterpreter::interpretAssociatedSequences)
            // Geological context
            .via(GeologicalContextInterpreter.interpretChronostratigraphy(vocabularyService))
            .via(GeologicalContextInterpreter::interpretLowestBiostratigraphicZone)
            .via(GeologicalContextInterpreter::interpretHighestBiostratigraphicZone)
            .via(GeologicalContextInterpreter::interpretGroup)
            .via(GeologicalContextInterpreter::interpretFormation)
            .via(GeologicalContextInterpreter::interpretMember)
            .via(GeologicalContextInterpreter::interpretBed);

    if (useDynamicPropertiesInterpretation) {
      handler
          .via(DynamicPropertiesInterpreter.interpretSex(vocabularyService))
          .via(DynamicPropertiesInterpreter.interpretLifeStage(vocabularyService));
    }

    return handler.getOfNullable();
  }
}
