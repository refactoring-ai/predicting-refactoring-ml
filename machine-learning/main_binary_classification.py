from configs import DATASETS
from ml.models.builder import build_models, build_deep_models
from ml.pipelines.binary import BinaryClassificationPipeline, DeepLearningBinaryClassificationPipeline
from refactoring import build_refactorings
from utils.log import log_init, log_close, log

log_init()
log("ML4Refactoring: Binary classification")

refactorings = build_refactorings()
models = build_models()

pipeline = BinaryClassificationPipeline(models, refactorings, DATASETS)
pipeline.run()

deep_models = build_deep_models()
pipeline = DeepLearningBinaryClassificationPipeline(deep_models, refactorings, DATASETS)
pipeline.run()


log_close()