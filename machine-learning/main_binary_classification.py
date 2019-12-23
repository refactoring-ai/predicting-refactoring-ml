from configs import DATASETS, ORDERED_DATA
from ml.models.builder import build_models, build_deep_models
from ml.pipelines.binary import BinaryClassificationPipeline, DeepLearningBinaryClassificationPipeline
from ml.pipelines.binary_ordered import BinaryOrderedClassificationPipeline
from refactoring import build_refactorings
from utils.log import log_init, log_close, log

log_init()
log("ML4Refactoring: Binary classification")

refactorings = build_refactorings()

# Run models
models = build_models()
pipeline = None

if ORDERED_DATA:
    pipeline = BinaryOrderedClassificationPipeline(models, refactorings, DATASETS)
else:
    pipeline = BinaryClassificationPipeline(models, refactorings, DATASETS)

pipeline.run()

# Run deep learning models
if not ORDERED_DATA:
    deep_models = build_deep_models()
    pipeline = DeepLearningBinaryClassificationPipeline(deep_models, refactorings, DATASETS)
    pipeline.run()
else:
    # TODO: deep learning models for ordered
    log("No DL models for ordered binary")

# That's it, folks.
log_close()
