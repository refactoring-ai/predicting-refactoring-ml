from imblearn.under_sampling import RandomUnderSampler

#more info: https://imbalanced-learn.readthedocs.io/en/stable/under_sampling.html
def perform_under_sampling(x, y):
    rus = RandomUnderSampler(random_state=42) # 42 is a random number, just to ensure our results are reproducible
    return rus.fit_resample(x, y)

