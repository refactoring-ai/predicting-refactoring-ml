import os
from imblearn.under_sampling import RandomUnderSampler

#more info: https://imbalanced-learn.readthedocs.io/en/stable/under_sampling.html
def perform_under_sampling(x, y):
    rus = RandomUnderSampler(random_state=42) # 42 is a random number, just to ensure our results are reproducible
    return rus.fit_resample(x, y)

def create_persistence_file_name(f, m_refactoring):
    return os.path.splitext(f.name)[0] + '--' + m_refactoring.lower().replace(' ', '-')

