package com.exadel.frs.core.trainservice.service;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.exadel.frs.core.trainservice.domain.Face;
import com.exadel.frs.core.trainservice.domain.Face.Embedding;
import com.exadel.frs.core.trainservice.enums.RetrainOption;
import com.exadel.frs.core.trainservice.repository.FaceClassifierStorage;
import com.exadel.frs.core.trainservice.repository.FacesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FaceService {
    @Autowired
    private final FaceClassifierStorage storage;
    private final FacesRepository facesRepository;

    public Map<String, List<List<Double>>> findAllFaceEmbeddings() {
        var all = facesRepository.findAll();

        return all.stream()
                .collect(toMap(Face::getFaceName,
                        face -> face.getEmbeddings()
                                .stream()
                                .map(Embedding::getEmbedding)
                                .collect(toList()), (l1, l2) -> Stream
                                .concat(l1.stream(), l2.stream())
                                .collect(toList())
                ));
    }

    public Map<String, List<List<Double>>> findAllFaceEmbeddingsByAppKey(final String appKey) {
        var all = facesRepository.findByApiKey(appKey);

        return all.stream()
                .collect(toMap(Face::getFaceName,
                        face -> face.getEmbeddings()
                                .stream()
                                .map(Embedding::getEmbedding)
                                .collect(toList()), (l1, l2) -> Stream
                                .concat(l1.stream(), l2.stream())
                                .collect(toList())
                ));
    }

    public Map<String, List<String>> findAllFaceNamesByApiKey(final String appKey) {
        var faces = facesRepository.findByApiKey(appKey);
        var faceNames = faces.stream()
                .map(Face::getFaceName)
                .collect(toList());

        var response = new HashMap<String, List<String>>();
        response.put("names", faceNames);

        return response;
    }

    public void deleteFaceByNameAndTrainModelIfRequired(String faceName, final String appKey, final String modelGuid, String retrain) {
        facesRepository.deleteByApiKeyAndFaceName(appKey, faceName);
        handleModelTraining(appKey, modelGuid, retrain);
    }

    private void handleModelTraining(final String appKey, final String modelGuid, String retrain) {
        switch (RetrainOption.valueOf(retrain.toUpperCase())) {
            case YES:
                abortCurrentTrainingIfExists(appKey, modelGuid);
                beginNewTraining(appKey, modelGuid);
                break;
            case NO:
                break;
            default:
                abortCurrentTrainingIfExists(appKey, modelGuid);
        }
    }

    private void beginNewTraining(final String appKey, final String modelGuid) {
        storage.lock(appKey, modelGuid);
        storage.getFaceClassifier(appKey, modelGuid)
                .train(findAllFaceEmbeddingsByAppKey(appKey), appKey, modelGuid);
    }

    private void abortCurrentTrainingIfExists(final String appKey, final String modelGuid) {
        if (storage.isLocked(appKey, modelGuid)) {
            storage.unlock(appKey, modelGuid);
        }
    }
}
