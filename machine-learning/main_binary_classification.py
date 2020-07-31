from configs import DATASETS
from ml.models.builder import build_models
from ml.pipelines.binary import BinaryClassificationPipeline
from refactoring import build_refactorings
from utils.log import log_init, log_close, log

log_init()
log("ML4Refactoring: Binary classification")

refactorings = build_refactorings()

# Run models
models = build_models()
pipeline = None

pipeline = BinaryClassificationPipeline(models, refactorings, DATASETS)
pipeline.run()

# That's it, folks.
log_close()
