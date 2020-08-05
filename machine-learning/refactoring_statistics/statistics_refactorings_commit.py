import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from db.DBConnector import execute_query
from refactoring_statistics.plot_utils import heatmap
from utils.log import log_init, log_close, log
import time
from os import path


def compute_probability(dataframe, divisor, file_addition: str):
    """
    Compute the likelihood of a refactoring to occur in its row and assert if the results are valid.

    Parameters
    ----------
    dataframe
        Compute the likelihood of these refactoring types
    divisor
        Use this to compute the likelihood for each row
    file_addition
        Store the results in this file.
    """
    #compute the probabilty of a refatoring co-occuring on the same commit
    dataframe_probability = dataframe.div(divisor)

    #set the diagonal to zero, in order to clean up the matrix
    np.testing.assert_array_equal(np.diag(dataframe_probability), 1.0, err_msg="Not all elements on the diagonal are zero.")
    np.fill_diagonal(dataframe_probability.values, 0)
    dataframe_probability.to_csv(f"results/Refactorings_{file_addition}_probability.csv", index=False)
    print("Co-occurrence likelihood matrix with shape: " + str(dataframe_probability.shape) + " was computed and stored.")
    return dataframe_probability


def filter_probability(dataframe, threshold, labels, file_addition: str):
    """
    Filter the rows and columns of this dataframe to have at least the specified likelihhod.

    Parameters
    ----------
    dataframe
        Filter the rows and columns of this diagonal matrix.
    threshold
        Filter the max likelihood of each row and column with threshold.
    labels
        Labels fitting the rows and columns of the diagonal matrix.
    """
    filtered_rows_matrix, filtered_labels_rows, filtered_labels_columns, drop_columns = [], [], [], []
    #filter all columns
    for name, values in dataframe.iteritems():
        if values.max() <= threshold:
            drop_columns.append(name)
        else:
            filtered_labels_columns.append(name)
    dataframe_probality_filtered = dataframe.drop(drop_columns, axis=1)
    #filter all rows
    for index, row in dataframe_probality_filtered.iterrows():
        if row.max() > threshold:
            filtered_rows_matrix.append(row)
            filtered_labels_rows.append(labels[index])
    co_occurrence_matrix = pd.DataFrame(filtered_rows_matrix, columns=dataframe_probality_filtered.columns)
    co_occurrence_matrix.to_csv(f"results/Refactorings_{file_addition}_probability_{threshold:.2f}.csv", index=False)
    print(f"Co-occurrence likelihood matrix with shape: {co_occurrence_matrix.shape} and threshold: {threshold:.2f} was computed and  stored.")
    return co_occurrence_matrix, filtered_labels_rows, filtered_labels_columns


def co_occurence_commit(refactorings, probability_threshold = 0.0):
    """
    Computes the occurrence probability of refactorings occurring on the same commit.
    1. It retrieves the relevant data from the refactoringspercommit table.
    2. Processes the data, assert validity, remove diagonal, compute probability per commit
        and filters with the probability threshold
    3. Stores the intermediate results in csv files
    4. Plots and stores a heatmap of the resulting matrix

    Parameters
    ----------
    refactorings
        all relevant refactoring types to consider for the matrix
    probability_threshold
        the probability of a at least one value in a row or column has to be higher, for it to appear in the plot
    """
    #get the raw data
    file_path = "results/Refactorings_commit_statistics.csv"
    dataframe = pd.DataFrame()
    if not path.exists(file_path):
        for refactoring_name in refactorings:
            query = "SELECT "
            query += ", ".join([f"SUM(IF(`{refactoring_type} count` > 0, 1, 0)) AS `{refactoring_type}`" for refactoring_type in refactorings])
            query += ", COUNT(commitMetaData_id) AS `Commit Count` "
            query += (f"FROM refactoringspercommit WHERE `{refactoring_name} count` > 0")
            refactoringspercommit = execute_query(query)
            refactoringspercommit["Refactoring Type"] = refactoring_name
            dataframe = pd.concat([dataframe, refactoringspercommit])
        #store the dataframe to have the raw data
        dataframe.to_csv(file_path, index=False)
        print(f"Got the raw data from refactoringspercommit table and stored it in: {file_path}.")
    else:
        dataframe = pd.read_csv(file_path)

    # extract the labels for the plot and remove unplotted data
    labels = dataframe["Refactoring Type"]
    commit_count = dataframe["Commit Count"].values
    dataframe = dataframe.drop(["Refactoring Type", "Commit Count"], axis=1)

    #compute the probabilities
    dataframe_probability = compute_probability(dataframe, commit_count, "commit")

    #drop irrelevant rows, to simplify the plot
    co_occurrence_matrix, filtered_labels_rows, filtered_labels_columns = filter_probability(dataframe_probability, probability_threshold, labels, "commit")

    #plot the matrix
    #create a subplot, in order to name the columns and rows
    fig, ax = plt.subplots(figsize=(co_occurrence_matrix.shape[1], co_occurrence_matrix.shape[0]), dpi=160)
    im, cbar = heatmap(co_occurrence_matrix.to_numpy(), filtered_labels_rows, filtered_labels_columns, ax=ax, cmap="YlGn", cbarlabel="Co-occurence [P/ Commit]")

    plt.title(f"Co-occurrence of refactoring types on the same commit (min[row | col] > {probability_threshold:.2f})")
    plt.savefig(f"results/Refactorings_co-occurrence_commit_likelihood_{probability_threshold:.2f}_{co_occurrence_matrix.shape}.png")
    print(f"Saved figure: Co-occurrence of refactoring types on the same commit (min[row | col] > {probability_threshold:.2f})")


def co_occurence_window(refactorings, table_name: str, probability_threshold=0.0):
    """
    Computes the occurrence probability of refactorings occurring on the same commit.
    1. It retrieves the relevant data from the refactoringspercommit table.
    2. Processes the data, assert validity, remove diagonal, compute probability per commit
        and filters with the probability threshold
    3. Stores the intermediate results in csv files
    4. Plots and stores a heatmap of the resulting matrix

    Parameters
    ----------
    refactorings
        all relevant refactoring types to consider for the matrix
    probability_threshold
        the probability of a at least one value in a row or column has to be higher, for it to appear in the plot
    """
    dataframe = pd.DataFrame()
    file_path = f"results/Refactorings_window_{table_name}_statistics.csv"
    if not path.exists(file_path):
        for refactoring_name in refactorings:
            query = "SELECT "
            query += ", ".join([f"SUM(IF(`{refactoring_type}` > 0, 1, 0)) AS `{refactoring_type}`" for refactoring_type in refactorings])
            query += ",COUNT(*) AS `Window Count` "
            query += ",SUM(`Commit Count`) AS `Window Size Total` "
            query += f"FROM RefactoringsWindow_{table_name} WHERE `{refactoring_name}` > 0"
            refactoringspercommit = execute_query(query)
            refactoringspercommit["Refactoring Type"] = refactoring_name
            dataframe = pd.concat([dataframe, refactoringspercommit])
        #store the dataframe to have the raw data
        dataframe.to_csv(file_path, index=False)
        print(f"Got the raw data from Refactorings_window_{table_name} and stored it in: {file_path}.")
    else:
        dataframe = pd.read_csv(file_path)

    # extract the labels for the plot and remove unplotted data
    labels = dataframe["Refactoring Type"]
    window_count = dataframe["Window Count"].values
    window_size = dataframe["Window Size Total"].values
    dataframe = dataframe.drop(["Refactoring Type", "Window Count", "Window Size Total"], axis=1)

    #compute the probabilities
    dataframe_probability = compute_probability(dataframe, window_count, f"window_{table_name}")

    #drop irrelevant rows, to simplify the plot
    co_occurrence_matrix, filtered_labels_rows, filtered_labels_columns = filter_probability(dataframe_probability, probability_threshold, labels, f"window_{table_name}")

    #plot the matrix
    #create a subplot, in order to name the columns and rows
    fig, ax = plt.subplots(figsize=(co_occurrence_matrix.shape[1], co_occurrence_matrix.shape[0]), dpi=160)
    im, cbar = heatmap(co_occurrence_matrix.to_numpy(), filtered_labels_rows, filtered_labels_columns, ax=ax, cmap="YlGn", cbarlabel="Co-occurence [P/ Window]")
    plt.title(f"Co-occurrence of refactoring types in the same commit time window of {table_name} (min[row | col] > {probability_threshold:.2f})")
    plt.savefig(f"results/Refactorings_co-occurrence_window_likelihood_{probability_threshold:.2f}_{co_occurrence_matrix.shape}.png")
    print(f"Saved figure: Co-occurrence of refactoring types in the same commit time window of {table_name} (min[row | col] > {probability_threshold:.2f})")

#all refactoring types
refactorings = ["Change Attribute Type",
                "Change Package",
                "Change Parameter Type",
                "Change Return Type",
                "Change Variable Type",
                "Extract And Move Method",
                "Extract Attribute",
                "Extract Class",
                "Extract Interface",
                "Extract Method",
                "Extract Subclass",
                "Extract Superclass",
                "Extract Variable",
                "Inline Method",
                "Inline Variable",
                "Merge Parameter",
                "Merge Variable",
                "Move And Inline Method",
                "Move And Rename Attribute",
                "Move And Rename Class",
                "Move And Rename Method",
                "Move Attribute",
                "Move Class",
                "Move Method",
                "Move Source Folder",
                "Parameterize Variable",
                "Pull Up Attribute",
                "Pull Up Method",
                "Push Down Attribute",
                "Push Down Method",
                "Rename Attribute",
                "Rename Class",
                "Rename Method",
                "Rename Parameter",
                "Rename Variable",
                "Replace Attribute",
                "Replace Variable With Attribute",
                "Split Parameter",
                "Split Variable"]

log_init(header=False, config=False)
log('Begin Statistics')
start_time = time.time()

#Co-occurrence of refactoring types on the same commit
for threshold in np.arange(0.0, 0.6, 0.1):
    co_occurence_commit(refactorings, threshold)

#Co-occurrence of refactoring types in the same commit window
for table in [6, 12]:
    for threshold in np.arange(0.0, 0.6, 0.1):
        co_occurence_window(refactorings, str(table) + "H", threshold,)

log(f"Processing statistics took {time.time() - start_time:.2f} seconds.")
log_close()

exit()
