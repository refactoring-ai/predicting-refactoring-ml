from sklearn.cluster import KMeans
import pandas as pd
import matplotlib.pyplot as plotter


# Import
# import the raw data and process it into the right shape
def import_data(filePath):
    print("Start importing: " + filePath)
    # read the csv
    data_raw = pd.read_csv(filePath)

    # get the data properties
    unique_ids = len(data_raw.id.unique())
    column_names = ["id", "commits", "exceptionsCount", "javaLoc", "numberOfProductionFiles",
                    "numberOfTestFiles", "productionLoc", "testLoc"]
    unique_refactorings = data_raw.refactoring.unique()
    unique_refactorings_label = [refactoring + " Count" for refactoring in unique_refactorings]
    column_names.extend(unique_refactorings_label)
    column_count = len(column_names)

    # create a new dataframe to store the processed projects
    data = pd.DataFrame(columns=column_names)
    for id in data_raw.id.unique():
        # get the standard data from the
        standard_data = data_raw.loc[data_raw['id'] == id].iloc[0]
        # get all the unique refactorings
        for refactoring in unique_refactorings:
            refactoring_count = data_raw.loc[(data_raw['id'] == id) & (data_raw['refactoring'] == refactoring)]['Refactorings_Count']
            if len(refactoring_count) > 0:
                refactoring_count = refactoring_count.iloc[0]
            else:
                refactoring_count = 0
            standard_data[refactoring + " Count"] = refactoring_count
        data = data.append(standard_data)

    # drop all unused columns
    data = data.drop(['gitUrl'], axis=1)
    data = data.drop(['refactoring'], axis=1)
    data = data.drop(['Refactorings_Count'], axis=1)

    # check if the data was processed correct
    if data.shape == (unique_ids, column_count):
        print("Finished importing: " + filePath)
        return data
    else:
        raise ImportError("Failed to process " + str(filePath) + " with shape" + str(data.shape) + " instead of " + str((unique_ids, column_count)))


# Cluster
# use kmeans clustering to cluster the data
def kmeans_cluster(data_numpy, cluster_count):
    print("Cluster data")
    kmeans = KMeans(n_clusters=cluster_count)
    kmeans.fit(data_numpy)
    return kmeans.predict(data_numpy)


# Visualization
# plot the results of the k-means into a scatter plot
def plot(data, centers, labels):
    plotter.scatter(data[:, 0], c=labels, s=50, cmap='viridis')
    plotter.scatter(centers[:, 0], centers[:, 1], c='black', s=200, alpha=0.5);


# pipeline
data = import_data("projects.csv")

# prepare training, drop the ids for it
data_train = data.drop(['id'], axis=1)
labels = kmeans_cluster(data_train.to_numpy(), 10)
data["Cluster"] = labels

# store the results
print("Store processed data at: projects_processed.csv")
data.to_csv(r'projects_processed.csv', index=False, header=True)
print("Stored processed data at: projects_processed.csv")

# Select 10 samples
for cluster in range(0, 10):
    print(data.loc[data['Cluster'] == cluster].sample()['id'])