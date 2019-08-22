from configs import DATASETS
from ml.models.base import build_models, build_deep_models
from ml.pipelines import BinaryClassificationPipeline
from refactoring import build_refactorings

refactorings = build_refactorings()
models = build_models()
deep_models = build_deep_models()

pipeline = BinaryClassificationPipeline(refactorings, models, deep_models, DATASETS)
pipeline.run()